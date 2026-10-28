package com.example.beprojectweb.service.report;

import com.example.beprojectweb.entity.User;
import com.example.beprojectweb.entity.report.ReportWork;
import com.example.beprojectweb.enums.Role;
import com.example.beprojectweb.repository.UserRepository;
import com.example.beprojectweb.repository.report.ReportWorkRepository;
import com.example.beprojectweb.service.EmailService;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReportWorkNotificationListener {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final List<String> MANAGER_ROLES = List.of(Role.ADMIN.name(), Role.REPORT_WORK.name());

    private final ReportWorkRepository workRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Async("reportMailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNotification(ReportWorkNotificationEvent event) {
        try {
            ReportWork work = workRepository.findNotificationWorkById(event.workId()).orElse(null);
            if (work == null) return;
            switch (event.type()) {
                case CREATED -> {
                    if (!"Hoàn thành".equals(work.getStatus())) {
                        notifyAssignedMember(work, "Công việc mới được giao", "Bạn vừa được giao một công việc mới.", event);
                    }
                }
                case UPDATED -> notifyWorkChange(work, event);
                case DEADLINE_CHANGED -> notifyDeadlineChange(work, event);
                case MEMBER_UPDATED -> notifyManagers(work, event);
            }
        } catch (Exception exception) {
            // Email failure must not roll back the saved work or escape the asynchronous listener.
            log.warn("Could not send report work notification for workId={}", event.workId(), exception);
        }
    }

    private void notifyWorkChange(ReportWork work, ReportWorkNotificationEvent event) {
        UUID currentOwnerId = work.getOwner().getId();
        if (event.previousOwnerId() != null && !event.previousOwnerId().equals(currentOwnerId)) {
            userRepository.findByReportStaff_IdAndEnabledTrue(event.previousOwnerId())
                    .filter(this::isReportMember)
                    .ifPresent(user -> send(user, "Công việc đã chuyển người phụ trách: " + work.getTitle(),
                            "Công việc này đã được chuyển sang nhân sự khác." + workSummary(work)));
            notifyAssignedMember(work, "Công việc mới được giao", "Bạn vừa được giao công việc này.", event);
            return;
        }
        notifyAssignedMember(work, "Công việc đã được cập nhật", "Quản lý vừa cập nhật công việc được giao cho bạn.", event);
    }

    private void notifyDeadlineChange(ReportWork work, ReportWorkNotificationEvent event) {
        String kind = switch (event.changeType() == null ? "EXTENSION" : event.changeType()) {
            case "SHORTENING" -> "Rút ngắn thời hạn";
            case "RESET" -> "Khôi phục thời hạn ban đầu";
            case "ADMIN_CORRECTION" -> "Điều chỉnh thời hạn";
            default -> "Gia hạn công việc";
        };
        String extra = "<p><b>Hạn trước:</b> " + date(event.previousDueDate()) + "</p>"
                + "<p><b>Hạn mới:</b> " + date(event.newDueDate()) + "</p>"
                + "<p><b>Lý do:</b> " + escape(event.note()) + "</p>";
        notifyAssignedMember(work, kind + ": " + work.getTitle(), kind + " cho công việc của bạn.", event, extra);
    }

    private void notifyManagers(ReportWork work, ReportWorkNotificationEvent event) {
        String status = event.status() == null ? work.getStatus() : event.status();
        boolean needsHelp = "Cần hỗ trợ".equals(status);
        String subject = (needsHelp ? "[CẦN HỖ TRỢ] " : "[Cập nhật công việc] ") + work.getTitle();
        String lead = needsHelp
                ? "Nhân viên vừa gửi yêu cầu cần hỗ trợ. Vui lòng xem xét và phản hồi."
                : "Nhân viên vừa cập nhật tiến độ công việc.";
        String extra = "<p><b>Nhân viên:</b> " + escape(work.getOwner().getName()) + "</p>"
                + "<p><b>Trạng thái:</b> " + escape(status) + "</p>"
                + "<p><b>Tiến độ:</b> " + (event.progress() == null ? work.getProgress() : event.progress()) + "%</p>"
                + "<p><b>Nội dung cập nhật:</b><br>" + escape(event.note()) + "</p>";
        String html = emailTemplate(lead, work, extra);
        Set<String> sentTo = new HashSet<>();
        List<User> managers = userRepository.findAllEnabledWithAnyRoleAndEmail(MANAGER_ROLES);
        for (User manager : managers) {
            if (event.actor() != null && event.actor().equals(manager.getUsername())) continue;
            sendOnce(manager, subject, html, sentTo);
        }
        if (sentTo.isEmpty()) {
            log.warn("No active ADMIN/REPORT_WORK email recipient found for workId={}", work.getId());
        }
    }

    private void notifyAssignedMember(ReportWork work, String subjectPrefix, String lead, ReportWorkNotificationEvent event) {
        notifyAssignedMember(work, subjectPrefix, lead, event, "");
    }

    private void notifyAssignedMember(ReportWork work, String subjectPrefix, String lead,
                                      ReportWorkNotificationEvent event, String extra) {
        var assignedUser = userRepository.findByReportStaff_IdAndEnabledTrue(work.getOwner().getId());
        if (assignedUser.isEmpty()) {
            log.warn("No active account is linked to assigned reportStaffId={} for workId={}", work.getOwner().getId(), work.getId());
            return;
        }
        User user = assignedUser.get();
        if (!isReportMember(user)) {
            log.warn("Linked account is not REPORT_MEMBER for reportStaffId={} and workId={}", work.getOwner().getId(), work.getId());
            return;
        }
        send(user, subjectPrefix + ": " + work.getTitle(), emailTemplate(lead, work, extra));
    }

    private boolean isReportMember(User user) {
        return user.getRoles() != null && user.getRoles().contains(Role.REPORT_MEMBER.name());
    }

    private void send(User user, String subject, String html) {
        if (user.getEmail() == null || user.getEmail().isBlank()) return;
        try {
            emailService.sendHtmlEmail(user.getEmail(), subject, html);
        } catch (MessagingException | RuntimeException exception) {
            log.warn("Could not send report work email to userId={}", user.getId(), exception);
        }
    }

    private void sendOnce(User user, String subject, String html, Set<String> sentTo) {
        if (user.getEmail() == null || user.getEmail().isBlank()) return;
        String normalizedEmail = user.getEmail().trim().toLowerCase(java.util.Locale.ROOT);
        if (sentTo.add(normalizedEmail)) send(user, subject, html);
    }

    private String emailTemplate(String lead, ReportWork work, String extra) {
        String group = work.getWorkGroup() == null ? work.getGroup() : work.getWorkGroup().getName();
        return "<html><body style='font-family:Arial,sans-serif;color:#172033;line-height:1.55'>"
                + "<div style='max-width:640px;margin:0 auto;border:1px solid #e2e8f0;border-radius:12px;overflow:hidden'>"
                + "<div style='background:#b91c1c;color:white;padding:16px 20px;font-size:18px;font-weight:700'>Báo cáo công việc</div>"
                + "<div style='padding:20px'><p>" + escape(lead) + "</p>"
                + "<h2 style='font-size:18px;margin:16px 0 8px'>" + escape(work.getTitle()) + "</h2>"
                + "<p style='color:#64748b;margin-top:0'>" + escape(group) + " · " + escape(work.getPeriod()) + "</p>"
                + extra
                + (hasText(work.getDescription()) ? "<p><b>Mô tả:</b><br>" + escape(work.getDescription()) + "</p>" : "")
                + (hasText(work.getExpectedOutput()) ? "<p><b>Đầu ra mong đợi:</b><br>" + escape(work.getExpectedOutput()) + "</p>" : "")
                + "<p><b>Ngày bắt đầu:</b> " + date(work.getStartDate()) + "</p>"
                + "<p><b>Hạn hiện tại:</b> " + date(work.getCurrentDueDate()) + "</p>"
                + "<p><b>Trạng thái:</b> " + escape(work.getStatus()) + " · <b>Tiến độ:</b> " + work.getProgress() + "%</p>"
                + "<p style='margin-top:24px;color:#64748b;font-size:12px'>Thông báo tự động từ hệ thống Báo cáo công việc.</p>"
                + "</div></div></body></html>";
    }

    private String workSummary(ReportWork work) {
        return "<p><b>Công việc:</b> " + escape(work.getTitle()) + "</p><p>Người phụ trách mới: "
                + escape(work.getOwner().getName()) + "</p>";
    }

    private String date(LocalDate value) {
        return value == null ? "—" : DATE_FORMAT.format(value);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String escape(String value) {
        if (value == null || value.isBlank()) return "—";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;").replace("\r\n", "<br>")
                .replace("\n", "<br>").replace("\r", "<br>");
    }
}
