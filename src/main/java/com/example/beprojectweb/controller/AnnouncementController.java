package com.example.beprojectweb.controller;

import com.example.beprojectweb.dto.request.AnnouncementRequest;
import com.example.beprojectweb.dto.request.AnnouncementBulkRequest;
import com.example.beprojectweb.dto.request.AnnouncementProgressBatchRequest;
import com.example.beprojectweb.dto.response.APIResponse;
import com.example.beprojectweb.dto.response.AnnouncementResponse;
import com.example.beprojectweb.service.AnnouncementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/announcements")
@RequiredArgsConstructor
public class AnnouncementController {
    private final AnnouncementService announcementService;

    @GetMapping
    public APIResponse<List<AnnouncementResponse>> getAnnouncements(
            @RequestParam(required = false) UUID schoolId) {
        return APIResponse.<List<AnnouncementResponse>>builder()
                .result(announcementService.getAnnouncements(schoolId)).build();
    }

    @PostMapping
    public APIResponse<AnnouncementResponse> mark(@RequestBody @Valid AnnouncementRequest request) {
        return APIResponse.<AnnouncementResponse>builder().result(announcementService.mark(request)).build();
    }

    @PostMapping("/bulk")
    public APIResponse<List<AnnouncementResponse>> markBulk(@RequestBody @Valid AnnouncementBulkRequest request) {
        return APIResponse.<List<AnnouncementResponse>>builder().result(announcementService.markBulk(request)).build();
    }

    @DeleteMapping("/{key}")
    public APIResponse<Void> unmark(@PathVariable String key, @RequestBody(required = false) @Valid AnnouncementProgressBatchRequest request) {
        announcementService.unmark(key, request);
        return APIResponse.<Void>builder().build();
    }
}
