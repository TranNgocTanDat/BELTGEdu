package com.example.beprojectweb.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class StemCourseDuplicateRequest {
    @NotBlank
    @Size(max = 40)
    private String code;

    @NotEmpty
    @Size(max = 100)
    private List<@Valid StemLessonCreateRequest> lessons;
}
