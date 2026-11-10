package com.example.beprojectweb.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class RegistrationStemEventsRequest {
    private List<@Valid Assignment> assignments;

    @Data
    public static class Assignment {
        @NotBlank
        private String eventId;
        @NotNull @Min(1) @Max(4)
        private Integer term;
        @NotNull
        private LocalDate eventDate;
    }
}
