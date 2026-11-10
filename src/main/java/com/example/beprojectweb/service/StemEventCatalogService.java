package com.example.beprojectweb.service;

import com.example.beprojectweb.dto.request.StemEventLessonRequest;
import com.example.beprojectweb.dto.response.StemEventLessonResponse;
import com.example.beprojectweb.entity.LessonProgress;
import com.example.beprojectweb.entity.StemEventLesson;
import com.example.beprojectweb.entity.StemLesson;
import com.example.beprojectweb.repository.LessonProgressRepository;
import com.example.beprojectweb.repository.StemEventLessonRepository;
import com.example.beprojectweb.repository.StemLessonRepository;
import com.example.beprojectweb.repository.StemPlanRepository;
import com.example.beprojectweb.repository.StemPlanVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StemEventCatalogService {
    private static final String EVENT_TITLE = "Event tổng kết học phần";

    private final StemEventLessonRepository eventRepository;
    private final StemLessonRepository lessonRepository;
    private final StemPlanRepository planRepository;
    private final StemPlanVersionRepository planVersionRepository;
    private final LessonProgressRepository progressRepository;

    @Transactional(readOnly = true)
    public List<StemEventLessonResponse> getEvents() {
        return eventRepository.findAllByOrderByGradeAsc().stream().map(this::toResponse).toList();
    }

    @PreAuthorize("hasAnyRole('ADMIN','ADMIN_SCHOOL')")
    @Transactional
    public StemEventLessonResponse saveEvent(Integer grade, StemEventLessonRequest request) {
        validateGrade(grade);
        StemEventLesson event = eventRepository.findByGrade(grade).orElseGet(() -> StemEventLesson.builder()
                .id(eventId(grade)).grade(grade).build());
        event.setTitle(request.getTitle().trim());
        event.setNote(normalizeOptional(request.getNote()));
        event.setDriveImageUrl(normalizeOptional(request.getDriveImageUrl()));
        return toResponse(eventRepository.save(event));
    }

    /**
     * Creates one shared Event record for each grade. Existing course-bound Event rows are
     * retained for history, while their notes/images and progress are moved to the shared rows.
     */
    @Transactional
    public void initializeSharedEvents() {
        List<StemLesson> legacyEvents = lessonRepository.findAllByTitleIgnoreCase(EVENT_TITLE);
        if (legacyEvents.isEmpty()) return;
        Comparator<StemLesson> preference = Comparator
                .comparing((StemLesson lesson) -> isBlank(lesson.getNote()) && isBlank(lesson.getDriveImageUrl()))
                .thenComparing(lesson -> lesson.getCourse().getProgram().name())
                .thenComparing(lesson -> lesson.getCourse().getCode());

        Map<Integer, StemEventLesson> eventsByGrade = new HashMap<>();
        boolean createdSharedEvents = false;
        for (int grade = 1; grade <= 12; grade++) {
            final int targetGrade = grade;
            StemEventLesson event = eventRepository.findByGrade(grade).orElse(null);
            if (event == null) {
                StemLesson source = legacyEvents.stream()
                        .filter(lesson -> Integer.valueOf(targetGrade).equals(lesson.getCourse().getGrade()))
                        .min(preference)
                        .orElseGet(() -> legacyEvents.stream()
                                .filter(lesson -> lesson.getCourse().getGrade() == null)
                                .min(preference).orElse(null));
                event = eventRepository.save(StemEventLesson.builder()
                        .id(eventId(targetGrade))
                        .grade(targetGrade)
                        .title(EVENT_TITLE)
                        .note(source == null ? null : normalizeOptional(source.getNote()))
                        .driveImageUrl(source == null ? null : normalizeOptional(source.getDriveImageUrl()))
                        .build());
                createdSharedEvents = true;
            }
            eventsByGrade.put(grade, event);
        }
        if (createdSharedEvents) migrateLegacyProgress(legacyEvents, eventsByGrade);
    }

    private void migrateLegacyProgress(List<StemLesson> legacyEvents, Map<Integer, StemEventLesson> eventsByGrade) {
        Map<UUID, Integer> planGrades = new HashMap<>();
        planRepository.findAll().forEach(plan -> planGrades.put(plan.getId(), plan.getGrade()));
        planVersionRepository.findAll().forEach(plan -> planGrades.put(plan.getId(), plan.getGrade()));

        for (StemLesson legacy : legacyEvents) {
            for (LessonProgress saved : progressRepository.findAllByPlanTypeAndLessonId("STEM", legacy.getId())) {
                Integer grade = planGrades.get(saved.getPlanItemId());
                StemEventLesson shared = grade == null ? null : eventsByGrade.get(grade);
                if (shared == null || shared.getId().equals(saved.getLessonId())) continue;

                LessonProgress existing = progressRepository.findByPlanTypeAndPlanItemIdAndLessonId(
                        "STEM", saved.getPlanItemId(), shared.getId()).orElse(null);
                if (existing == null) {
                    saved.setLessonId(shared.getId());
                    progressRepository.save(saved);
                } else {
                    if ("DONE".equalsIgnoreCase(saved.getStatus()) || "DONE".equalsIgnoreCase(existing.getStatus())) {
                        existing.setStatus("DONE");
                    } else if ("DOING".equalsIgnoreCase(saved.getStatus())) {
                        existing.setStatus("DOING");
                    }
                    if (saved.getDate() != null && (existing.getDate() == null || saved.getDate().isAfter(existing.getDate()))) {
                        existing.setDate(saved.getDate());
                    }
                    progressRepository.save(existing);
                    progressRepository.delete(saved);
                }
            }
        }
    }

    private void validateGrade(Integer grade) {
        if (grade == null || grade < 1 || grade > 12) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "grade must be between 1 and 12");
        }
    }

    private String eventId(int grade) {
        return "stem-event-grade-" + grade;
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private StemEventLessonResponse toResponse(StemEventLesson event) {
        return new StemEventLessonResponse(event.getId(), event.getGrade(), event.getTitle(), event.getNote(), event.getDriveImageUrl());
    }
}
