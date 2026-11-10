package com.example.beprojectweb.dto.response;

import java.util.List;

public record StemEventTemplateResponse(String id, String name, List<StemEventGradeLessonResponse> gradeLessons) {
}
