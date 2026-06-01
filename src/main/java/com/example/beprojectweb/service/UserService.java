package com.example.beprojectweb.service;

import com.example.beprojectweb.dto.request.UserCreationRequest;
import com.example.beprojectweb.dto.request.UserUpdateRequest;
import com.example.beprojectweb.dto.request.VerifyUser;
import com.example.beprojectweb.dto.response.UserResponse;
import com.example.beprojectweb.entity.User;
import com.example.beprojectweb.enums.Role;
import com.example.beprojectweb.exception.AppException;
import com.example.beprojectweb.exception.ErrorCode;
import com.example.beprojectweb.mapper.UserMapper;
import com.example.beprojectweb.repository.UserRepository;
import jakarta.mail.MessagingException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserService {
    UserRepository userRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;

    public List<UserResponse> getUsers() {
        return userRepository.findAll()
                .stream()
                .map(user -> userMapper.toUserResponse(user))
                .toList();
    }

    public UserResponse getUserById(UUID userId){
        return userMapper.toUserResponse(userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found")));
    }

    @PreAuthorize("hasRole('USER')")
    public UserResponse updateUser( UserUpdateRequest request){
        var context = SecurityContextHolder.getContext();
        String name = context.getAuthentication().getName();
        User user = userRepository.findByUsername(name).orElseThrow(
                () -> new AppException(ErrorCode.USER_NOT_EXISTS));

        userMapper.updateUser(user, request);
        return userMapper.toUserResponse(userRepository.save(user));
    }



    @PreAuthorize("hasRole('USER')")
    public UserResponse getMyInfo(){
        //sau khi đăng nhập thành công thng tin được lưu trong SecurityContextHolder
        var context = SecurityContextHolder.getContext();
        String name = context.getAuthentication().getName();
        User user = userRepository.findByUsername(name).orElseThrow(
                () -> new AppException(ErrorCode.USER_NOT_EXISTS));
        return userMapper.toUserResponse(user);
    }

    public void deleteUser(UUID userId){
        userRepository.deleteById(userId);
    }



}
