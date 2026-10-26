package com.example.beprojectweb.dto.request.report;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;
import java.time.LocalDate;

@Data
public class ReportWorkRequest {
    @NotBlank @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2]|Q[1-4])")
    private String period;

    @Size(max = 80)
    private String group;

    private UUID groupId;

    @NotBlank @Size(max = 500)
    private String title;

    @NotNull
    private UUID ownerId;

    @NotNull @Min(0) @Max(100)
    private Integer progress;

    @NotBlank @Pattern(regexp = "Hoàn thành|Đang thực hiện|Chưa bắt đầu|Trễ hạn|Cần hỗ trợ")
    private String status;

    @Size(max = 2000)
    private String impact;

    private LocalDate startDate;

    private LocalDate originalDueDate;

    private LocalDate currentDueDate;

    private LocalDate completedDate;
}
