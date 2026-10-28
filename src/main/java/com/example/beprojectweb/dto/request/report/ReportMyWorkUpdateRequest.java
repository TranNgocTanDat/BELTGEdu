package com.example.beprojectweb.dto.request.report;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReportMyWorkUpdateRequest {
    @NotNull @Min(0) @Max(100)
    private Integer progress;

    @NotBlank @Pattern(regexp = "Chưa bắt đầu|Đang thực hiện|Hoàn thành|Cần hỗ trợ")
    private String status;

    @Size(max = 2000)
    private String impact;
}