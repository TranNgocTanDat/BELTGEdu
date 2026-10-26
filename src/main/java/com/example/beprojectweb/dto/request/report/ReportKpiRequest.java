package com.example.beprojectweb.dto.request.report;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ReportKpiRequest {
    @NotBlank @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2]|Q[1-4]|W(0[1-9]|[1-4][0-9]|5[0-3]))|\\d{4}")
    private String period;

    @NotBlank @Pattern(regexp = "schools|teachers|programs|ppct|lms|issues")
    private String key;

    @NotNull @Min(0) @Max(100000000)
    private Integer target;

    @NotNull @Min(0) @Max(100000000)
    private Integer actual;
}
