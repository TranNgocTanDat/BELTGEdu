package com.example.beprojectweb.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserCreationRequest {
    @Size(min = 3,message = "USERNAME_INVALID")
    String username;
    @Size(min = 3, message = "PASSWORD_INVALID")
    String password;

    @Email(message = "EMAIL_INVALID")  // Đảm bảo đây là một email hợp lệ
    String email;
}
