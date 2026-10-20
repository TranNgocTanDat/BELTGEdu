package com.example.beprojectweb.dto.request;

import com.example.beprojectweb.enums.StemProgram;
import com.example.beprojectweb.enums.StemSubject;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StemCourseRequest {
    @NotBlank @Size(max = 40)
    private String code;
    @NotNull
    private StemProgram program;
    @NotNull
    private StemSubject subject;
    @Min(1) @Max(12)
    private Integer grade;
    @Min(1)
    private Integer module;
    @NotNull @Min(1) @Max(4)
    private Integer part;
}
