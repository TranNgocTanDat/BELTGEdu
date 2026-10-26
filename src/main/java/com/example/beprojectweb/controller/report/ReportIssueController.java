package com.example.beprojectweb.controller.report;

import com.example.beprojectweb.dto.request.report.ReportIssueRequest;
import com.example.beprojectweb.dto.response.APIResponse;
import com.example.beprojectweb.dto.response.report.ReportIssueResponse;
import com.example.beprojectweb.service.report.ReportIssueService;
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
@RequestMapping("/report-work/issues")
@RequiredArgsConstructor
public class ReportIssueController {
    private final ReportIssueService service;

    @GetMapping
    public APIResponse<List<ReportIssueResponse>> getAll() {
        return APIResponse.<List<ReportIssueResponse>>builder().result(service.getAll()).build();
    }

    @PostMapping
    public APIResponse<ReportIssueResponse> create(@Valid @RequestBody ReportIssueRequest request) {
        return APIResponse.<ReportIssueResponse>builder().result(service.create(request)).build();
    }

    @PutMapping("/{id}")
    public APIResponse<ReportIssueResponse> update(@PathVariable UUID id, @Valid @RequestBody ReportIssueRequest request) {
        return APIResponse.<ReportIssueResponse>builder().result(service.update(id, request)).build();
    }

    @DeleteMapping("/{id}")
    public APIResponse<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return APIResponse.<Void>builder().build();
    }
}
