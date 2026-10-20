package com.example.beprojectweb.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LessonTitleRequest {
    @NotBlank @Size(max = 500)
    private String title;
}
