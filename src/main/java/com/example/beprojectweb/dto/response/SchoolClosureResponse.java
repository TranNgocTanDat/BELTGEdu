package com.example.beprojectweb.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchoolClosureResponse {
    private UUID id;
    private UUID schoolId;
    private LocalDate weekStart;
    private String reason;
}
