package com.example.beprojectweb.controller;

import com.example.beprojectweb.dto.request.RegistrationStemEventsRequest;
import com.example.beprojectweb.dto.request.StemEventTemplateRequest;
import com.example.beprojectweb.dto.response.APIResponse;
import com.example.beprojectweb.dto.response.RegistrationStemEventResponse;
import com.example.beprojectweb.dto.response.StemEventTemplateResponse;
import com.example.beprojectweb.service.StemEventManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class StemEventManagementController {
    private final StemEventManagementService service;

    @GetMapping("/curriculum/stem/event-catalog")
    public APIResponse<List<StemEventTemplateResponse>> getTemplates() {
        return APIResponse.<List<StemEventTemplateResponse>>builder().result(service.getTemplates()).build();
    }

    @PostMapping("/curriculum/stem/event-catalog")
    public APIResponse<StemEventTemplateResponse> createTemplate(@RequestBody @Valid StemEventTemplateRequest request) {
        return APIResponse.<StemEventTemplateResponse>builder().result(service.createTemplate(request)).build();
    }

    @PutMapping("/curriculum/stem/event-catalog/{id}")
    public APIResponse<StemEventTemplateResponse> updateTemplate(@PathVariable String id,
            @RequestBody @Valid StemEventTemplateRequest request) {
        return APIResponse.<StemEventTemplateResponse>builder().result(service.updateTemplate(id, request)).build();
    }

    @GetMapping("/stem-event-assignments")
    public APIResponse<List<RegistrationStemEventResponse>> getAssignments() {
        return APIResponse.<List<RegistrationStemEventResponse>>builder().result(service.getAssignments()).build();
    }

    @PutMapping("/registrations/{registrationId}/stem-event-assignments")
    public APIResponse<List<RegistrationStemEventResponse>> replaceAssignments(@PathVariable UUID registrationId,
            @RequestBody @Valid RegistrationStemEventsRequest request) {
        return APIResponse.<List<RegistrationStemEventResponse>>builder()
                .result(service.replaceAssignments(registrationId, request)).build();
    }
}
