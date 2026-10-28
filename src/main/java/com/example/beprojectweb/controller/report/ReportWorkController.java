package com.example.beprojectweb.controller.report;

import com.example.beprojectweb.dto.request.report.ReportWorkRequest;
import com.example.beprojectweb.dto.request.report.ReportWorkExtensionRequest;
import com.example.beprojectweb.dto.request.report.ReportWorkDeadlineAdjustmentRequest;
import com.example.beprojectweb.dto.response.APIResponse;
import com.example.beprojectweb.dto.response.report.ReportWorkResponse;
import com.example.beprojectweb.service.report.ReportWorkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/report-work/work")
@RequiredArgsConstructor
public class ReportWorkController {
    private final ReportWorkService service;

    @GetMapping
    public APIResponse<List<ReportWorkResponse>> getAll() {
        return APIResponse.<List<ReportWorkResponse>>builder().result(service.getAll()).build();
    }

    @GetMapping("/support-requests")
    public APIResponse<List<ReportWorkResponse>> getSupportRequests() {
        return APIResponse.<List<ReportWorkResponse>>builder().result(service.getSupportRequests()).build();
    }

    @PostMapping
    public APIResponse<ReportWorkResponse> create(@Valid @RequestBody ReportWorkRequest request) {
        return APIResponse.<ReportWorkResponse>builder().result(service.create(request)).build();
    }

    @PutMapping("/{id}")
    public APIResponse<ReportWorkResponse> update(@PathVariable UUID id, @Valid @RequestBody ReportWorkRequest request) {
        return APIResponse.<ReportWorkResponse>builder().result(service.update(id, request)).build();
    }

    @PostMapping("/{id}/extensions")
    public APIResponse<ReportWorkResponse> extend(@PathVariable UUID id, @Valid @RequestBody ReportWorkExtensionRequest request) {
        return APIResponse.<ReportWorkResponse>builder().result(service.extend(id, request)).build();
    }

    @PostMapping("/{id}/deadline-changes")
    public APIResponse<ReportWorkResponse> changeDeadline(@PathVariable UUID id, @Valid @RequestBody ReportWorkExtensionRequest request) {
        return APIResponse.<ReportWorkResponse>builder().result(service.changeDeadline(id, request)).build();
    }

    @PostMapping("/{id}/deadline-adjustments")
    public APIResponse<ReportWorkResponse> adjustCompletedDeadline(@PathVariable UUID id, @Valid @RequestBody ReportWorkDeadlineAdjustmentRequest request) {
        return APIResponse.<ReportWorkResponse>builder().result(service.adjustCompletedDeadline(id, request)).build();
    }

    @DeleteMapping("/{id}")
    public APIResponse<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return APIResponse.<Void>builder().build();
    }
}
