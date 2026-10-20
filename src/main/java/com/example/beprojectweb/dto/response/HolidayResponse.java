package com.example.beprojectweb.dto.response;

import java.time.LocalDate;
import java.util.UUID;

public record HolidayResponse(UUID id, String name, LocalDate startDate, LocalDate endDate) {}
