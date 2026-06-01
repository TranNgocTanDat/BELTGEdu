package com.example.beprojectweb.service;

import com.example.beprojectweb.entity.User;
import com.example.beprojectweb.enums.Role;
import com.example.beprojectweb.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.HashSet;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User>{
    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = new DefaultOAuth2UserService().loadUser(userRequest);

        // Lấy thông tin người dùng từ Google
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        // Nếu người dùng chưa có trong DB -> tạo mới
        userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setUsername(email); // hoặc tạo username riêng nếu muốn
            newUser.setFirstName(name);
            newUser.setEnabled(true); // Google login không cần verify code
            HashSet<String> roles = new HashSet<>();
            roles.add(Role.USER.name());
            newUser.setRoles(roles);
            return userRepository.save(newUser);
        });

        return oAuth2User;
    }
}
