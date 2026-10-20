package com.example.beprojectweb.service;

import com.example.beprojectweb.dto.request.HolidayRequest;
import com.example.beprojectweb.dto.response.HolidayResponse;
import com.example.beprojectweb.entity.Holiday;
import com.example.beprojectweb.repository.HolidayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service @RequiredArgsConstructor
public class HolidayService {
    private final HolidayRepository holidays;

    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @Transactional(readOnly = true)
    public List<HolidayResponse> list(Integer year) {
        List<Holiday> rows = holidays.findAll().stream()
                .filter(h -> year == null || h.getStartDate().getYear() <= year && h.getEndDate().getYear() >= year)
                .sorted(java.util.Comparator.comparing(Holiday::getStartDate)).toList();
        return rows.stream().map(this::response).toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public HolidayResponse create(HolidayRequest request) {
        validate(request);
        return response(holidays.save(Holiday.builder().name(request.getName().trim())
                .startDate(request.getStartDate()).endDate(request.getEndDate()).build()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public HolidayResponse update(UUID id, HolidayRequest request) {
        validate(request);
        Holiday holiday = holidays.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Holiday not found"));
        holiday.setName(request.getName().trim());
        holiday.setStartDate(request.getStartDate());
        holiday.setEndDate(request.getEndDate());
        return response(holiday);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void delete(UUID id) {
        Holiday holiday = holidays.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Holiday not found"));
        holidays.delete(holiday);
    }

    private void validate(HolidayRequest request) {
        if (request.getStartDate().isAfter(request.getEndDate()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate must be before or equal to endDate");
    }

    private HolidayResponse response(Holiday h) {
        return new HolidayResponse(h.getId(), h.getName(), h.getStartDate(), h.getEndDate());
    }
}
