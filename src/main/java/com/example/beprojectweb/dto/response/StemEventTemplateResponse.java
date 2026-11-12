package com.example.beprojectweb.dto.response;

import java.util.List;

public record StemEventTemplateResponse(String id, String name, String announcementIntro,
        String announcementGreeting, List<StemEventGradeLessonResponse> gradeLessons) {
}
