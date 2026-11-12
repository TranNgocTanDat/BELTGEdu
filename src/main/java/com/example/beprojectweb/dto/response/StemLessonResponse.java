package com.example.beprojectweb.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StemLessonResponse {
    private String id;
    private Integer order;
    private String title;
    private String note;
    private String driveImageUrl;
    private String driveImageUrlAig;
    private String driveImageUrlEtech;
}
