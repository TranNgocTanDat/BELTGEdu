package com.example.beprojectweb.service;

import com.example.beprojectweb.dto.response.DigitalLessonResponse;
import com.example.beprojectweb.dto.request.LessonTitleRequest;
import com.example.beprojectweb.dto.request.DigitalLessonCreateRequest;
import com.example.beprojectweb.dto.request.DigitalLessonUpdateRequest;
import com.example.beprojectweb.entity.DigitalLesson;
import com.example.beprojectweb.enums.DigitalProgram;
import com.example.beprojectweb.repository.DigitalLessonRepository;
import com.example.beprojectweb.repository.LessonProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DigitalCurriculumService {
    private final DigitalLessonRepository lessonRepository;
    private final LessonProgressRepository progressRepository;

    @Transactional(readOnly = true)
    public List<DigitalLessonResponse> getLessons(DigitalProgram program, Integer grade) {
        if (grade != null && (grade < 1 || grade > 12)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "grade must be between 1 and 12");
        }
        List<DigitalLesson> lessons;
        if (program != null && grade != null) {
            lessons = lessonRepository.findAllByProgramAndGradeOrderByLessonOrderAsc(program, grade);
        } else if (program != null) {
            lessons = lessonRepository.findAllByProgramOrderByGradeAscLessonOrderAsc(program);
        } else {
            lessons = lessonRepository.findAllByOrderByProgramAscGradeAscLessonOrderAsc();
        }
        return lessons.stream().filter(lesson -> grade == null || lesson.getGrade().equals(grade))
                .map(this::toResponse).toList();
    }

    @PreAuthorize("hasAnyRole('ADMIN','ADMIN_SCHOOL')")
    @Transactional
    public DigitalLessonResponse renameLesson(String id, LessonTitleRequest request) {
        DigitalLesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Digital lesson not found"));
        lesson.setTitle(request.getTitle().trim());
        if (request.getNote() != null) lesson.setNote(normalizeOptional(request.getNote()));
        if (request.getDriveImageUrl() != null) lesson.setDriveImageUrl(normalizeOptional(request.getDriveImageUrl()));
        lesson = lessonRepository.save(lesson);
        return toResponse(lesson);
    }

    @PreAuthorize("hasAnyRole('ADMIN','ADMIN_SCHOOL')")
    @Transactional
    public DigitalLessonResponse createLesson(DigitalLessonCreateRequest request) {
        if (lessonRepository.existsByProgramAndGradeAndLessonOrder(request.getProgram(), request.getGrade(), request.getOrder())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Vị trí bài học đã được sử dụng trong chương trình và khối này.");
        }
        DigitalLesson lesson = DigitalLesson.builder().id(java.util.UUID.randomUUID().toString())
                .program(request.getProgram()).grade(request.getGrade()).lessonOrder(request.getOrder())
                .title(request.getTitle().trim()).note(normalizeOptional(request.getNote()))
                .driveImageUrl(normalizeOptional(request.getDriveImageUrl())).build();
        return toResponse(lessonRepository.save(lesson));
    }

    @PreAuthorize("hasAnyRole('ADMIN','ADMIN_SCHOOL')")
    @Transactional
    public DigitalLessonResponse updateLesson(String id, DigitalLessonUpdateRequest request) {
        DigitalLesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Digital lesson not found"));
        if (lessonRepository.existsByProgramAndGradeAndLessonOrderAndIdNot(
                lesson.getProgram(), lesson.getGrade(), request.getOrder(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Vị trí bài học đã được sử dụng trong chương trình và khối này.");
        }
        lesson.setLessonOrder(request.getOrder());
        lesson.setTitle(request.getTitle().trim());
        if (request.getNote() != null) lesson.setNote(normalizeOptional(request.getNote()));
        if (request.getDriveImageUrl() != null) lesson.setDriveImageUrl(normalizeOptional(request.getDriveImageUrl()));
        return toResponse(lessonRepository.save(lesson));
    }

    @PreAuthorize("hasAnyRole('ADMIN','ADMIN_SCHOOL')")
    @Transactional
    public void deleteLesson(String id) {
        DigitalLesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Digital lesson not found"));
        if (progressRepository.existsByPlanTypeAndLessonId("DIGITAL", id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bài học đã có tiến độ ở trường, không thể xóa.");
        }
        lessonRepository.delete(lesson);
    }

    private DigitalLessonResponse toResponse(DigitalLesson lesson) {
        return DigitalLessonResponse.builder()
                .id(lesson.getId())
                .program(lesson.getProgram())
                .grade(lesson.getGrade())
                .order(lesson.getLessonOrder())
                .title(lesson.getTitle())
                .note(lesson.getNote())
                .driveImageUrl(lesson.getDriveImageUrl())
                .build();
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
