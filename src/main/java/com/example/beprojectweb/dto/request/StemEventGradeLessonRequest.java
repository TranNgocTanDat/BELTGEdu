package com.example.beprojectweb.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StemEventGradeLessonRequest {
    @NotNull @Min(1) @Max(12)
    private Integer grade;

    @NotBlank @Size(max = 500)
    private String title;

    @Size(max = 2000)
    private String note;

    @Size(max = 2048)
    private String driveImageUrl;
}
