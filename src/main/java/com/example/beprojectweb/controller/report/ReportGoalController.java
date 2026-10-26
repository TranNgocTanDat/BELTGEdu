package com.example.beprojectweb.controller.report;

import com.example.beprojectweb.dto.request.report.ReportGoalRequest;
import com.example.beprojectweb.dto.response.APIResponse;
import com.example.beprojectweb.dto.response.report.ReportGoalResponse;
import com.example.beprojectweb.service.report.ReportGoalService;
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
@RequestMapping("/report-work/goals")
@RequiredArgsConstructor
public class ReportGoalController {
    private final ReportGoalService service;

    @GetMapping
    public APIResponse<List<ReportGoalResponse>> getAll() {
        return APIResponse.<List<ReportGoalResponse>>builder().result(service.getAll()).build();
    }

    @PostMapping
    public APIResponse<ReportGoalResponse> create(@Valid @RequestBody ReportGoalRequest request) {
        return APIResponse.<ReportGoalResponse>builder().result(service.create(request)).build();
    }

    @PutMapping("/{id}")
    public APIResponse<ReportGoalResponse> update(@PathVariable UUID id, @Valid @RequestBody ReportGoalRequest request) {
        return APIResponse.<ReportGoalResponse>builder().result(service.update(id, request)).build();
    }

    @DeleteMapping("/{id}")
    public APIResponse<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return APIResponse.<Void>builder().build();
    }
}
