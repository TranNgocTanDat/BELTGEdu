package com.example.beprojectweb.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegionRequest {
    @NotBlank
    @Size(max = 120)
    private String name;
}
