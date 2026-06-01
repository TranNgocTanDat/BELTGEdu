package com.example.beprojectweb.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserResponse {
    UUID id;
    String username;
    String password;
    String firstName;
    String lastName;
    String email;
    String verificationCode;
    LocalDateTime verificationCodeExpireAt;
    LocalDate dob;
    Set<String> roles;

}
