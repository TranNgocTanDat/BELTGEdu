package com.example.beprojectweb.service.report;

import java.time.LocalDate;
import java.util.UUID;

public record ReportWorkNotificationEvent(
        Type type,
        UUID workId,
        UUID previousOwnerId,
        LocalDate previousDueDate,
        LocalDate newDueDate,
        String changeType,
        String actor,
        Integer progress,
        String status,
        String note
) {
    public enum Type { CREATED, UPDATED, DEADLINE_CHANGED, MEMBER_UPDATED }
}
