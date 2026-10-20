package com.example.beprojectweb.dto.response;
import java.time.LocalDate; import java.util.UUID;
public record LessonProgressResponse(UUID id,String key,UUID registrationId,UUID planItemId,String planType,String lessonId,String status,LocalDate date){}
