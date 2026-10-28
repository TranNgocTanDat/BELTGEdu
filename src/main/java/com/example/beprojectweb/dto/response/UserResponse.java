package com.example.beprojectweb.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
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
    String firstName;
    String lastName;
    String email;
    LocalDate dob;
    Set<String> roles;
    UUID regionId;
    String regionName;
    UUID reportStaffId;
    String reportStaffName;
    boolean enabled;

}
