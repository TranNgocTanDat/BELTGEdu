package com.example.beprojectweb.controller.report;

import com.example.beprojectweb.dto.request.report.ReportMyWorkUpdateRequest;
import com.example.beprojectweb.dto.response.APIResponse;
import com.example.beprojectweb.dto.response.report.ReportWorkResponse;
import com.example.beprojectweb.service.report.ReportMyWorkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/report-work/my-work")
@RequiredArgsConstructor
public class ReportMyWorkController {
    private final ReportMyWorkService service;

    @GetMapping
    public APIResponse<List<ReportWorkResponse>> getMyWork() {
        return APIResponse.<List<ReportWorkResponse>>builder().result(service.getMyWork()).build();
    }

    @PatchMapping("/{workId}")
    public APIResponse<ReportWorkResponse> updateMyWork(
            @PathVariable UUID workId,
            @Valid @RequestBody ReportMyWorkUpdateRequest request
    ) {
        return APIResponse.<ReportWorkResponse>builder().result(service.updateMyWork(workId, request)).build();
    }
}