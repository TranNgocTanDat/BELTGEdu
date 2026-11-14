package com.example.beprojectweb.dto.request;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class StemPlanLessonOrderRequest {
    private UUID registrationId;
    private Integer grade;
    private Integer term;
    private UUID planItemId;
    private List<String> lessonIds;
    private List<String> excludedLessonIds;
}
