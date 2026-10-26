package com.example.beprojectweb.controller.report;

import com.example.beprojectweb.dto.request.report.ReportKpiRequest;
import com.example.beprojectweb.dto.response.APIResponse;
import com.example.beprojectweb.dto.response.report.ReportKpiResponse;
import com.example.beprojectweb.service.report.ReportKpiService;
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
@RequestMapping("/report-work/kpis")
@RequiredArgsConstructor
public class ReportKpiController {
    private final ReportKpiService service;

    @GetMapping
    public APIResponse<List<ReportKpiResponse>> getAll() {
        return APIResponse.<List<ReportKpiResponse>>builder().result(service.getAll()).build();
    }

    @PostMapping
    public APIResponse<ReportKpiResponse> create(@Valid @RequestBody ReportKpiRequest request) {
        return APIResponse.<ReportKpiResponse>builder().result(service.create(request)).build();
    }

    @PutMapping("/{id}")
    public APIResponse<ReportKpiResponse> update(@PathVariable UUID id, @Valid @RequestBody ReportKpiRequest request) {
        return APIResponse.<ReportKpiResponse>builder().result(service.update(id, request)).build();
    }

    @DeleteMapping("/{id}")
    public APIResponse<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return APIResponse.<Void>builder().build();
    }
}
