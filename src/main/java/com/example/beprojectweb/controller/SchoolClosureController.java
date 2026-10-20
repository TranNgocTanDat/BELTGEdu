package com.example.beprojectweb.controller;

import com.example.beprojectweb.dto.request.SchoolClosureRequest;
import com.example.beprojectweb.dto.response.APIResponse;
import com.example.beprojectweb.dto.response.SchoolClosureResponse;
import com.example.beprojectweb.service.SchoolClosureService;
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
@RequestMapping("/school-closures")
@RequiredArgsConstructor
public class SchoolClosureController {
    private final SchoolClosureService closureService;

    @GetMapping
    public APIResponse<List<SchoolClosureResponse>> getClosures(@RequestParam(required = false) UUID schoolId) {
        return APIResponse.<List<SchoolClosureResponse>>builder()
                .result(closureService.getClosures(schoolId)).build();
    }

    @PostMapping
    public APIResponse<SchoolClosureResponse> addOrUpdate(@RequestBody @Valid SchoolClosureRequest request) {
        return APIResponse.<SchoolClosureResponse>builder().result(closureService.addOrUpdate(request)).build();
    }

    @DeleteMapping("/{id}")
    public APIResponse<Void> remove(@PathVariable UUID id) {
        closureService.remove(id);
        return APIResponse.<Void>builder().build();
    }
}
