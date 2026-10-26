package com.example.beprojectweb.controller.report;

import com.example.beprojectweb.dto.request.report.ReportStaffRequest;
import com.example.beprojectweb.dto.response.APIResponse;
import com.example.beprojectweb.dto.response.report.ReportStaffResponse;
import com.example.beprojectweb.service.report.ReportStaffService;
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
@RequestMapping("/report-work/staff")
@RequiredArgsConstructor
public class ReportStaffController {
    private final ReportStaffService service;

    @GetMapping
    public APIResponse<List<ReportStaffResponse>> getAll() {
        return APIResponse.<List<ReportStaffResponse>>builder().result(service.getAll()).build();
    }

    @PostMapping
    public APIResponse<ReportStaffResponse> create(@Valid @RequestBody ReportStaffRequest request) {
        return APIResponse.<ReportStaffResponse>builder().result(service.create(request)).build();
    }

    @PutMapping("/{id}")
    public APIResponse<ReportStaffResponse> update(@PathVariable UUID id, @Valid @RequestBody ReportStaffRequest request) {
        return APIResponse.<ReportStaffResponse>builder().result(service.update(id, request)).build();
    }

    @DeleteMapping("/{id}")
    public APIResponse<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return APIResponse.<Void>builder().build();
    }
}
