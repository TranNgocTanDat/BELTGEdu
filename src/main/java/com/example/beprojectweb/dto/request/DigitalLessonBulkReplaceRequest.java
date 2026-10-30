package com.example.beprojectweb.dto.request;

import com.example.beprojectweb.enums.DigitalProgram;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class DigitalLessonBulkReplaceRequest {
    @NotNull
    private DigitalProgram program;

    @NotNull @Min(1) @Max(12)
    private Integer grade;

    @NotNull @Size(max = 100)
    private List<@Valid LessonItem> lessons;

    @Data
    public static class LessonItem {
        @NotNull @Positive
        private Integer order;

        @NotBlank @Size(max = 500)
        private String title;

        @Size(max = 2000)
        private String note;

        @Size(max = 2048)
        private String driveImageUrl;
    }
}
