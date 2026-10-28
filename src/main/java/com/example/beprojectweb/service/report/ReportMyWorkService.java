package com.example.beprojectweb.service.report;

import com.example.beprojectweb.dto.request.report.ReportMyWorkUpdateRequest;
import com.example.beprojectweb.dto.response.report.ReportWorkResponse;
import com.example.beprojectweb.entity.User;
import com.example.beprojectweb.entity.report.ReportWork;
import com.example.beprojectweb.entity.report.ReportWorkUpdate;
import com.example.beprojectweb.exception.AppException;
import com.example.beprojectweb.exception.ErrorCode;
import com.example.beprojectweb.repository.UserRepository;
import com.example.beprojectweb.repository.report.ReportWorkRepository;
import com.example.beprojectweb.repository.report.ReportWorkUpdateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@PreAuthorize("hasRole('REPORT_MEMBER')")
public class ReportMyWorkService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final UserRepository userRepository;
    private final ReportWorkRepository workRepository;
    private final ReportWorkUpdateRepository updateRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<ReportWorkResponse> getMyWork() {
        User user = currentUser();
        UUID staffId = requireStaffId(user);
        return workRepository.findAllByOwner_IdOrderByCurrentDueDateAscTitleAsc(staffId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public ReportWorkResponse updateMyWork(UUID workId, ReportMyWorkUpdateRequest request) {
        User user = currentUser();
        UUID staffId = requireStaffId(user);
        ReportWork work = workRepository.findByIdAndOwner_Id(workId, staffId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy công việc được giao"));

        if ("Hoàn thành".equals(work.getStatus()) || work.getCompletedDate() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Công việc đã hoàn thành. Liên hệ quản lý nếu cần điều chỉnh.");
        }
        String status = request.getStatus().trim();
        String impact = request.getImpact() == null ? "" : request.getImpact().trim();
        if (("Hoàn thành".equals(status) || "Cần hỗ trợ".equals(status)) && impact.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng ghi kết quả hoặc nêu rõ nội dung cần hỗ trợ.");
        }
        if ("Chưa bắt đầu".equals(status) && request.getProgress() != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Công việc chưa bắt đầu phải có tiến độ 0%.");
        }
        if (!"Hoàn thành".equals(status) && request.getProgress() == 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ đặt 100% khi chuyển công việc sang Hoàn thành.");
        }

        work.setStatus(status);
        work.setProgress("Hoàn thành".equals(status) ? 100 : request.getProgress());
        work.setImpact(impact.isBlank() ? work.getImpact() : impact);
        work.setCompletedDate("Hoàn thành".equals(status) ? LocalDate.now(ZONE) : null);

        ReportWorkUpdate update = new ReportWorkUpdate();
        update.setWork(work);
        update.setUpdatedAt(LocalDateTime.now(ZONE));
        update.setProgress(work.getProgress());
        update.setStatus(work.getStatus());
        update.setNote(impact);
        update.setUpdatedBy(user.getUsername());
        work.getUpdates().add(update);
        updateRepository.save(update);

        work = workRepository.save(work);
        eventPublisher.publishEvent(new ReportWorkNotificationEvent(ReportWorkNotificationEvent.Type.MEMBER_UPDATED,
                work.getId(), null, null, null, null, user.getUsername(), work.getProgress(), work.getStatus(), impact));
        return toResponse(work);
    }

    private UUID requireStaffId(User user) {
        if (user.getReportStaff() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tài khoản chưa được liên kết với hồ sơ nhân sự.");
        }
        return user.getReportStaff().getId();
    }

    private User currentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTS));
    }

    private ReportWorkResponse toResponse(ReportWork work) {
        List<ReportWorkResponse.Update> updates = work.getUpdates().stream()
                .sorted(java.util.Comparator.comparing(ReportWorkUpdate::getUpdatedAt))
                .map(item -> new ReportWorkResponse.Update(item.getId(), item.getUpdatedAt(), item.getProgress(), item.getStatus(), item.getNote(), item.getUpdatedBy()))
                .toList();
        List<ReportWorkResponse.Extension> extensions = work.getExtensions().stream()
                .sorted(java.util.Comparator.comparing(com.example.beprojectweb.entity.report.ReportWorkExtension::getCreatedAt))
                .map(item -> new ReportWorkResponse.Extension(item.getId(), item.getCreatedAt(), item.getPreviousDueDate(), item.getNewDueDate(), item.getReason(), item.getExtendedBy(), item.getChangeType() == null ? "EXTENSION" : item.getChangeType()))
                .toList();
        String deadlineResult = work.getCompletedDate() != null
                ? work.getCurrentDueDate() != null && work.getCompletedDate().isAfter(work.getCurrentDueDate()) ? "Hoàn thành trễ" : "Hoàn thành"
                : work.getCurrentDueDate() != null && LocalDate.now(ZONE).isAfter(work.getCurrentDueDate()) ? "Quá hạn" : "Trong hạn";
        return new ReportWorkResponse(work.getId(), work.getPeriod(), work.getWorkGroup() == null ? work.getGroup() : work.getWorkGroup().getName(),
                work.getTitle(), work.getDescription() == null ? "" : work.getDescription(), work.getExpectedOutput() == null ? "" : work.getExpectedOutput(), work.getOwner().getId(), work.getProgress(), work.getStatus(), work.getImpact(),
                work.getWorkGroup() == null ? null : work.getWorkGroup().getId(), work.getStartDate(), work.getOriginalDueDate(),
                work.getCurrentDueDate(), work.getCompletedDate(), work.getCreatedAt(), deadlineResult, updates, extensions);
    }
}