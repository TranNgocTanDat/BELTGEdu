package com.example.beprojectweb.service;

import com.example.beprojectweb.dto.request.RegistrationStemEventsRequest;
import com.example.beprojectweb.dto.request.StemEventGradeLessonRequest;
import com.example.beprojectweb.dto.request.StemEventTemplateRequest;
import com.example.beprojectweb.dto.response.RegistrationStemEventResponse;
import com.example.beprojectweb.dto.response.StemEventGradeLessonResponse;
import com.example.beprojectweb.dto.response.StemEventTemplateResponse;
import com.example.beprojectweb.entity.Registration;
import com.example.beprojectweb.entity.RegistrationStemEvent;
import com.example.beprojectweb.entity.Holiday;
import com.example.beprojectweb.entity.SchoolClosure;
import com.example.beprojectweb.entity.StemEventGradeLesson;
import com.example.beprojectweb.entity.StemEventLesson;
import com.example.beprojectweb.entity.StemEventTemplate;
import com.example.beprojectweb.entity.User;
import com.example.beprojectweb.enums.Role;
import com.example.beprojectweb.exception.AppException;
import com.example.beprojectweb.exception.ErrorCode;
import com.example.beprojectweb.repository.RegistrationRepository;
import com.example.beprojectweb.repository.RegistrationStemEventRepository;
import com.example.beprojectweb.repository.StemEventGradeLessonRepository;
import com.example.beprojectweb.repository.StemEventLessonRepository;
import com.example.beprojectweb.repository.StemEventTemplateRepository;
import com.example.beprojectweb.repository.HolidayRepository;
import com.example.beprojectweb.repository.SchoolClosureRepository;
import com.example.beprojectweb.repository.UserRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StemEventManagementService {
    private static final String EVENT_20_11_ID = "stem-event-20-11";
    private static final String EVENT_20_11_INTRO = "Nhân dịp kỷ niệm Ngày Nhà giáo Việt Nam 20/11, trong tuần này, Quý Thầy/Cô sẽ triển khai giảng dạy các bài học Event với chủ đề chào mừng Ngày Nhà giáo Việt Nam theo nội dung thông báo báo bài như sau:";
    private static final String EVENT_20_11_GREETING = "💐 Nhân dịp Ngày Nhà giáo Việt Nam 20/11, kính chúc Quý Thầy/Cô luôn mạnh khỏe, hạnh phúc, tràn đầy nhiệt huyết và thành công trong sự nghiệp trồng người. Chúc Quý Thầy/Cô có một ngày lễ thật ý nghĩa, nhiều niềm vui và nhận được thật nhiều tình cảm yêu thương từ các thế hệ học sinh!";

    private final StemEventTemplateRepository templateRepository;
    private final StemEventGradeLessonRepository gradeLessonRepository;
    private final StemEventLessonRepository legacyEventRepository;
    private final RegistrationStemEventRepository assignmentRepository;
    private final RegistrationRepository registrationRepository;
    private final UserRepository userRepository;
    private final SchoolClosureRepository closureRepository;
    private final HolidayRepository holidayRepository;
    private final JdbcTemplate jdbcTemplate;

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('USER','ADMIN','ADMIN_SCHOOL')")
    public List<StemEventTemplateResponse> getTemplates() {
        List<StemEventTemplate> templates = templateRepository.findAllByOrderByNameAsc();
        if (templates.isEmpty()) return List.of();
        Map<String, List<StemEventGradeLessonResponse>> lessons = gradeLessonRepository
                .findAllByEvent_IdInOrderByEvent_IdAscGradeAsc(templates.stream().map(StemEventTemplate::getId).toList())
                .stream().map(this::toGradeLessonResponse)
                .collect(Collectors.groupingBy(StemEventGradeLessonResponse::eventId, HashMap::new, Collectors.toList()));
        return templates.stream().map(template -> new StemEventTemplateResponse(template.getId(), template.getName(),
                template.getAnnouncementIntro(), template.getAnnouncementGreeting(),
                lessons.getOrDefault(template.getId(), List.of()))).toList();
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','ADMIN_SCHOOL')")
    public StemEventTemplateResponse createTemplate(StemEventTemplateRequest request) {
        String name = request.getName().trim();
        if (templateRepository.findAllByOrderByNameAsc().stream().anyMatch(item -> item.getName().equalsIgnoreCase(name))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tên Event đã tồn tại.");
        }
        StemEventTemplate template = templateRepository.save(StemEventTemplate.builder()
                .id(UUID.randomUUID().toString()).name(name)
                .announcementIntro(normalizeOptional(request.getAnnouncementIntro()))
                .announcementGreeting(normalizeOptional(request.getAnnouncementGreeting())).build());
        saveGradeLessons(template, request.getGradeLessons());
        return getTemplate(template);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','ADMIN_SCHOOL')")
    public StemEventTemplateResponse updateTemplate(String id, StemEventTemplateRequest request) {
        StemEventTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event không tồn tại."));
        String name = request.getName().trim();
        if (templateRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tên Event đã tồn tại.");
        }
        template.setName(name);
        template.setAnnouncementIntro(normalizeOptional(request.getAnnouncementIntro()));
        template.setAnnouncementGreeting(normalizeOptional(request.getAnnouncementGreeting()));
        templateRepository.save(template);
        saveGradeLessons(template, request.getGradeLessons());
        return getTemplate(template);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('USER','ADMIN','ADMIN_SCHOOL')")
    public List<RegistrationStemEventResponse> getAssignments() {
        User user = currentUser();
        List<Registration> visible = isAdmin(user) ? registrationRepository.findAll()
                : user.getRegion() == null ? List.of()
                        : registrationRepository.findAllBySchool_Region_Id(user.getRegion().getId());
        if (visible.isEmpty()) return List.of();
        return assignmentRepository.findAllByRegistration_IdInOrderByRegistration_IdAscTermAscEventDateAsc(
                visible.stream().map(Registration::getId).toList()).stream().map(this::toAssignmentResponse).toList();
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','ADMIN_SCHOOL')")
    public List<RegistrationStemEventResponse> replaceAssignments(UUID registrationId,
            RegistrationStemEventsRequest request) {
        Registration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Đăng ký không tồn tại."));
        assertAccess(currentUser(), registration);
        if (registration.getStemProgram() == null && request.getAssignments() != null && !request.getAssignments().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đăng ký này chưa bật STEM.");
        }

        List<RegistrationStemEventsRequest.Assignment> requested = request.getAssignments() == null
                ? List.of() : request.getAssignments();
        List<RegistrationStemEvent> existing = assignmentRepository
                .findAllByRegistration_IdOrderByTermAscEventDateAsc(registrationId);
        Map<String, RegistrationStemEvent> existingByKey = existing.stream().collect(Collectors.toMap(
                item -> item.getTerm() + ":" + item.getEvent().getId(), Function.identity()));
        Set<String> uniqueEvents = new HashSet<>();
        for (RegistrationStemEventsRequest.Assignment item : requested) {
            String key = item.getTerm() + ":" + item.getEventId();
            if (!uniqueEvents.add(key)) throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Không thể chọn trùng một Event trong cùng học phần.");
            RegistrationStemEvent existingAssignment = existingByKey.get(key);
            if (existingAssignment != null && existingAssignment.getEventDate().equals(item.getEventDate())) continue;
            LocalDate start = termStart(registration, item.getTerm());
            if (start == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Hãy thiết lập ngày bắt đầu học phần trước khi xếp Event.");
            if (item.getEventDate().isBefore(start)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ngày Event phải nằm từ ngày bắt đầu học phần trở đi.");
            StemEventTemplate selectedEvent = templateRepository.findById(item.getEventId()).orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy Event đã chọn."));
            Set<Integer> contentGrades = gradeLessonRepository.findAllByEvent_IdOrderByGradeAsc(selectedEvent.getId())
                    .stream().map(StemEventGradeLesson::getGrade).collect(Collectors.toSet());
            if (!contentGrades.containsAll(registration.getStemGrades())) throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Event cần có nội dung cho toàn bộ khối đã đăng ký.");
        }

        Set<String> kept = new HashSet<>();
        List<RegistrationStemEvent> saved = new ArrayList<>();
        for (RegistrationStemEventsRequest.Assignment item : requested) {
            String key = item.getTerm() + ":" + item.getEventId();
            RegistrationStemEvent assignment = existingByKey.get(key);
            if (assignment == null) assignment = RegistrationStemEvent.builder()
                    .registration(registration).event(templateRepository.getReferenceById(item.getEventId()))
                    .term(item.getTerm()).build();
            assignment.setEventDate(item.getEventDate());
            saved.add(assignmentRepository.save(assignment));
            kept.add(key);
        }
        existing.stream().filter(item -> !kept.contains(item.getTerm() + ":" + item.getEvent().getId()))
                .forEach(assignmentRepository::delete);
        saved.sort(Comparator.comparing(RegistrationStemEvent::getTerm).thenComparing(RegistrationStemEvent::getEventDate));
        return saved.stream().map(this::toAssignmentResponse).toList();
    }

    @Transactional
    public void initializeTemplates() {
        seedTemplate("stem-event-20-10", "20/10 - Ngày Phụ nữ Việt Nam", null, null);
        seedTemplate(EVENT_20_11_ID, "20/11 - Ngày Nhà giáo Việt Nam", EVENT_20_11_INTRO, EVENT_20_11_GREETING);
        seedTemplate("stem-event-mid-autumn", "Tết Trung Thu", null, null);
        seedTemplate("stem-event-lunar-new-year", "Tết Nguyên Đán", null, null);
        seedEventAnnouncementDefaultsOnce();

        List<StemEventLesson> legacy = legacyEventRepository.findAllByOrderByGradeAsc();
        if (!legacy.isEmpty() && !templateRepository.existsById("stem-event-legacy-final")) {
            StemEventTemplate oldTemplate = templateRepository.save(StemEventTemplate.builder()
                    .id("stem-event-legacy-final").name("Event tổng kết học phần (dữ liệu cũ)").build());
            List<StemEventGradeLesson> rows = legacy.stream().map(item -> StemEventGradeLesson.builder()
                    .id(item.getId()).event(oldTemplate).grade(item.getGrade())
                    .title(item.getTitle()).note(item.getNote()).driveImageUrl(item.getDriveImageUrl()).build()).toList();
            gradeLessonRepository.saveAll(rows);
        }
        migrateExistingFixedEvents();
    }

    private void migrateExistingFixedEvents() {
        final String migrationKey = "stem-event-legacy-assignments-v1";
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS curriculum_seed_history (seed_key varchar(80) NOT NULL PRIMARY KEY, seeded_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP)");
        Integer completed = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM curriculum_seed_history WHERE seed_key = ?", Integer.class, migrationKey);
        if (completed != null && completed > 0) return;

        StemEventTemplate legacyEvent = templateRepository.findById("stem-event-legacy-final").orElse(null);
        if (legacyEvent != null) {
            List<Holiday> holidays = holidayRepository.findAll();
            for (Registration registration : registrationRepository.findAll()) {
                if (registration.getStemProgram() == null || registration.getStemGrades() == null || registration.getStemGrades().isEmpty()) continue;
                for (int term = 1; term <= 4; term++) {
                    Integer configuredWeeks = registration.getStemTermWeeks() == null ? null : registration.getStemTermWeeks().get(term);
                    if (Integer.valueOf(8).equals(configuredWeeks)) continue;
                    LocalDate eventDate = legacyWeekNineDate(registration, term, holidays);
                    if (eventDate == null || assignmentRepository.findByRegistration_IdAndTermAndEvent_Id(
                            registration.getId(), term, legacyEvent.getId()).isPresent()) continue;
                    assignmentRepository.save(RegistrationStemEvent.builder().registration(registration).event(legacyEvent)
                            .term(term).eventDate(eventDate).build());
                }
            }
        }
        jdbcTemplate.update("INSERT INTO curriculum_seed_history (seed_key) VALUES (?)", migrationKey);
    }

    private void seedEventAnnouncementDefaultsOnce() {
        final String migrationKey = "stem-event-announcement-copy-v1";
        Integer completed = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM curriculum_seed_history WHERE seed_key = ?", Integer.class, migrationKey);
        if (completed != null && completed > 0) return;
        templateRepository.findById(EVENT_20_11_ID).ifPresent(template -> {
            if (template.getAnnouncementIntro() == null || template.getAnnouncementIntro().isBlank())
                template.setAnnouncementIntro(EVENT_20_11_INTRO);
            if (template.getAnnouncementGreeting() == null || template.getAnnouncementGreeting().isBlank())
                template.setAnnouncementGreeting(EVENT_20_11_GREETING);
            templateRepository.save(template);
        });
        jdbcTemplate.update("INSERT INTO curriculum_seed_history (seed_key) VALUES (?)", migrationKey);
    }

    private LocalDate legacyWeekNineDate(Registration registration, int term, List<Holiday> holidays) {
        LocalDate start = termStart(registration, term);
        if (start == null) return null;
        Set<LocalDate> closed = new HashSet<>(closureRepository
                .findAllBySchool_IdOrderByWeekStartAsc(registration.getSchool().getId()).stream()
                .map(SchoolClosure::getWeekStart).toList());
        if (registration.getStemStudyDay() != null) {
            for (Holiday holiday : holidays) {
                for (LocalDate day = holiday.getStartDate(); !day.isAfter(holiday.getEndDate()); day = day.plusDays(1)) {
                    if (day.getDayOfWeek() == registration.getStemStudyDay()) closed.add(weekStart(day));
                }
            }
        }
        LocalDate scheduled = weekStart(start);
        for (int week = 1; week <= 9; week++) {
            while (closed.contains(scheduled)) scheduled = scheduled.plusWeeks(1);
            if (week < 9) scheduled = scheduled.plusWeeks(1);
        }
        return scheduled;
    }

    private void seedTemplate(String id, String name, String defaultIntro, String defaultGreeting) {
        StemEventTemplate template = templateRepository.findById(id).orElse(null);
        boolean created = template == null;
        if (created) {
            template = templateRepository.save(StemEventTemplate.builder().id(id).name(name)
                    .announcementIntro(defaultIntro).announcementGreeting(defaultGreeting).build());
        }
        if (!created) return;
        List<StemEventGradeLesson> lessons = new ArrayList<>();
        for (int grade = 1; grade <= 12; grade++) lessons.add(StemEventGradeLesson.builder()
                .id(gradeLessonId(id, grade)).event(template).grade(grade).title(name).build());
        gradeLessonRepository.saveAll(lessons);
    }

    private void saveGradeLessons(StemEventTemplate template, List<StemEventGradeLessonRequest> requests) {
        Set<Integer> grades = new HashSet<>();
        for (StemEventGradeLessonRequest request : requests) {
            if (!grades.add(request.getGrade())) throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Khối " + request.getGrade() + " bị lặp trong nội dung Event.");
            StemEventGradeLesson lesson = gradeLessonRepository.findByEvent_IdAndGrade(template.getId(), request.getGrade())
                    .orElseGet(() -> StemEventGradeLesson.builder().id(gradeLessonId(template.getId(), request.getGrade()))
                            .event(template).grade(request.getGrade()).build());
            lesson.setTitle(request.getTitle().trim());
            lesson.setNote(normalizeOptional(request.getNote()));
            lesson.setDriveImageUrl(normalizeOptional(request.getDriveImageUrl()));
            gradeLessonRepository.save(lesson);
        }
    }

    private StemEventTemplateResponse getTemplate(StemEventTemplate template) {
        return new StemEventTemplateResponse(template.getId(), template.getName(),
                template.getAnnouncementIntro(), template.getAnnouncementGreeting(),
                gradeLessonRepository.findAllByEvent_IdOrderByGradeAsc(template.getId()).stream()
                        .map(this::toGradeLessonResponse).toList());
    }

    private StemEventGradeLessonResponse toGradeLessonResponse(StemEventGradeLesson lesson) {
        return new StemEventGradeLessonResponse(lesson.getId(), lesson.getEvent().getId(), lesson.getGrade(),
                lesson.getTitle(), lesson.getNote(), lesson.getDriveImageUrl());
    }

    private RegistrationStemEventResponse toAssignmentResponse(RegistrationStemEvent assignment) {
        return new RegistrationStemEventResponse(assignment.getId(), assignment.getRegistration().getId().toString(),
                assignment.getEvent().getId(), assignment.getEvent().getName(), assignment.getTerm(), assignment.getEventDate());
    }

    private LocalDate termStart(Registration registration, Integer term) {
        return switch (term) {
            case 1 -> registration.getTerm1Start();
            case 2 -> registration.getTerm2Start();
            case 3 -> registration.getTerm3Start();
            case 4 -> registration.getTerm4Start();
            default -> null;
        };
    }

    private LocalDate weekStart(LocalDate date) {
        int day = date.getDayOfWeek().getValue();
        return date.plusDays(day >= 6 ? 8L - day : 1L - day);
    }

    private String gradeLessonId(String eventId, Integer grade) {
        String compact = eventId.replaceAll("[^A-Za-z0-9_-]", "-");
        return (compact.length() > 95 ? compact.substring(0, 95) : compact) + "-grade-" + grade;
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private User currentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTS));
    }

    private boolean isAdmin(User user) {
        return user.getRoles() != null && (user.getRoles().contains(Role.ADMIN.name())
                || user.getRoles().contains(Role.ADMIN_SCHOOL.name()));
    }

    private void assertAccess(User user, Registration registration) {
        if (isAdmin(user)) return;
        if (user.getRegion() == null || !user.getRegion().getId().equals(registration.getSchool().getRegion().getId())) {
            throw new AccessDeniedException("You can only manage STEM events for schools in your region");
        }
    }
}
