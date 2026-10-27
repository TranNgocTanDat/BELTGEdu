package com.example.beprojectweb.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class AnnouncementBulkRequest {
    @NotEmpty
    @Size(max = 100)
    private List<@Valid AnnouncementRequest> items;
}
