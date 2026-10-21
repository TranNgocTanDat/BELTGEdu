package com.example.beprojectweb.service;

import com.example.beprojectweb.dto.response.StemCourseResponse;
import com.example.beprojectweb.dto.response.StemLessonResponse;
import com.example.beprojectweb.dto.request.LessonTitleRequest;
import com.example.beprojectweb.dto.request.StemCourseRequest;
import com.example.beprojectweb.dto.request.StemLessonCreateRequest;
import com.example.beprojectweb.entity.StemCourse;
import com.example.beprojectweb.entity.StemLesson;
import com.example.beprojectweb.enums.StemProgram;
import com.example.beprojectweb.enums.StemSubject;
import com.example.beprojectweb.repository.StemCourseRepository;
import com.example.beprojectweb.repository.StemLessonRepository;
import com.example.beprojectweb.repository.StemPlanRepository;
import com.example.beprojectweb.repository.LessonProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StemCurriculumService {
    private final StemCourseRepository courseRepository;
    private final StemLessonRepository lessonRepository;
    private final StemPlanRepository planRepository;
    private final LessonProgressRepository progressRepository;

    @Transactional(readOnly = true)
    public List<StemCourseResponse> getCourses(StemProgram program, StemSubject subject, Integer grade) {
        if (grade != null && (grade < 1 || grade > 12)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "grade must be between 1 and 12");
        }
        Map<String, List<StemLessonResponse>> lessonsByCourse = lessonRepository
                .findAllByOrderByCourse_IdAscLessonOrderAsc().stream()
                .collect(Collectors.groupingBy(lesson -> lesson.getCourse().getId(), HashMap::new,
                        Collectors.mapping(this::toLessonResponse, Collectors.toList())));

        List<StemCourse> courses = new ArrayList<>(courseRepository.findAll());
        courses.sort(Comparator.comparing((StemCourse c) -> c.getProgram().name())
                .thenComparing(c -> c.getSubject().name())
                .thenComparing(StemCourse::getGrade, Comparator.nullsFirst(Integer::compareTo))
                .thenComparing(StemCourse::getModule, Comparator.nullsFirst(Integer::compareTo))
                .thenComparing(StemCourse::getPart));

        return courses.stream()
                .filter(course -> program == null || course.getProgram() == program)
                .filter(course -> subject == null || course.getSubject() == subject)
                .filter(course -> grade == null || course.getGrade() == null || course.getGrade().equals(grade))
                .map(course -> toCourseResponse(course, lessonsByCourse.getOrDefault(course.getId(), List.of())))
                .toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public StemLessonResponse renameLesson(String id, LessonTitleRequest request) {
        StemLesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "STEM lesson not found"));
        lesson.setTitle(request.getTitle().trim());
        if (request.getNote() != null) lesson.setNote(normalizeOptional(request.getNote()));
        if (request.getDriveImageUrl() != null) lesson.setDriveImageUrl(normalizeOptional(request.getDriveImageUrl()));
        lesson = lessonRepository.save(lesson);
        return toLessonResponse(lesson);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public StemCourseResponse createCourse(StemCourseRequest request) {
        StemCourse course = courseFrom(request, new StemCourse());
        course.setId(UUID.randomUUID().toString());
        course = courseRepository.save(course);
        return toCourseResponse(course, List.of());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public StemCourseResponse updateCourse(String id, StemCourseRequest request) {
        StemCourse course = courseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "STEM course not found"));
        course = courseFrom(request, course);
        course = courseRepository.save(course);
        return toCourseResponse(course, lessonRepository.findAllByCourse_IdOrderByLessonOrderAsc(id).stream()
                .map(this::toLessonResponse).toList());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void deleteCourse(String id) {
        StemCourse course = courseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "STEM course not found"));
        if (planRepository.existsByCourse_Id(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Khóa học đang được kế hoạch học sử dụng, không thể xóa.");
        }
        List<StemLesson> lessons = lessonRepository.findAllByCourse_IdOrderByLessonOrderAsc(id);
        if (lessons.stream().anyMatch(lesson -> progressRepository.existsByPlanTypeAndLessonId("STEM", lesson.getId()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Khóa học có bài đã ghi nhận tiến độ, không thể xóa.");
        }
        lessonRepository.deleteAll(lessons);
        courseRepository.delete(course);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public StemLessonResponse createLesson(String courseId, StemLessonCreateRequest request) {
        StemCourse course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "STEM course not found"));
        if (lessonRepository.existsByCourse_IdAndLessonOrder(courseId, request.getOrder())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Vị trí bài học đã được sử dụng trong khóa này.");
        }
        StemLesson lesson = StemLesson.builder().id(UUID.randomUUID().toString()).course(course)
                .lessonOrder(request.getOrder()).title(request.getTitle().trim())
                .note(normalizeOptional(request.getNote())).driveImageUrl(normalizeOptional(request.getDriveImageUrl())).build();
        return toLessonResponse(lessonRepository.save(lesson));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void deleteLesson(String id) {
        StemLesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "STEM lesson not found"));
        if (progressRepository.existsByPlanTypeAndLessonId("STEM", id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bài học đã có tiến độ ở trường, không thể xóa.");
        }
        lessonRepository.delete(lesson);
    }

    private StemCourse courseFrom(StemCourseRequest request, StemCourse course) {
        course.setCode(request.getCode().trim());
        course.setProgram(request.getProgram());
        course.setSubject(request.getSubject());
        course.setGrade(request.getGrade());
        course.setModule(request.getModule());
        course.setPart(request.getPart());
        return course;
    }

    private StemCourseResponse toCourseResponse(StemCourse course, List<StemLessonResponse> lessons) {
        return StemCourseResponse.builder()
                .id(course.getId())
                .code(course.getCode())
                .program(course.getProgram())
                .subject(course.getSubject())
                .grade(course.getGrade())
                .module(course.getModule())
                .part(course.getPart())
                .lessons(lessons)
                .build();
    }

    private StemLessonResponse toLessonResponse(StemLesson lesson) {
        return StemLessonResponse.builder()
                .id(lesson.getId())
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
