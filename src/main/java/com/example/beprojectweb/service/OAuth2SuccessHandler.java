package com.example.beprojectweb.service;

import com.example.beprojectweb.entity.User;
import com.example.beprojectweb.enums.Role;
import com.example.beprojectweb.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashSet;

@RequiredArgsConstructor
@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final AuthenticationService authenticationService; // ✅ Thêm dòng này

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setUsername(email); // vì username là unique
            newUser.setFirstName(name);
            newUser.setEnabled(true); // ✅ vì Google đã xác thực
            HashSet<String> roles = new HashSet<>();
            roles.add(Role.USER.name());
            newUser.setRoles(roles);
            return userRepository.save(newUser);
        });

        // ✅ Sinh token sử dụng service cũ của bạn
        String token = authenticationService.generateToken(user);

        // ✅ Redirect về frontend với token
        String redirectUrl = "http://localhost:5173/oauth2-redirect?token=" + token;
        System.out.println("Redirecting to: " + redirectUrl);
        response.sendRedirect(redirectUrl);
    }
}

