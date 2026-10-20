package com.example.beprojectweb.dto.response;

import com.example.beprojectweb.enums.DigitalProgram;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DigitalLessonResponse {
    private String id;
    private DigitalProgram program;
    private Integer grade;
    private Integer order;
    private String title;
}
