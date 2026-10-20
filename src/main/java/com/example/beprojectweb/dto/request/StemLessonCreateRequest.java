package com.example.beprojectweb.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StemLessonCreateRequest {
    @NotNull @Min(1)
    private Integer order;
    @NotBlank @Size(max = 500)
    private String title;
}
