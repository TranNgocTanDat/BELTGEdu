package com.example.beprojectweb.service.report;

import com.example.beprojectweb.dto.request.report.ReportGoalRequest;
import com.example.beprojectweb.dto.response.report.ReportGoalResponse;
import com.example.beprojectweb.entity.report.ReportGoal;
import com.example.beprojectweb.repository.report.ReportGoalRepository;
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
public class ReportGoalService {
    private final ReportGoalRepository repository;

    @Transactional(readOnly = true)
    public List<ReportGoalResponse> getAll() {
        return repository.findAllByOrderByPeriodDescTitleAsc().stream().map(this::toResponse).toList();
    }

    @Transactional
    public ReportGoalResponse create(ReportGoalRequest request) {
        ReportGoal goal = new ReportGoal();
        apply(goal, request);
        return toResponse(repository.save(goal));
    }

    @Transactional
    public ReportGoalResponse update(UUID id, ReportGoalRequest request) {
        ReportGoal goal = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report goal not found"));
        apply(goal, request);
        return toResponse(repository.save(goal));
    }

    @Transactional
    public void delete(UUID id) {
        ReportGoal goal = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report goal not found"));
        repository.delete(goal);
    }

    private void apply(ReportGoal goal, ReportGoalRequest request) {
        goal.setPeriod(request.getPeriod().trim());
        goal.setTitle(request.getTitle().trim());
        goal.setOutput(request.getOutput().trim());
        goal.setOwner(request.getOwner().trim());
        int progress = request.getProgress();
        String status = request.getStatus().trim();
        if ("Hoàn thành".equals(status)) progress = 100;
        else if (progress == 100) status = "Hoàn thành";
        goal.setProgress(progress);
        goal.setStatus(status);
    }

    private ReportGoalResponse toResponse(ReportGoal goal) {
        return new ReportGoalResponse(goal.getId(), goal.getPeriod(), goal.getTitle(), goal.getOutput(), goal.getOwner(), goal.getProgress(), goal.getStatus());
    }
}
