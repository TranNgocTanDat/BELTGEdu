package com.example.beprojectweb.dto.request;
import lombok.Data; import java.util.UUID;
@Data public class LessonProgressRequest { private String planType; private UUID planItemId; private String lessonId; private String status; }
