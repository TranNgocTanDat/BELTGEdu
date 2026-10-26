package com.example.beprojectweb.service.report;

import com.example.beprojectweb.dto.request.report.ReportIssueRequest;
import com.example.beprojectweb.dto.response.report.ReportIssueResponse;
import com.example.beprojectweb.entity.report.ReportIssue;
import com.example.beprojectweb.repository.report.ReportIssueRepository;
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
public class ReportIssueService {
    private final ReportIssueRepository repository;

    @Transactional(readOnly = true)
    public List<ReportIssueResponse> getAll() {
        return repository.findAllByOrderByLevelAscTitleAsc().stream().map(this::toResponse).toList();
    }

    @Transactional
    public ReportIssueResponse create(ReportIssueRequest request) {
        ReportIssue issue = new ReportIssue();
        apply(issue, request);
        return toResponse(repository.save(issue));
    }

    @Transactional
    public ReportIssueResponse update(UUID id, ReportIssueRequest request) {
        ReportIssue issue = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report issue not found"));
        apply(issue, request);
        return toResponse(repository.save(issue));
    }

    @Transactional
    public void delete(UUID id) {
        ReportIssue issue = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report issue not found"));
        repository.delete(issue);
    }

    private void apply(ReportIssue issue, ReportIssueRequest request) {
        issue.setTitle(request.getTitle().trim());
        issue.setLevel(request.getLevel().trim());
        issue.setProposal(request.getProposal().trim());
        issue.setKind(request.getKind() == null || request.getKind().isBlank() ? "ISSUE" : request.getKind());
        issue.setOwner(request.getOwner());
        issue.setDueDate(request.getDueDate());
        issue.setPeriod(request.getPeriod());
        issue.setStatus(request.getStatus() == null || request.getStatus().isBlank() ? "OPEN" : request.getStatus());
        issue.setImpact(request.getImpact());
    }

    private ReportIssueResponse toResponse(ReportIssue issue) {
        return new ReportIssueResponse(issue.getId(), issue.getTitle(), issue.getLevel(), issue.getProposal(), issue.getKind(), issue.getOwner(), issue.getDueDate(), issue.getPeriod(), issue.getStatus(), issue.getImpact());
    }
}
