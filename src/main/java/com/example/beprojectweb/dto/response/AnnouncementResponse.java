package com.example.beprojectweb.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnnouncementResponse {
    private UUID id;
    private String key;
    private UUID schoolId;
    private String registrationId;
    private String service;
    private LocalDate weekStart;
    private String message;
    private LocalDateTime announcedAt;
}
