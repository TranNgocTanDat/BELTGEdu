package com.example.beprojectweb.dto.response;

import java.util.List;
import java.util.UUID;

public record StemPlanResponse(UUID id, UUID registrationId, Integer grade, Integer term, String courseId,
                               Integer effectiveFromWeek, String changeNote, boolean version, List<String> lessonOrder,
                               List<String> excludedLessonIds) {}
