package com.example.beprojectweb.dto.request.report;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Data;

@Data
public class ReportIssueRequest {
    @NotBlank @Size(max = 2000)
    private String title;

    @NotBlank @Pattern(regexp = "Theo dõi|Cần phối hợp|Cần hỗ trợ nguồn lực")
    private String level;

    @NotBlank @Size(max = 2000)
    private String proposal;

    @Pattern(regexp = "RISK|ISSUE|ACTION")
    private String kind;

    @Size(max = 160)
    private String owner;

    private LocalDate dueDate;

    @Pattern(regexp = "|\\d{4}-(0[1-9]|1[0-2]|Q[1-4]|W(0[1-9]|[1-4][0-9]|5[0-3]))|\\d{4}")
    private String period;

    @Pattern(regexp = "|OPEN|IN_PROGRESS|RESOLVED|CLOSED")
    private String status;

    @Size(max = 2000)
    private String impact;
}
