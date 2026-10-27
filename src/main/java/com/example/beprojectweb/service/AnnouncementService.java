package com.example.beprojectweb.service;

import com.example.beprojectweb.dto.request.AnnouncementRequest;
import com.example.beprojectweb.dto.request.AnnouncementBulkRequest;
import com.example.beprojectweb.dto.request.AnnouncementProgressBatchRequest;
import com.example.beprojectweb.dto.request.LessonProgressRequest;
import com.example.beprojectweb.dto.response.AnnouncementResponse;
import com.example.beprojectweb.entity.Announcement;
import com.example.beprojectweb.entity.DigitalPlan;
import com.example.beprojectweb.entity.LessonProgress;
import com.example.beprojectweb.entity.Registration;
import com.example.beprojectweb.entity.School;
import com.example.beprojectweb.entity.StemPlan;
import com.example.beprojectweb.entity.User;
import com.example.beprojectweb.enums.Role;
import com.example.beprojectweb.exception.AppException;
import com.example.beprojectweb.exception.ErrorCode;
import com.example.beprojectweb.repository.AnnouncementRepository;
import com.example.beprojectweb.repository.DigitalLessonRepository;
import com.example.beprojectweb.repository.DigitalPlanRepository;
import com.example.beprojectweb.repository.LessonProgressRepository;
import com.example.beprojectweb.repository.RegistrationRepository;
import com.example.beprojectweb.repository.SchoolRepository;
import com.example.beprojectweb.repository.StemLessonRepository;
import com.example.beprojectweb.repository.StemPlanRepository;
import com.example.beprojectweb.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnnouncementService {
    private final AnnouncementRepository announcementRepository;
    private final RegistrationRepository registrationRepository;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final StemPlanRepository stemPlanRepository;
    private final com.example.beprojectweb.repository.StemPlanVersionRepository stemPlanVersionRepository;
    private final DigitalPlanRepository digitalPlanRepository;
    private final StemLessonRepository stemLessonRepository;
    private final DigitalLessonRepository digitalLessonRepository;
    private final LessonProgressRepository lessonProgressRepository;

    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_SCHOOL', 'USER')")
    @Transactional(readOnly = true)
    public List<AnnouncementResponse> getAnnouncements(UUID schoolId) {
        User user = currentUser();
        List<Announcement> items;
        if (schoolId != null) {
            School school = findSchool(schoolId);
            assertSchoolAccess(user, school);
            items = announcementRepository.findAllBySchool_IdOrderByWeekStartAsc(schoolId);
        } else if (isAdmin(user)) {
            items = announcementRepository.findAllByOrderByWeekStartAsc();
        } else if (user.getRegion() != null) {
            items = announcementRepository.findAllBySchool_Region_IdOrderByWeekStartAsc(user.getRegion().getId());
        } else {
            items = List.of();
        }
        return items.stream().map(this::toResponse).toList();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_SCHOOL', 'USER')")
    @Transactional
    public AnnouncementResponse mark(AnnouncementRequest request) {
        User user = currentUser();
        School school = findSchool(request.getSchoolId());
        assertSchoolAccess(user, school);
        assertMonday(request.getWeekStart());
        UUID registrationId;
        try {
            registrationId = UUID.fromString(request.getRegistrationId().trim());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "registrationId must be a valid UUID");
        }
        Registration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registration not found"));
        if (!registration.getSchool().getId().equals(school.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Registration does not belong to the specified school");
        }
        String service = request.getService().toLowerCase(java.util.Locale.ROOT);
        if (request.getProgress() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "progress is required to mark an announcement");
        }
        boolean enabled = service.equals("stem")
                ? registration.getStemProgram() != null
                : registration.getDigitalProgram() != null;
        if (!enabled) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Registration does not have the requested program");
        }
        String key = registrationId + ":" + service + ":" + request.getWeekStart();
        Announcement item = announcementRepository.findByKey(key).orElseGet(Announcement::new);
        if (item.getSchool() != null && !item.getSchool().getId().equals(school.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This report key is already used by another school");
        }
        item.setKey(key);
        item.setRegistrationId(registrationId.toString());
        item.setService(service);
        item.setWeekStart(request.getWeekStart());
        item.setMessage(request.getMessage().trim());
        item.setAnnouncedAt(LocalDateTime.now());
        item.setSchool(school);
        validateAndUpdateProgress(registration, service, request.getProgress(), "DONE");
        return toResponse(announcementRepository.save(item));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_SCHOOL', 'USER')")
    @Transactional
    public List<AnnouncementResponse> markBulk(AnnouncementBulkRequest request) {
        List<AnnouncementRequest> items = request == null ? null : request.getItems();
        if (items == null || items.isEmpty() || items.size() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "items must contain between 1 and 100 announcements");
        }
        if (items.stream().anyMatch(java.util.Objects::isNull) || items.get(0).getService() == null || items.get(0).getWeekStart() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Each bulk announcement must be complete");
        }
        String service = items.get(0).getService().toLowerCase(Locale.ROOT);
        LocalDate weekStart = items.get(0).getWeekStart();
        Set<String> keys = new HashSet<>();
        for (AnnouncementRequest item : items) {
            if (item.getService() == null || item.getWeekStart() == null
                    || !service.equals(item.getService().toLowerCase(Locale.ROOT)) || !weekStart.equals(item.getWeekStart())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bulk announcements must use the same service and actual week");
            }
            String key = item.getRegistrationId() + ":" + service + ":" + weekStart;
            if (!keys.add(key)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bulk request contains a duplicate school registration");
            }
        }
        return items.stream().map(this::mark).toList();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_SCHOOL', 'USER')")
    @Transactional
    public void unmark(String key, AnnouncementProgressBatchRequest request) {
        User user = currentUser();
        Announcement item = announcementRepository.findByKey(key)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Announcement not found"));
        assertSchoolAccess(user, item.getSchool());
        if (request == null || request.getProgress() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "progress is required to unmark an announcement");
        }
        UUID registrationId = UUID.fromString(item.getRegistrationId());
        Registration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registration not found"));
        validateAndUpdateProgress(registration, item.getService(), request == null ? null : request.getProgress(), "TODO");
        announcementRepository.delete(item);
    }

    private void validateAndUpdateProgress(Registration registration, String service,
                                           List<LessonProgressRequest> targets, String status) {
        String expectedType = service.equalsIgnoreCase("stem") ? "STEM" : "DIGITAL";
        List<LessonProgressRequest> safeTargets = targets;
        for (LessonProgressRequest target : safeTargets) {
            if (target == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid lesson progress target");
            }
            String type = target.getPlanType() == null ? "" : target.getPlanType().toUpperCase(Locale.ROOT);
            if (!expectedType.equals(type) || target.getPlanItemId() == null || target.getLessonId() == null || target.getLessonId().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid lesson progress target");
            }
            if (expectedType.equals("STEM")) {
                StemPlan plan = stemPlanRepository.findById(target.getPlanItemId()).orElse(null);
                String courseId;
                UUID planRegistrationId;
                if (plan != null) {
                    courseId = plan.getCourse().getId();
                    planRegistrationId = plan.getRegistration().getId();
                } else {
                    com.example.beprojectweb.entity.StemPlanVersion version = stemPlanVersionRepository.findById(target.getPlanItemId())
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "STEM plan not found"));
                    if (!version.isActive()) throw new ResponseStatusException(HttpStatus.CONFLICT, "This STEM plan assignment is no longer active");
                    courseId = version.getCourse().getId();
                    planRegistrationId = version.getRegistration().getId();
                }
                if (!planRegistrationId.equals(registration.getId())
                        || !stemLessonRepository.existsByIdAndCourse_Id(target.getLessonId(), courseId)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lesson does not belong to this registration plan");
                }
            } else {
                DigitalPlan plan = digitalPlanRepository.findById(target.getPlanItemId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Digital plan not found"));
                if (!plan.getRegistration().getId().equals(registration.getId())
                        || registration.getDigitalProgram() == null
                        || !digitalLessonRepository.existsByIdAndProgramAndGrade(target.getLessonId(), registration.getDigitalProgram(), plan.getGrade())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lesson does not belong to this registration plan");
                }
            }
        }
        for (LessonProgressRequest target : safeTargets) {
            String type = expectedType;
            if (status.equals("TODO")) {
                lessonProgressRepository.deleteByPlanTypeAndPlanItemIdAndLessonId(type, target.getPlanItemId(), target.getLessonId());
            } else {
                LessonProgress progress = lessonProgressRepository.findByPlanTypeAndPlanItemIdAndLessonId(type, target.getPlanItemId(), target.getLessonId())
                        .orElseGet(LessonProgress::new);
                progress.setPlanType(type);
                progress.setPlanItemId(target.getPlanItemId());
                progress.setLessonId(target.getLessonId());
                progress.setStatus(status);
                progress.setDate(LocalDate.now());
                lessonProgressRepository.save(progress);
            }
        }
    }

    private void assertMonday(java.time.LocalDate date) {
        if (date.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "weekStart must be a Monday");
        }
    }

    private School findSchool(UUID id) {
        return schoolRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "School not found"));
    }

    private void assertSchoolAccess(User user, School school) {
        if (isAdmin(user)) return;
        UUID userRegion = user.getRegion() == null ? null : user.getRegion().getId();
        if (userRegion == null || !userRegion.equals(school.getRegion().getId())) {
            throw new AccessDeniedException("You can only manage announcements for schools in your region");
        }
    }

    private User currentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTS));
    }

    private boolean isAdmin(User user) {
        return user.getRoles() != null && (user.getRoles().contains(Role.ADMIN.name()) || user.getRoles().contains(Role.ADMIN_SCHOOL.name()));
    }

    private AnnouncementResponse toResponse(Announcement item) {
        return AnnouncementResponse.builder()
                .id(item.getId())
                .key(item.getKey())
                .schoolId(item.getSchool().getId())
                .registrationId(item.getRegistrationId())
                .service(item.getService())
                .weekStart(item.getWeekStart())
                .message(item.getMessage())
                .announcedAt(item.getAnnouncedAt())
                .build();
    }
}
