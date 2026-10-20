package com.example.beprojectweb.controller;

import com.example.beprojectweb.dto.request.UserAccessUpdateRequest;
import com.example.beprojectweb.dto.request.UserUpdateRequest;
import com.example.beprojectweb.dto.response.APIResponse;
import com.example.beprojectweb.dto.response.UserResponse;
import com.example.beprojectweb.service.UserService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {
    UserService userService;

    @GetMapping
    public APIResponse<List<UserResponse>> getUsers() {
        return APIResponse.<List<UserResponse>>builder().result(userService.getUsers()).build();
    }

    @GetMapping("/{userId}")
    public APIResponse<UserResponse> getUserById(@PathVariable UUID userId) {
        return APIResponse.<UserResponse>builder().result(userService.getUserById(userId)).build();
    }

    @GetMapping("/myInfo")
    public APIResponse<UserResponse> getMyInfo() {
        return APIResponse.<UserResponse>builder().result(userService.getMyInfo()).build();
    }

    @PutMapping("/me/update")
    public APIResponse<UserResponse> updateUser(@RequestBody @Valid UserUpdateRequest request) {
        return APIResponse.<UserResponse>builder().result(userService.updateUser(request)).build();
    }

    @PatchMapping("/{userId}/access")
    public APIResponse<UserResponse> updateAccess(@PathVariable UUID userId,
                                                   @RequestBody @Valid UserAccessUpdateRequest request) {
        return APIResponse.<UserResponse>builder().result(userService.updateAccess(userId, request)).build();
    }

    @DeleteMapping("/{userId}")
    public APIResponse<Void> deleteUser(@PathVariable UUID userId) {
        userService.deleteUser(userId);
        return APIResponse.<Void>builder().build();
    }
}
