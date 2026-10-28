package com.example.beprojectweb.dto.request;

import com.example.beprojectweb.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class AdminUserUpdateRequest {
    @NotBlank @Size(min = 3, max = 100)
    private String username;
    @NotBlank @Email @Size(max = 254)
    private String email;
    @Size(max = 100)
    private String firstName;
    @Size(max = 100)
    private String lastName;
    private LocalDate dob;
    @NotNull
    private Role role;
    private UUID regionId;
    private UUID reportStaffId;
}
