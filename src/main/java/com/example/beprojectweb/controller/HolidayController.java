package com.example.beprojectweb.controller;

import com.example.beprojectweb.dto.request.HolidayRequest;
import com.example.beprojectweb.dto.response.APIResponse;
import com.example.beprojectweb.dto.response.HolidayResponse;
import com.example.beprojectweb.service.HolidayService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/holidays")
@RequiredArgsConstructor
public class HolidayController {
    private final HolidayService service;

    @GetMapping
    public APIResponse<List<HolidayResponse>> list(@RequestParam(required = false) Integer year) {
        return APIResponse.<List<HolidayResponse>>builder().result(service.list(year)).build();
    }

    @PostMapping
    public APIResponse<HolidayResponse> create(@RequestBody @Valid HolidayRequest request) {
        return APIResponse.<HolidayResponse>builder().result(service.create(request)).build();
    }

    @PutMapping("/{id}")
    public APIResponse<HolidayResponse> update(@PathVariable UUID id, @RequestBody @Valid HolidayRequest request) {
        return APIResponse.<HolidayResponse>builder().result(service.update(id, request)).build();
    }

    @DeleteMapping("/{id}")
    public APIResponse<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return APIResponse.<Void>builder().build();
    }
}
