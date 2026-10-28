package com.example.beprojectweb.service.report;

import com.example.beprojectweb.dto.request.report.ReportStaffRequest;
import com.example.beprojectweb.dto.response.report.ReportStaffResponse;
import com.example.beprojectweb.entity.report.ReportStaff;
import com.example.beprojectweb.repository.report.ReportStaffRepository;
import com.example.beprojectweb.repository.report.ReportWorkRepository;
import com.example.beprojectweb.repository.UserRepository;
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
public class ReportStaffService {
    private final ReportStaffRepository repository;
    private final ReportWorkRepository workRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<ReportStaffResponse> getAll() {
        return repository.findAllByOrderByNameAsc().stream().map(this::toResponse).toList();
    }

    @Transactional
    public ReportStaffResponse create(ReportStaffRequest request) {
        ReportStaff staff = new ReportStaff();
        apply(staff, request);
        return toResponse(repository.save(staff));
    }

    @Transactional
    public ReportStaffResponse update(UUID id, ReportStaffRequest request) {
        ReportStaff staff = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report staff not found"));
        apply(staff, request);
        return toResponse(repository.save(staff));
    }

    @Transactional
    public void delete(UUID id) {
        ReportStaff staff = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report staff not found"));
        if (userRepository.existsByReportStaff_Id(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Unlink the login account before deleting this staff profile");
        }
        if (workRepository.existsByOwner_Id(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reassign work items before deleting this staff member");
        }
        repository.delete(staff);
    }

    private void apply(ReportStaff staff, ReportStaffRequest request) {
        staff.setName(request.getName().trim());
        staff.setRole(request.getRole().trim());
        staff.setScope(request.getScope().trim());
        staff.setOutput(request.getOutput().trim());
        staff.setStatus(request.getStatus().trim());
    }

    private ReportStaffResponse toResponse(ReportStaff staff) {
        return new ReportStaffResponse(staff.getId(), staff.getName(), staff.getRole(), staff.getScope(), staff.getOutput(), staff.getStatus());
    }
}
