package com.example.beprojectweb.dto.request;

import lombok.Data;
import java.util.UUID;

@Data
public class StemPlanRequest {
    private UUID registrationId;
    private Integer grade;
    private Integer term;
    private String courseId;
    private Integer effectiveFromWeek;
    private String changeNote;
}
