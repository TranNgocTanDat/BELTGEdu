package com.example.beprojectweb.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class AnnouncementRequest {
    @NotNull
    private UUID schoolId;

    @NotBlank
    @Size(max = 100)
    private String registrationId;

    @NotBlank
    @Pattern(regexp = "stem|digital")
    private String service;

    @NotNull
    private LocalDate weekStart;

    @NotBlank
    @Size(max = 10000)
    private String message;
}
