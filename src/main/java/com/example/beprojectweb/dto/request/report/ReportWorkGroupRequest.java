package com.example.beprojectweb.dto.request.report;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReportWorkGroupRequest {
    @NotBlank @Size(max = 80)
    private String name;
    private Boolean active;
}
