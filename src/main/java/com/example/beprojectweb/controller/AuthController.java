package com.example.beprojectweb.controller;

import com.example.beprojectweb.dto.request.AuthenticationRequest;
import com.example.beprojectweb.dto.request.IntrospectRequest;
import com.example.beprojectweb.dto.request.RefreshTokenRequest;
import com.example.beprojectweb.dto.request.UserCreationRequest;
import com.example.beprojectweb.dto.request.VerifyUser;
import com.example.beprojectweb.dto.response.APIResponse;
import com.example.beprojectweb.dto.response.AuthenticationResponse;
import com.example.beprojectweb.dto.response.IntrospectResponse;
import com.example.beprojectweb.dto.response.UserResponse;
import com.example.beprojectweb.mapper.UserMapper;
import com.example.beprojectweb.service.AuthenticationService;
import com.nimbusds.jose.JOSEException;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthController {
    AuthenticationService authenticationService;
    UserMapper userMapper;

    @PostMapping("/signup")
    APIResponse<UserResponse> createUser(@RequestBody @Valid UserCreationRequest request) {
        return APIResponse.<UserResponse>builder()
                .result(userMapper.toUserResponse(authenticationService.createUser(request)))
                .build();
    }
    //Tạo endpoint login
    @PostMapping("/login")
    APIResponse<AuthenticationResponse> authenticate(@RequestBody AuthenticationRequest request){
        var result = authenticationService.authenticate(request);
        return APIResponse.<AuthenticationResponse>builder()
                .result(result)
                .build();
    }

    @PostMapping("/introspect")
    APIResponse<IntrospectResponse> introspect(@RequestBody IntrospectRequest request) throws ParseException, JOSEException {
        var result = authenticationService.introspectResponse(request);
        return APIResponse.<IntrospectResponse>builder()
                .result(result)
                .build();
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyUser(@RequestBody VerifyUser request) {
        try {
            authenticationService.verifyUser(request);
            return ResponseEntity.ok("Account verified successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/resend")
    public ResponseEntity<?> resendVerificationCode(@RequestParam String email) {
        try {
            authenticationService.resendVerificationCode(email);
            return ResponseEntity.ok("Verification code sent");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/refresh")
    public APIResponse<AuthenticationResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        var result = authenticationService.refreshAccessToken(request.getRefreshToken());
        return APIResponse.<AuthenticationResponse>builder()
                .result(result)
                .build();
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody RefreshTokenRequest request) {
        try {
            authenticationService.logout(request.getRefreshToken());
            return ResponseEntity.ok("Logged out successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}
