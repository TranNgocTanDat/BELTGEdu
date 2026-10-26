package com.example.beprojectweb.dto.request.report;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReportGoalRequest {
    @NotBlank @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2]|Q[1-4]|W(0[1-9]|[1-4][0-9]|5[0-3]))|\\d{4}")
    private String period;

    @NotBlank @Size(max = 500)
    private String title;

    @NotBlank @Size(max = 2000)
    private String output;

    @NotBlank @Size(max = 160)
    private String owner;

    @NotNull @Min(0) @Max(100)
    private Integer progress;

    @NotBlank @Pattern(regexp = "Hoàn thành|Đang thực hiện|Chưa bắt đầu|Trễ hạn|Cần hỗ trợ")
    private String status;
}
