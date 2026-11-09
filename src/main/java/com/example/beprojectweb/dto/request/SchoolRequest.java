package com.example.beprojectweb.dto.request;

import com.example.beprojectweb.enums.SchoolLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class SchoolRequest {
    @NotBlank
    @Size(max = 200)
    private String name;

    @Size(max = 40)
    private String code;

    @Size(max = 500)
    private String address;

    @Size(max = 150)
    private String contactName;

    @Size(max = 40)
    private String phone;

    @Size(max = 300)
    private String zaloGroupStem;

    @Size(max = 300)
    private String zaloGroupDigital;

    @NotNull
    private SchoolLevel level;

    private UUID regionId;
}
