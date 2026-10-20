package com.example.beprojectweb.controller;

import com.example.beprojectweb.dto.request.RegionRequest;
import com.example.beprojectweb.dto.response.APIResponse;
import com.example.beprojectweb.dto.response.RegionResponse;
import com.example.beprojectweb.service.RegionService;
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
@RequestMapping("/regions")
@RequiredArgsConstructor
public class RegionController {
    private final RegionService regionService;

    @GetMapping
    public APIResponse<List<RegionResponse>> getRegions() {
        return APIResponse.<List<RegionResponse>>builder().result(regionService.getRegions()).build();
    }

    @PostMapping
    public APIResponse<RegionResponse> create(@RequestBody @Valid RegionRequest request) {
        return APIResponse.<RegionResponse>builder().result(regionService.create(request)).build();
    }

    @PutMapping("/{id}")
    public APIResponse<RegionResponse> rename(@PathVariable UUID id, @RequestBody @Valid RegionRequest request) {
        return APIResponse.<RegionResponse>builder().result(regionService.rename(id, request)).build();
    }

    @DeleteMapping("/{id}")
    public APIResponse<Void> delete(@PathVariable UUID id) {
        regionService.delete(id);
        return APIResponse.<Void>builder().build();
    }
}
