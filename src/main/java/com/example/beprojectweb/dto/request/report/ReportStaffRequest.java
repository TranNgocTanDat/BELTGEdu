package com.example.beprojectweb.dto.request.report;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReportStaffRequest {
    @NotBlank @Size(max = 160)
    private String name;

    @NotBlank @Size(max = 120)
    private String role;

    @NotBlank @Size(max = 2000)
    private String scope;

    @NotBlank @Size(max = 2000)
    private String output;

    @NotBlank @Pattern(regexp = "Đang làm|Tạm nghỉ")
    private String status;
}
