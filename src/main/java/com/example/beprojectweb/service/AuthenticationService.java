package com.example.beprojectweb.service;

import com.example.beprojectweb.dto.request.AuthenticationRequest;
import com.example.beprojectweb.dto.request.IntrospectRequest;
import com.example.beprojectweb.dto.request.UserCreationRequest;
import com.example.beprojectweb.dto.request.VerifyUser;
import com.example.beprojectweb.dto.response.AuthenticationResponse;
import com.example.beprojectweb.dto.response.IntrospectResponse;
import com.example.beprojectweb.entity.User;
import com.example.beprojectweb.enums.Role;
import com.example.beprojectweb.exception.AppException;
import com.example.beprojectweb.exception.ErrorCode;
import com.example.beprojectweb.mapper.UserMapper;
import com.example.beprojectweb.repository.UserRepository;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.mail.MessagingException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationService {
    UserRepository userRepository;
    PasswordEncoder passwordEncoder;
    UserMapper userMapper;
    EmailService emailService;


    @NonFinal
    @Value("${jwt.signerKey}")
    protected String signerKey;

    public User createUser(UserCreationRequest request) {
        if (userRepository.existsUserByUsername(request.getUsername()))
            throw new AppException(ErrorCode.USER_EXISTED);

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        User user = userMapper.toUser(request);
        user.setVerificationCode(generateVerificationCode());
        user.setVerificationCodeExpiredAt(LocalDateTime.now().plusMinutes(15));
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        HashSet<String> roles = new HashSet<>();
        roles.add(Role.USER.name());
        user.setRoles(roles);
        sendVerificationEmail(user);
        return userRepository.save(user);
    }

    //check login
    public AuthenticationResponse authenticate(AuthenticationRequest request){
        //check user
        var user = userRepository.findByEmail(request.getEmail()).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTS));

        // check if user is enabled (email verified)
        if (!user.isEnabled()) {
            throw new AppException(ErrorCode.USER_NOT_ENABLED);
        }

        //sử dụng matches để kiểm tra pass nhập vào đúng với pass trong db
        boolean authenticated = passwordEncoder.matches(request.getPassword(), user.getPassword());

        //kiểm tra đăng nhập
        if(!authenticated)
            throw new AppException(ErrorCode.UNATHENTICATIED);
        //sucess
        var token = generateToken(user);

        return AuthenticationResponse.builder()
                .token(token)
                .authenticated(true)
                .userResponse(userMapper.toUserResponse(user))
                .build();

    }

    public String generateToken(User user){
        //Create Header
        JWSHeader jwsHeader = new JWSHeader(JWSAlgorithm.HS512);

        //create Payload
        //claim
        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(user.getUsername()) //user login
                .issuer("beproject")
                .issueTime(new Date()) //thời gian tạo token
                .expirationTime(new Date(
                        Instant.now().plus(1, ChronoUnit.HOURS).toEpochMilli()
                ))
                //tạo 1 claim chứa scope nhận biết user or admin
                //tạo 1 hàm build scope bên dưới
                .claim("scope", buildScope(user))
                .build();

        //Payload
        Payload payload = new Payload(jwtClaimsSet.toJSONObject());

        //JWSObject
        JWSObject jwsObject = new JWSObject(jwsHeader, payload);

        //ký token
        try {
            jwsObject.sign(new MACSigner(signerKey.getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {

            throw new RuntimeException(e);
        }
    }

    //verify token
    public IntrospectResponse introspectResponse(IntrospectRequest request) throws JOSEException, ParseException {
        //lấy token
        var token = request.getToken();

        // Sử dụng JWSVerifier
        JWSVerifier jwsVerifier =new MACVerifier(signerKey.getBytes());

        // Sử dụng signedJWT
        SignedJWT signedJWT = SignedJWT.parse(token);

        // Kiểm tra thời gian token
        Date expityTime = signedJWT.getJWTClaimsSet().getExpirationTime();

        // verify
        var verifier =signedJWT.verify(jwsVerifier);
        return IntrospectResponse.builder()
                .valid(verifier && expityTime.after(new Date()))
                .build();

    }

    //bui scope từ 1 user
    private String buildScope(User user){
        //scope đang là một 1 lisr , dùng StringJoiner để nối các scope bằng dấu " "
        StringJoiner stringJoiner = new StringJoiner(" "); // Oauth2 quy định scope phân cách nhau bằng " "

        //kiểm tra role
        if(!CollectionUtils.isEmpty(user.getRoles()))
            user.getRoles().forEach(stringJoiner::add);
        return stringJoiner.toString();
    }

    public void verifyUser(VerifyUser request) {
        Optional<User> optionalUser = userRepository.findByEmail(request.getEmail());
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (user.getVerificationCodeExpiredAt().isBefore(LocalDateTime.now())) {
                throw new RuntimeException("Verification code has expired");
            }
            if (user.getVerificationCode().equals(request.getVerificationCode())) {
                user.setEnabled(true);
                user.setVerificationCode(null);
                user.setVerificationCodeExpiredAt(null);
                userRepository.save(user);
            } else {
                throw new RuntimeException("Invalid verification code");
            }
        } else {
            throw new RuntimeException("User not found");
        }
    }

    public void resendVerificationCode(String email) {
        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (user.isEnabled()) {
                throw new RuntimeException("Account is already verified");
            }
            user.setVerificationCode(generateVerificationCode());
            user.setVerificationCodeExpiredAt(LocalDateTime.now().plusHours(1));
            sendVerificationEmail(user);
            userRepository.save(user);
        } else {
            throw new RuntimeException("User not found");
        }
    }


    private void sendVerificationEmail(User user) { //TODO: Update with company logo
        String subject = "Account Verification";
        String verificationCode = "VERIFICATION CODE " + user.getVerificationCode();
        String htmlMessage = "<html>"
                + "<body style=\"font-family: Arial, sans-serif;\">"
                + "<div style=\"background-color: #f5f5f5; padding: 20px;\">"
                + "<h2 style=\"color: #333;\">Welcome to our app!</h2>"
                + "<p style=\"font-size: 16px;\">Please enter the verification code below to continue:</p>"
                + "<div style=\"background-color: #fff; padding: 20px; border-radius: 5px; box-shadow: 0 0 10px rgba(0,0,0,0.1);\">"
                + "<h3 style=\"color: #333;\">Verification Code:</h3>"
                + "<p style=\"font-size: 18px; font-weight: bold; color: #007bff;\">" + verificationCode + "</p>"
                + "</div>"
                + "</div>"
                + "</body>"
                + "</html>";

        try {
            emailService.sendVerificationEmail(user.getEmail(), subject, htmlMessage);
        } catch (MessagingException e) {
            // Handle email sending exception
            e.printStackTrace();
        }
    }
    private String generateVerificationCode() {
        Random random = new Random();
        int code = random.nextInt(900000) + 100000;
        return String.valueOf(code);
    }
}
