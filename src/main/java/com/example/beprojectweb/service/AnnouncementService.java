package com.example.beprojectweb.service;

import com.example.beprojectweb.dto.request.AnnouncementRequest;
import com.example.beprojectweb.dto.request.AnnouncementBulkRequest;
import com.example.beprojectweb.dto.request.AnnouncementProgressBatchRequest;
import com.example.beprojectweb.dto.request.LessonProgressRequest;
import com.example.beprojectweb.dto.response.AnnouncementResponse;
import com.example.beprojectweb.dto.response.ScheduleWeekResponse;
import com.example.beprojectweb.entity.Announcement;
import com.example.beprojectweb.entity.DigitalPlan;
import com.example.beprojectweb.entity.LessonProgress;
import com.example.beprojectweb.entity.Registration;
import com.example.beprojectweb.entity.School;
import com.example.beprojectweb.entity.StemLesson;
import com.example.beprojectweb.entity.StemEventGradeLesson;
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
import com.example.beprojectweb.repository.StemEventLessonRepository;
import com.example.beprojectweb.repository.StemEventGradeLessonRepository;
import com.example.beprojectweb.repository.RegistrationStemEventRepository;
import com.example.beprojectweb.repository.StemPlanRepository;
import com.example.beprojectweb.repository.StemPlanVersionRepository;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnnouncementService {
    private final AnnouncementRepository announcementRepository;
    private final RegistrationRepository registrationRepository;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final StemPlanRepository stemPlanRepository;
    private final StemPlanVersionRepository stemPlanVersionRepository;
    private final DigitalPlanRepository digitalPlanRepository;
    private final StemLessonRepository stemLessonRepository;
    private final StemEventLessonRepository stemEventLessonRepository;
    private final StemEventGradeLessonRepository stemEventGradeLessonRepository;
    private final RegistrationStemEventRepository registrationStemEventRepository;
    private final ScheduleService scheduleService;
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
        UUID registrationId = parseRegistrationId(request.getRegistrationId());
        Registration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registration not found"));
        if (!registration.getSchool().getId().equals(school.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Registration does not belong to the specified school");
        }
        String service = request.getService().toLowerCase(Locale.ROOT);
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
        validateAndUpdateProgress(registration, service, request.getProgress(), "DONE", request.getWeekStart());
        return toResponse(announcementRepository.save(item));
    }

    /**
     * Bulk mark — fixed issues:
     *  1. Removed wrong constraint requiring all items to share the same service and weekStart.
     *     The frontend legitimately sends mixed services and weeks in one bulk call.
     *  2. User is loaded once instead of once per item.
     *  3. Schools and registrations are batch-loaded with a single IN query each.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_SCHOOL', 'USER')")
    @Transactional
    public List<AnnouncementResponse> markBulk(AnnouncementBulkRequest request) {
        List<AnnouncementRequest> items = request == null ? null : request.getItems();
        if (items == null || items.isEmpty() || items.size() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "items must contain between 1 and 100 announcements");
        }

        // Validate completeness and collect unique keys + IDs for batch loading
        Set<String> keys = new HashSet<>();
        Set<UUID> schoolIds = new HashSet<>();
        Set<UUID> registrationIds = new HashSet<>();
        for (AnnouncementRequest item : items) {
            if (item == null || item.getService() == null || item.getWeekStart() == null
                    || item.getRegistrationId() == null || item.getSchoolId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Each bulk announcement must include schoolId, registrationId, service, weekStart and message");
            }
            String service = item.getService().toLowerCase(Locale.ROOT);
            String key = item.getRegistrationId().trim() + ":" + service + ":" + item.getWeekStart();
            if (!keys.add(key)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Bulk request contains a duplicate school registration");
            }
            schoolIds.add(item.getSchoolId());
            registrationIds.add(parseRegistrationId(item.getRegistrationId()));
        }

        // Load user once for the entire bulk operation
        User user = currentUser();

        // Batch-load schools (single IN query instead of N individual selects)
        Map<UUID, School> schoolMap = schoolRepository.findAllById(schoolIds).stream()
                .collect(Collectors.toMap(School::getId, s -> s));

        // Batch-load registrations (single IN query)
        Map<UUID, Registration> registrationMap = registrationRepository.findAllById(registrationIds).stream()
                .collect(Collectors.toMap(Registration::getId, r -> r));

        return items.stream()
                .map(item -> markSingle(user, item, schoolMap, registrationMap))
                .toList();
    }

    /**
     * Core save logic — shared by mark() (single) and markBulk() (batch).
     * Receives pre-loaded school and registration maps to avoid redundant DB calls.
     */
    private AnnouncementResponse markSingle(User user, AnnouncementRequest request,
                                             Map<UUID, School> schoolMap,
                                             Map<UUID, Registration> registrationMap) {
        School school = schoolMap.get(request.getSchoolId());
        if (school == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "School not found");
        }
        assertSchoolAccess(user, school);
        assertMonday(request.getWeekStart());

        UUID registrationId = parseRegistrationId(request.getRegistrationId());
        Registration registration = registrationMap.get(registrationId);
        if (registration == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Registration not found");
        }
        if (!registration.getSchool().getId().equals(school.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Registration does not belong to the specified school");
        }

        String service = request.getService().toLowerCase(Locale.ROOT);
        if (request.getProgress() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "progress is required to mark an announcement");
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
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This report key is already used by another school");
        }
        item.setKey(key);
        item.setRegistrationId(registrationId.toString());
        item.setService(service);
        item.setWeekStart(request.getWeekStart());
        item.setMessage(request.getMessage().trim());
        item.setAnnouncedAt(LocalDateTime.now());
        item.setSchool(school);
        validateAndUpdateProgress(registration, service, request.getProgress(), "DONE", request.getWeekStart());
        return toResponse(announcementRepository.save(item));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_SCHOOL', 'USER')")
    @Transactional
    public void unmark(String key, AnnouncementProgressBatchRequest request) {
        User user = currentUser();
        Announcement item = announcementRepository.findByKey(key)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Announcement not found"));
        assertSchoolAccess(user, item.getSchool());
        if (request == null || request.getProgress() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "progress is required to unmark an announcement");
        }
        UUID registrationId = UUID.fromString(item.getRegistrationId());
        Registration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registration not found"));
        validateAndUpdateProgress(registration, item.getService(), request.getProgress(), "TODO", item.getWeekStart());
        announcementRepository.delete(item);
    }

    /**
     * Validate lesson targets and update progress — fixed issues:
     *  1. Batch-loads StemPlan/StemPlanVersion/DigitalPlan with single IN queries
     *     instead of findById() for each target (was O(N) queries, now O(1) + O(1)).
     *  2. Batch-loads existing LessonProgress records with findAllByPlanItemIdIn().
     *  3. Uses saveAll() for DONE updates instead of save() per record.
     */
    private void validateAndUpdateProgress(Registration registration, String service,
                                           List<LessonProgressRequest> targets, String status, LocalDate weekStart) {
        if (targets == null || targets.isEmpty()) return;

        String expectedType = service.equalsIgnoreCase("stem") ? "STEM" : "DIGITAL";
        Map<String, LocalDate> scheduledEventWeeks = new HashMap<>();
        if (expectedType.equals("STEM")) {
            for (ScheduleWeekResponse scheduledWeek : scheduleService.allWeeksForSummary(registration.getId(), "stem")) {
                for (ScheduleWeekResponse.Item item : scheduledWeek.items()) {
                    if (item.event() && item.planItemId() != null && item.lessonId() != null) {
                        scheduledEventWeeks.put(item.planItemId() + ":" + item.lessonId(), scheduledWeek.weekStart());
                    }
                }
            }
        }

        // Collect planItemIds for batch loading
        Set<UUID> planItemIds = new HashSet<>();
        for (LessonProgressRequest target : targets) {
            if (target == null || target.getPlanItemId() == null || target.getLessonId() == null
                    || target.getLessonId().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid lesson progress target");
            }
            String type = target.getPlanType() == null ? "" : target.getPlanType().toUpperCase(Locale.ROOT);
            if (!expectedType.equals(type)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid lesson progress target");
            }
            planItemIds.add(target.getPlanItemId());
        }

        // --- Validate targets by plan type (batch queries) ---
        if (expectedType.equals("STEM")) {
            // Batch-load all StemPlan records for the given IDs
            Map<UUID, String> planIdToCourseId = new HashMap<>();
            Map<UUID, UUID> planIdToRegistrationId = new HashMap<>();
            Map<UUID, Integer> planIdToGrade = new HashMap<>();
            Map<UUID, Integer> planIdToTerm = new HashMap<>();
            stemPlanRepository.findAllById(planItemIds).forEach(plan -> {
                planIdToCourseId.put(plan.getId(), plan.getCourse().getId());
                planIdToRegistrationId.put(plan.getId(), plan.getRegistration().getId());
                planIdToGrade.put(plan.getId(), plan.getGrade());
                planIdToTerm.put(plan.getId(), plan.getTerm());
            });

            // For IDs not found in StemPlan, check StemPlanVersion
            Set<UUID> missingIds = new HashSet<>(planItemIds);
            missingIds.removeAll(planIdToCourseId.keySet());
            if (!missingIds.isEmpty()) {
                stemPlanVersionRepository.findAllById(missingIds).forEach(version -> {
                    if (!version.isActive()) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                                "This STEM plan assignment is no longer active");
                    }
                    planIdToCourseId.put(version.getId(), version.getCourse().getId());
                    planIdToRegistrationId.put(version.getId(), version.getRegistration().getId());
                    planIdToGrade.put(version.getId(), version.getGrade());
                    planIdToTerm.put(version.getId(), version.getTerm());
                });
            }

            for (LessonProgressRequest target : targets) {
                String courseId = planIdToCourseId.get(target.getPlanItemId());
                UUID planRegId = planIdToRegistrationId.get(target.getPlanItemId());
                Integer grade = planIdToGrade.get(target.getPlanItemId());
                Integer term = planIdToTerm.get(target.getPlanItemId());
                if (courseId == null || planRegId == null) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "STEM plan not found");
                }
                StemLesson selectedLesson = stemLessonRepository.findById(target.getLessonId()).orElse(null);
                boolean regularLesson = selectedLesson != null && !isStemEventLesson(selectedLesson)
                        && selectedLesson.getCourse().getId().equals(courseId);
                StemEventGradeLesson eventLesson = stemEventGradeLessonRepository.findById(target.getLessonId()).orElse(null);
                boolean sharedEvent = planRegId.equals(registration.getId()) && grade != null && term != null
                        && registration.getStemGrades().contains(grade) && eventLesson != null
                        && eventLesson.getGrade().equals(grade)
                        && registrationStemEventRepository.findByRegistration_IdAndTermAndEvent_Id(
                                registration.getId(), term, eventLesson.getEvent().getId())
                                .isPresent();
                boolean eventScheduledThisWeek = weekStart.equals(scheduledEventWeeks.get(
                        target.getPlanItemId() + ":" + target.getLessonId()));
                sharedEvent = sharedEvent && eventScheduledThisWeek;
                if (!planRegId.equals(registration.getId()) || (!regularLesson && !sharedEvent)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Lesson does not belong to this registration plan");
                }
            }

        } else {
            // Batch-load all DigitalPlan records
            Map<UUID, DigitalPlan> digitalPlanMap = digitalPlanRepository.findAllById(planItemIds).stream()
                    .collect(Collectors.toMap(DigitalPlan::getId, p -> p));

            for (LessonProgressRequest target : targets) {
                DigitalPlan plan = digitalPlanMap.get(target.getPlanItemId());
                if (plan == null) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Digital plan not found");
                }
                if (!plan.getRegistration().getId().equals(registration.getId())
                        || registration.getDigitalProgram() == null
                        || !digitalLessonRepository.existsByIdAndProgramAndGrade(
                                target.getLessonId(), registration.getDigitalProgram(), plan.getGrade())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Lesson does not belong to this registration plan");
                }
            }
        }

        // --- Apply progress updates (batch) ---
        if (status.equals("TODO")) {
            // Individual deletes — no bulk delete API for (type, planItemId, lessonId) triples
            for (LessonProgressRequest target : targets) {
                lessonProgressRepository.deleteByPlanTypeAndPlanItemIdAndLessonId(
                        expectedType, target.getPlanItemId(), target.getLessonId());
            }
        } else {
            // Batch-load existing LessonProgress records for all planItemIds (single IN query)
            Map<String, LessonProgress> existingByKey = lessonProgressRepository
                    .findAllByPlanItemIdIn(planItemIds).stream()
                    .filter(p -> expectedType.equals(p.getPlanType()))
                    .collect(Collectors.toMap(
                            p -> p.getPlanItemId() + ":" + p.getLessonId(),
                            p -> p));

            List<LessonProgress> toSave = new ArrayList<>(targets.size());
            for (LessonProgressRequest target : targets) {
                String progressKey = target.getPlanItemId() + ":" + target.getLessonId();
                LessonProgress progress = existingByKey.getOrDefault(progressKey, new LessonProgress());
                progress.setPlanType(expectedType);
                progress.setPlanItemId(target.getPlanItemId());
                progress.setLessonId(target.getLessonId());
                progress.setStatus(status);
                progress.setDate(LocalDate.now());
                toSave.add(progress);
            }
            // Single batch insert/update instead of N individual saves
            lessonProgressRepository.saveAll(toSave);
        }
    }

    private boolean isStemEventLesson(StemLesson lesson) {
        return lesson.getTitle() != null
                && lesson.getTitle().trim().equalsIgnoreCase("Event tổng kết học phần");
    }

    private LocalDate weekStart(LocalDate date) {
        int day = date.getDayOfWeek().getValue();
        return date.plusDays(day >= 6 ? 8L - day : 1L - day);
    }

    private boolean isWeekNinePlan(Registration registration, Integer grade, Integer term, UUID planItemId) {
        StemPlan base = stemPlanRepository.findByRegistration_IdAndGradeAndTerm(registration.getId(), grade, term)
                .orElse(null);
        List<com.example.beprojectweb.entity.StemPlanVersion> versions = stemPlanVersionRepository
                .findAllByRegistration_IdAndGradeAndTermOrderByEffectiveFromWeekAsc(registration.getId(), grade, term)
                .stream().filter(com.example.beprojectweb.entity.StemPlanVersion::isActive).toList();
        UUID activePlanId = null;
        int activeStart = -1;
        if (base != null) {
            activePlanId = base.getId();
            activeStart = base.getEffectiveFromWeek() == null ? 1 : base.getEffectiveFromWeek();
        }
        for (com.example.beprojectweb.entity.StemPlanVersion version : versions) {
            if (version.getEffectiveFromWeek() <= 9 && version.getEffectiveFromWeek() >= activeStart) {
                activePlanId = version.getId();
                activeStart = version.getEffectiveFromWeek();
            }
        }
        return planItemId.equals(activePlanId);
    }

    private int termWeeks(Registration registration, int term) {
        Integer configured = registration.getStemTermWeeks() == null ? null : registration.getStemTermWeeks().get(term);
        return Integer.valueOf(8).equals(configured) ? 8 : 9;
    }

    // --- Helpers ---

    private UUID parseRegistrationId(String raw) {
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "registrationId must be a valid UUID");
        }
    }

    private void assertMonday(LocalDate date) {
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
        return user.getRoles() != null
                && (user.getRoles().contains(Role.ADMIN.name())
                        || user.getRoles().contains(Role.ADMIN_SCHOOL.name()));
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
