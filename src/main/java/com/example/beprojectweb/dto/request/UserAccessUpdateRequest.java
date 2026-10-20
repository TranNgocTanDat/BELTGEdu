package com.example.beprojectweb.dto.request;

import com.example.beprojectweb.enums.Role;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class UserAccessUpdateRequest {
    @NotNull
    private Role role;
    private UUID regionId;
}
