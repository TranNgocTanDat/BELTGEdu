package com.example.beprojectweb.service;

import com.example.beprojectweb.dto.request.UserAccessUpdateRequest;
import com.example.beprojectweb.dto.request.AdminUserUpdateRequest;
import com.example.beprojectweb.dto.request.UserUpdateRequest;
import com.example.beprojectweb.dto.response.UserResponse;
import com.example.beprojectweb.entity.Region;
import com.example.beprojectweb.entity.User;
import com.example.beprojectweb.enums.Role;
import com.example.beprojectweb.exception.AppException;
import com.example.beprojectweb.exception.ErrorCode;
import com.example.beprojectweb.mapper.UserMapper;
import com.example.beprojectweb.repository.RegionRepository;
import com.example.beprojectweb.repository.UserRepository;
import com.example.beprojectweb.repository.RefreshTokenRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserService {
    UserRepository userRepository;
    RefreshTokenRepository refreshTokenRepository;
    RegionRepository regionRepository;
    UserMapper userMapper;

    @Transactional(readOnly = true)
    public List<UserResponse> getUsers() {
        User current = currentUser();
        List<User> users;
        if (isAdmin(current)) {
            users = userRepository.findAll();
        } else if (current.getRegion() != null) {
            users = userRepository.findAllByRegion_Id(current.getRegion().getId());
        } else {
            users = List.of();
        }
        return users.stream().map(userMapper::toUserResponse).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID userId) {
        User current = currentUser();
        User requested = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTS));
        assertSameRegionOrAdmin(current, requested);
        return userMapper.toUserResponse(requested);
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'REPORT_WORK', 'ADMIN_SCHOOL')")
    @Transactional(readOnly = true)
    public UserResponse getMyInfo() {
        return userMapper.toUserResponse(currentUser());
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Transactional
    public UserResponse updateUser(UserUpdateRequest request) {
        User user = currentUser();
        userMapper.updateUser(user, request);
        return userMapper.toUserResponse(userRepository.save(user));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public UserResponse updateAccess(UUID userId, UserAccessUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTS));
        Region region = null;
        if (request.getRole() == Role.USER) {
            if (request.getRegionId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "USER accounts must be assigned to a region");
            }
            region = regionRepository.findById(request.getRegionId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Region not found"));
        }
        user.setRoles(new HashSet<>(List.of(request.getRole().name())));
        user.setRegion(region);
        return userMapper.toUserResponse(userRepository.save(user));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public UserResponse updateUserByAdmin(UUID userId, AdminUserUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTS));
        String username = request.getUsername().trim();
        String email = request.getEmail().trim();
        if (userRepository.existsByUsernameAndIdNot(username, userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tên tài khoản đã được sử dụng");
        }
        if (userRepository.existsByEmailAndIdNot(email, userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email đã được sử dụng");
        }
        Region region = null;
        if (request.getRole() == Role.USER) {
            if (request.getRegionId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "USER accounts must be assigned to a region");
            }
            region = regionRepository.findById(request.getRegionId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Region not found"));
        }
        user.setUsername(username);
        user.setEmail(email);
        user.setFirstName(request.getFirstName() == null ? null : request.getFirstName().trim());
        user.setLastName(request.getLastName() == null ? null : request.getLastName().trim());
        user.setDob(request.getDob());
        user.setRoles(new HashSet<>(List.of(request.getRole().name())));
        user.setRegion(region);
        return userMapper.toUserResponse(userRepository.save(user));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public UserResponse activateUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTS));
        user.setEnabled(true);
        user.setVerificationCode(null);
        user.setVerificationCodeExpiredAt(null);
        return userMapper.toUserResponse(userRepository.save(user));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void deleteUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTS));
        if (user.getId().equals(currentUser().getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Không thể xóa tài khoản đang đăng nhập");
        }
        refreshTokenRepository.deleteAllByUser_Id(userId);
        userRepository.delete(user);
    }

    private User currentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTS));
    }

    private boolean isAdmin(User user) {
        return user.getRoles() != null && user.getRoles().contains(Role.ADMIN.name());
    }

    private void assertSameRegionOrAdmin(User current, User requested) {
        if (isAdmin(current)) return;
        UUID currentRegion = current.getRegion() == null ? null : current.getRegion().getId();
        UUID requestedRegion = requested.getRegion() == null ? null : requested.getRegion().getId();
        if (currentRegion == null || !currentRegion.equals(requestedRegion)) {
            throw new AccessDeniedException("You can only view users assigned to your region");
        }
    }
}
