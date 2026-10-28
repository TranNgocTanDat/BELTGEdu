package com.example.beprojectweb.service.report;

import com.example.beprojectweb.dto.request.report.ReportWorkRequest;
import com.example.beprojectweb.dto.response.report.ReportWorkResponse;
import com.example.beprojectweb.entity.report.ReportStaff;
import com.example.beprojectweb.entity.report.ReportWork;
import com.example.beprojectweb.entity.report.ReportWorkGroup;
import com.example.beprojectweb.entity.report.ReportWorkUpdate;
import com.example.beprojectweb.entity.report.ReportWorkExtension;
import com.example.beprojectweb.repository.report.ReportStaffRepository;
import com.example.beprojectweb.repository.report.ReportWorkRepository;
import com.example.beprojectweb.repository.report.ReportWorkGroupRepository;
import com.example.beprojectweb.repository.report.ReportWorkUpdateRepository;
import com.example.beprojectweb.repository.report.ReportWorkExtensionRepository;
import com.example.beprojectweb.dto.request.report.ReportWorkExtensionRequest;
import com.example.beprojectweb.dto.request.report.ReportWorkDeadlineAdjustmentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.security.core.context.SecurityContextHolder;

@Service
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'REPORT_WORK')")
public class ReportWorkService {
    private final ReportWorkRepository workRepository;
    private final ReportStaffRepository staffRepository;
    private final ReportWorkGroupRepository groupRepository;
    private final ReportWorkUpdateRepository updateRepository;
    private final ReportWorkExtensionRepository extensionRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<ReportWorkResponse> getAll() {
        return workRepository.findAllByOrderByPeriodDescTitleAsc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ReportWorkResponse> getSupportRequests() {
        return workRepository.findAllByStatusOrderByCurrentDueDateAscTitleAsc("Cần hỗ trợ")
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public ReportWorkResponse create(ReportWorkRequest request) {
        ReportWork work = new ReportWork();
        apply(work, request);
        work.setCreatedAt(LocalDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")));
        work = workRepository.save(work);
        recordUpdate(work, request.getImpact());
        eventPublisher.publishEvent(new ReportWorkNotificationEvent(ReportWorkNotificationEvent.Type.CREATED,
                work.getId(), null, null, null, null, SecurityContextHolder.getContext().getAuthentication().getName(),
                work.getProgress(), work.getStatus(), work.getImpact()));
        return toResponse(work);
    }

    @Transactional
    public ReportWorkResponse update(UUID id, ReportWorkRequest request) {
        ReportWork work = workRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report work item not found"));
        UUID previousOwnerId = work.getOwner().getId();
        boolean changed = !java.util.Objects.equals(work.getProgress(), request.getProgress())
                || !java.util.Objects.equals(work.getStatus(), request.getStatus())
                || !java.util.Objects.equals(work.getImpact(), request.getImpact());
        UUID previousGroupId = work.getWorkGroup() == null ? null : work.getWorkGroup().getId();
        boolean groupChanged = request.getGroupId() != null
                ? !java.util.Objects.equals(previousGroupId, request.getGroupId())
                : request.getGroup() != null && !java.util.Objects.equals(work.getGroup(), request.getGroup().trim());
        boolean notificationChanged = changed
                || !java.util.Objects.equals(previousOwnerId, request.getOwnerId())
                || groupChanged
                || !java.util.Objects.equals(work.getTitle(), request.getTitle().trim())
                || (request.getDescription() != null && !java.util.Objects.equals(work.getDescription(), request.getDescription().trim()))
                || (request.getExpectedOutput() != null && !java.util.Objects.equals(work.getExpectedOutput(), request.getExpectedOutput().trim()))
                || !java.util.Objects.equals(work.getStartDate(), request.getStartDate());
        apply(work, request);
        if (changed) recordUpdate(work, request.getImpact());
        work = workRepository.save(work);
        if (notificationChanged) {
            eventPublisher.publishEvent(new ReportWorkNotificationEvent(ReportWorkNotificationEvent.Type.UPDATED,
                    work.getId(), previousOwnerId, null, null, null,
                    SecurityContextHolder.getContext().getAuthentication().getName(), work.getProgress(), work.getStatus(), work.getImpact()));
        }
        return toResponse(work);
    }

    @Transactional
    public ReportWorkResponse extend(UUID id, ReportWorkExtensionRequest request) {
        ReportWork work = workRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report work item not found"));
        if (work.getCompletedDate() != null || "Hoàn thành".equals(work.getStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Không thể gia hạn công việc đã hoàn thành");
        LocalDate previous = work.getCurrentDueDate();
        if (previous == null || !request.getNewDueDate().isAfter(previous))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hạn mới phải sau hạn hoàn thành hiện tại");
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        ReportWorkExtension extension = new ReportWorkExtension();
        extension.setWork(work);
        extension.setCreatedAt(LocalDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")));
        extension.setPreviousDueDate(previous);
        extension.setNewDueDate(request.getNewDueDate());
        extension.setReason(request.getReason().trim());
        extension.setExtendedBy(username);
        extension.setChangeType("EXTENSION");
        work.getExtensions().add(extension);
        extensionRepository.save(extension);
        work.setCurrentDueDate(request.getNewDueDate());
        work.setStatus("Đang thực hiện");
        work = workRepository.save(work);
        eventPublisher.publishEvent(new ReportWorkNotificationEvent(ReportWorkNotificationEvent.Type.DEADLINE_CHANGED,
                work.getId(), null, previous, request.getNewDueDate(), "EXTENSION",
                SecurityContextHolder.getContext().getAuthentication().getName(), work.getProgress(), work.getStatus(), request.getReason()));
        return toResponse(work);
    }

    @Transactional
    public ReportWorkResponse changeDeadline(UUID id, ReportWorkExtensionRequest request) {
        ReportWork work = workRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report work item not found"));
        if (work.getCompletedDate() != null || "Hoàn thành".equals(work.getStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Không thể đổi hạn của công việc đã hoàn thành");
        if (work.getOriginalDueDate() == null || work.getCurrentDueDate() == null)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Công việc chưa có hạn hoàn thành");
        if (work.getStartDate() != null && request.getNewDueDate().isBefore(work.getStartDate()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hạn mới không được trước ngày bắt đầu");
        if (request.getNewDueDate().equals(work.getCurrentDueDate()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hạn mới đang trùng với hạn hiện tại");

        int comparisonWithOriginal = request.getNewDueDate().compareTo(work.getOriginalDueDate());
        String changeType = comparisonWithOriginal < 0 ? "SHORTENING"
                : comparisonWithOriginal > 0 ? "EXTENSION" : "RESET";
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        ReportWorkExtension change = new ReportWorkExtension();
        change.setWork(work);
        change.setCreatedAt(LocalDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")));
        change.setPreviousDueDate(work.getCurrentDueDate());
        change.setNewDueDate(request.getNewDueDate());
        change.setReason(request.getReason().trim());
        change.setExtendedBy(username);
        change.setChangeType(changeType);
        work.getExtensions().add(change);
        extensionRepository.save(change);
        work.setCurrentDueDate(request.getNewDueDate());
        work = workRepository.save(work);
        eventPublisher.publishEvent(new ReportWorkNotificationEvent(ReportWorkNotificationEvent.Type.DEADLINE_CHANGED,
                work.getId(), null, change.getPreviousDueDate(), change.getNewDueDate(), changeType,
                username, work.getProgress(), work.getStatus(), change.getReason()));
        return toResponse(work);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ReportWorkResponse adjustCompletedDeadline(UUID id, ReportWorkDeadlineAdjustmentRequest request) {
        ReportWork work = workRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report work item not found"));
        LocalDate completedDate = work.getCompletedDate() != null ? work.getCompletedDate() : work.getUpdates().stream()
                .filter(update -> "Hoàn thành".equals(update.getStatus()))
                .max(java.util.Comparator.comparing(ReportWorkUpdate::getUpdatedAt))
                .map(update -> update.getUpdatedAt().toLocalDate()).orElse(null);
        if (completedDate == null || !"Hoàn thành".equals(work.getStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Chỉ có thể điều chỉnh hạn của công việc đã hoàn thành");
        if (request.getNewDueDate().isBefore(completedDate))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hạn điều chỉnh phải cùng ngày hoặc sau ngày hoàn thành thực tế");
        if (work.getStartDate() != null && request.getNewDueDate().isBefore(work.getStartDate()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hạn điều chỉnh không được trước ngày bắt đầu");

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        ReportWorkExtension adjustment = new ReportWorkExtension();
        adjustment.setWork(work);
        adjustment.setCreatedAt(LocalDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")));
        adjustment.setPreviousDueDate(work.getCurrentDueDate() != null ? work.getCurrentDueDate() : work.getOriginalDueDate());
        adjustment.setNewDueDate(request.getNewDueDate());
        adjustment.setReason(request.getReason().trim());
        adjustment.setExtendedBy(username);
        adjustment.setChangeType("ADMIN_CORRECTION");
        work.getExtensions().add(adjustment);
        extensionRepository.save(adjustment);
        work.setCurrentDueDate(request.getNewDueDate());
        work = workRepository.save(work);
        eventPublisher.publishEvent(new ReportWorkNotificationEvent(ReportWorkNotificationEvent.Type.DEADLINE_CHANGED,
                work.getId(), null, adjustment.getPreviousDueDate(), adjustment.getNewDueDate(), "ADMIN_CORRECTION",
                username, work.getProgress(), work.getStatus(), adjustment.getReason()));
        return toResponse(work);
    }

    @Transactional
    public void delete(UUID id) {
        ReportWork work = workRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report work item not found"));
        workRepository.delete(work);
    }

    @Transactional(readOnly = true)
    public boolean hasWorkForStaff(UUID staffId) {
        return workRepository.existsByOwner_Id(staffId);
    }

    private void apply(ReportWork work, ReportWorkRequest request) {
        ReportStaff owner = staffRepository.findById(request.getOwnerId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Report work owner not found"));
        if (request.getStartDate() == null || request.getOriginalDueDate() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ngày bắt đầu và hạn hoàn thành là bắt buộc");
        if (request.getOriginalDueDate().isBefore(request.getStartDate()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hạn hoàn thành không được trước ngày bắt đầu");
        if ("Hoàn thành".equals(request.getStatus())) {
            LocalDate completedDate = request.getCompletedDate() != null ? request.getCompletedDate() : LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
            if (completedDate.isBefore(request.getStartDate()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ngày hoàn thành không được trước ngày bắt đầu");
            if (completedDate.isAfter(LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"))))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ngày hoàn thành không được ở tương lai");
        }
        ReportWorkGroup group = null;
        if (request.getGroupId() != null) {
            group = groupRepository.findById(request.getGroupId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nhóm công việc không tồn tại"));
            if (!group.isActive()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nhóm công việc đã ngừng hoạt động");
        } else if (request.getGroup() != null) {
            group = groupRepository.findByNameIgnoreCase(request.getGroup().trim()).orElse(null);
        }
        if (group == null && (request.getGroup() == null || request.getGroup().isBlank()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nhóm công việc là bắt buộc");
        work.setPeriod(request.getStartDate().toString().substring(0, 7));
        work.setWorkGroup(group);
        work.setGroup(group == null ? request.getGroup().trim() : group.getName());
        work.setTitle(request.getTitle().trim());
        if (request.getDescription() != null) work.setDescription(request.getDescription().trim());
        else if (work.getDescription() == null) work.setDescription("");
        if (request.getExpectedOutput() != null) work.setExpectedOutput(request.getExpectedOutput().trim());
        else if (work.getExpectedOutput() == null) work.setExpectedOutput("");
        work.setOwner(owner);
        work.setProgress(request.getProgress());
        work.setStatus(request.getStatus().trim());
        work.setImpact(request.getImpact() == null ? "" : request.getImpact().trim());
        work.setStartDate(request.getStartDate());
        if (work.getOriginalDueDate() == null) work.setOriginalDueDate(request.getOriginalDueDate());
        if (work.getCurrentDueDate() == null) work.setCurrentDueDate(request.getOriginalDueDate());
        if (request.getCompletedDate() != null) work.setCompletedDate(request.getCompletedDate());
        if ("Hoàn thành".equals(work.getStatus())) {
            work.setProgress(100);
            if (work.getCompletedDate() == null) work.setCompletedDate(LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")));
        } else {
            work.setCompletedDate(null);
        }
    }

    private ReportWorkResponse toResponse(ReportWork work) {
        List<ReportWorkResponse.Update> updates = work.getUpdates().stream().sorted(java.util.Comparator.comparing(ReportWorkUpdate::getUpdatedAt))
                .map(item -> new ReportWorkResponse.Update(item.getId(), item.getUpdatedAt(), item.getProgress(), item.getStatus(), item.getNote(), item.getUpdatedBy())).toList();
        List<ReportWorkResponse.Extension> extensions = work.getExtensions().stream().sorted(java.util.Comparator.comparing(ReportWorkExtension::getCreatedAt))
                .map(item -> new ReportWorkResponse.Extension(item.getId(), item.getCreatedAt(), item.getPreviousDueDate(), item.getNewDueDate(), item.getReason(), item.getExtendedBy(), item.getChangeType() == null ? "EXTENSION" : item.getChangeType())).toList();
        // Return the persisted work state. Period-specific and current overdue status is calculated by report snapshots.
        return new ReportWorkResponse(work.getId(), work.getPeriod(), work.getWorkGroup() == null ? work.getGroup() : work.getWorkGroup().getName(), work.getTitle(), work.getDescription() == null ? "" : work.getDescription(), work.getExpectedOutput() == null ? "" : work.getExpectedOutput(), work.getOwner().getId(), work.getProgress(), work.getStatus(), work.getImpact(), work.getWorkGroup() == null ? null : work.getWorkGroup().getId(), work.getStartDate(), work.getOriginalDueDate(), work.getCurrentDueDate(), work.getCompletedDate(), work.getCreatedAt(), deadlineResult(work), updates, extensions);
    }

    private String deadlineResult(ReportWork work) {
        if (work.getCompletedDate() != null) {
            ReportWorkExtension latestChange = work.getExtensions().stream()
                    .max(java.util.Comparator.comparing(ReportWorkExtension::getCreatedAt)).orElse(null);
            LocalDate effectiveDueDate = work.getCurrentDueDate();
            if (effectiveDueDate != null && work.getCompletedDate().isAfter(effectiveDueDate)) return "Hoàn thành trễ";
            String changeType = latestChange == null ? null : latestChange.getChangeType();
            if ("ADMIN_CORRECTION".equals(changeType) && work.getOriginalDueDate() != null
                    && work.getCompletedDate().isAfter(work.getOriginalDueDate())) return "Đúng hạn sau điều chỉnh";
            if ("SHORTENING".equals(changeType)) return "Đúng hạn theo hạn rút ngắn";
            if (("EXTENSION".equals(changeType) || changeType == null) && latestChange != null
                    && work.getOriginalDueDate() != null && work.getCompletedDate().isAfter(work.getOriginalDueDate()))
                return "Đúng hạn sau gia hạn";
            return "Đúng hạn ban đầu";
        }
        return work.getCurrentDueDate() != null && LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).isAfter(work.getCurrentDueDate()) ? "Quá hạn" : "Trong hạn";
    }

    private void recordUpdate(ReportWork work, String note) {
        ReportWorkUpdate update = new ReportWorkUpdate();
        update.setWork(work);
        update.setUpdatedAt(LocalDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")));
        update.setProgress(work.getProgress());
        update.setStatus(work.getStatus());
        update.setNote(note == null ? "" : note.trim());
        update.setUpdatedBy(SecurityContextHolder.getContext().getAuthentication().getName());
        work.getUpdates().add(update);
        updateRepository.save(update);
    }
}
