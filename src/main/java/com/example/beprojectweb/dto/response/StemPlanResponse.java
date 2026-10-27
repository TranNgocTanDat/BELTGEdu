package com.example.beprojectweb.dto.response;

import java.util.UUID;

public record StemPlanResponse(UUID id, UUID registrationId, Integer grade, Integer term, String courseId,
                               Integer effectiveFromWeek, String changeNote, boolean version) {}
