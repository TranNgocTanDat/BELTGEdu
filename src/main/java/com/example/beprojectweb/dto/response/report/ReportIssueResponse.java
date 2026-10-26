package com.example.beprojectweb.dto.response.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportIssueResponse {
    private UUID id;
    private String title;
    private String level;
    private String proposal;
    private String kind;
    private String owner;
    private LocalDate dueDate;
    private String period;
    private String status;
    private String impact;
}
