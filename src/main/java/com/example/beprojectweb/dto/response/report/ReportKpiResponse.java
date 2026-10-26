package com.example.beprojectweb.dto.response.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportKpiResponse {
    private UUID id;
    private String period;
    private String key;
    private Integer target;
    private Integer actual;
}
