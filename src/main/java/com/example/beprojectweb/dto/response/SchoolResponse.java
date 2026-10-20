package com.example.beprojectweb.dto.response;

import com.example.beprojectweb.enums.SchoolLevel;
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
public class SchoolResponse {
    private UUID id;
    private String name;
    private String code;
    private String address;
    private String contactName;
    private String phone;
    private SchoolLevel level;
    private UUID regionId;
    private String regionName;
    private LocalDate createdAt;
}
