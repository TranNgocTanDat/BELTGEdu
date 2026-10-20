package com.example.beprojectweb.controller;

import com.example.beprojectweb.dto.request.SchoolRequest;
import com.example.beprojectweb.dto.response.APIResponse;
import com.example.beprojectweb.dto.response.SchoolResponse;
import com.example.beprojectweb.service.SchoolService;
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
@RequestMapping("/schools")
@RequiredArgsConstructor
public class SchoolController {
    private final SchoolService schoolService;

    @GetMapping
    public APIResponse<List<SchoolResponse>> getSchools() {
        return APIResponse.<List<SchoolResponse>>builder().result(schoolService.getSchools()).build();
    }

    @GetMapping("/{id}")
    public APIResponse<SchoolResponse> getSchool(@PathVariable UUID id) {
        return APIResponse.<SchoolResponse>builder().result(schoolService.getSchool(id)).build();
    }

    @PostMapping
    public APIResponse<SchoolResponse> create(@RequestBody @Valid SchoolRequest request) {
        return APIResponse.<SchoolResponse>builder().result(schoolService.create(request)).build();
    }

    @PutMapping("/{id}")
    public APIResponse<SchoolResponse> update(@PathVariable UUID id, @RequestBody @Valid SchoolRequest request) {
        return APIResponse.<SchoolResponse>builder().result(schoolService.update(id, request)).build();
    }

    @DeleteMapping("/{id}")
    public APIResponse<Void> delete(@PathVariable UUID id) {
        schoolService.delete(id);
        return APIResponse.<Void>builder().build();
    }
}
