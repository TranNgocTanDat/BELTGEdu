package com.example.beprojectweb.dto.response.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportGoalResponse {
    private UUID id;
    private String period;
    private String title;
    private String output;
    private String owner;
    private Integer progress;
    private String status;
}
