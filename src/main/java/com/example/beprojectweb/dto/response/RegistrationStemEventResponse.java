package com.example.beprojectweb.dto.response;

import java.time.LocalDate;
import java.util.UUID;

public record RegistrationStemEventResponse(UUID id, String registrationId, String eventId, String eventName,
        Integer term, LocalDate eventDate) {
}
