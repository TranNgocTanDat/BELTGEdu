package com.example.beprojectweb.controller.report;

import com.example.beprojectweb.dto.request.report.ReportWorkGroupRequest;
import com.example.beprojectweb.dto.response.APIResponse;
import com.example.beprojectweb.dto.response.report.ReportWorkGroupResponse;
import com.example.beprojectweb.service.report.ReportWorkGroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/report-work/groups")
@RequiredArgsConstructor
public class ReportWorkGroupController {
    private final ReportWorkGroupService service;
    @GetMapping public APIResponse<List<ReportWorkGroupResponse>> getAll() { return APIResponse.<List<ReportWorkGroupResponse>>builder().result(service.getAll()).build(); }
    @PostMapping public APIResponse<ReportWorkGroupResponse> create(@Valid @RequestBody ReportWorkGroupRequest request) { return APIResponse.<ReportWorkGroupResponse>builder().result(service.create(request)).build(); }
    @PutMapping("/{id}") public APIResponse<ReportWorkGroupResponse> update(@PathVariable UUID id, @Valid @RequestBody ReportWorkGroupRequest request) { return APIResponse.<ReportWorkGroupResponse>builder().result(service.update(id, request)).build(); }
    @DeleteMapping("/{id}") public APIResponse<Void> delete(@PathVariable UUID id) { service.delete(id); return APIResponse.<Void>builder().build(); }
}
