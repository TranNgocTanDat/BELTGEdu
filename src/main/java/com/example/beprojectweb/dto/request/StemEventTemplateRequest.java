package com.example.beprojectweb.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class StemEventTemplateRequest {
    @NotBlank @Size(max = 200)
    private String name;

    @NotEmpty @Size(min = 1, max = 12)
    private List<@Valid StemEventGradeLessonRequest> gradeLessons;
}
