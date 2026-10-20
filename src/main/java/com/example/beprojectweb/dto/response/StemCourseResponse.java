package com.example.beprojectweb.dto.response;

import com.example.beprojectweb.enums.StemProgram;
import com.example.beprojectweb.enums.StemSubject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StemCourseResponse {
    private String id;
    private String code;
    private StemProgram program;
    private StemSubject subject;
    private Integer grade;
    private Integer module;
    private Integer part;
    private List<StemLessonResponse> lessons;
}
