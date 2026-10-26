package com.example.beprojectweb.dto.response.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportStaffResponse {
    private UUID id;
    private String name;
    private String role;
    private String scope;
    private String output;
    private String status;
}
