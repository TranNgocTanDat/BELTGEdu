package com.example.beprojectweb.dto.request;

import lombok.Data;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Data
public class AnnouncementProgressBatchRequest {
    @NotNull
    @Valid
    @Size(max = 200)
    private List<LessonProgressRequest> progress;
}
