package com.example.beprojectweb.dto.request.report;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ReportWorkDeadlineAdjustmentRequest {
    @NotNull
    private LocalDate newDueDate;

    @NotBlank
    @Size(max = 1000)
    private String reason;
}
