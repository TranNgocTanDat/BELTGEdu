package com.example.beprojectweb.service.report;

import com.example.beprojectweb.dto.request.report.ReportKpiRequest;
import com.example.beprojectweb.dto.response.report.ReportKpiResponse;
import com.example.beprojectweb.entity.report.ReportKpi;
import com.example.beprojectweb.repository.report.ReportKpiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'REPORT_WORK')")
public class ReportKpiService {
    private final ReportKpiRepository repository;

    @Transactional(readOnly = true)
    public List<ReportKpiResponse> getAll() {
        return repository.findAllByOrderByPeriodDescKpiKeyAsc().stream().map(this::toResponse).toList();
    }

    @Transactional
    public ReportKpiResponse create(ReportKpiRequest request) {
        if (repository.existsByPeriodAndKpiKey(request.getPeriod(), request.getKey())) {
            throw duplicate();
        }
        ReportKpi kpi = new ReportKpi();
        apply(kpi, request);
        return toResponse(repository.save(kpi));
    }

    @Transactional
    public ReportKpiResponse update(UUID id, ReportKpiRequest request) {
        ReportKpi kpi = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report KPI not found"));
        if (repository.existsByPeriodAndKpiKeyAndIdNot(request.getPeriod(), request.getKey(), id)) {
            throw duplicate();
        }
        apply(kpi, request);
        return toResponse(repository.save(kpi));
    }

    @Transactional
    public void delete(UUID id) {
        ReportKpi kpi = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report KPI not found"));
        repository.delete(kpi);
    }

    private void apply(ReportKpi kpi, ReportKpiRequest request) {
        kpi.setPeriod(request.getPeriod().trim());
        kpi.setKpiKey(request.getKey().trim());
        kpi.setTarget(request.getTarget());
        kpi.setActual(request.getActual());
    }

    private ReportKpiResponse toResponse(ReportKpi kpi) {
        return new ReportKpiResponse(kpi.getId(), kpi.getPeriod(), kpi.getKpiKey(), kpi.getTarget(), kpi.getActual());
    }

    private ResponseStatusException duplicate() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "KPI already exists for this period");
    }
}
