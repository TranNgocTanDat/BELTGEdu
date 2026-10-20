package com.example.beprojectweb.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class SchoolClosureRequest {
    @NotNull
    private UUID schoolId;

    @NotNull
    private LocalDate weekStart;

    @NotBlank
    @Size(max = 500)
    private String reason;
}
