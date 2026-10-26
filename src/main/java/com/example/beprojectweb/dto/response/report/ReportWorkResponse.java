package com.example.beprojectweb.dto.response.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportWorkResponse {
    private UUID id;
    private String period;
    private String group;
    private String title;
    private UUID ownerId;
    private Integer progress;
    private String status;
    private String impact;
    private UUID groupId;
    private LocalDate startDate;
    private LocalDate originalDueDate;
    private LocalDate currentDueDate;
    private LocalDate completedDate;
    private LocalDateTime createdAt;
    private String deadlineResult;
    private List<Update> updates;
    private List<Extension> extensions;

    public record Update(UUID id, LocalDateTime updatedAt, Integer progress, String status, String note, String updatedBy) {}
    public record Extension(UUID id, LocalDateTime createdAt, LocalDate previousDueDate, LocalDate newDueDate, String reason, String extendedBy, String changeType) {}
}
