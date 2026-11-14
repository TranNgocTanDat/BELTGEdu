package com.example.beprojectweb.service;

import com.example.beprojectweb.dto.response.StemCourseResponse;
import com.example.beprojectweb.dto.response.StemLessonResponse;
import com.example.beprojectweb.dto.request.LessonTitleRequest;
import com.example.beprojectweb.dto.request.StemCourseRequest;
import com.example.beprojectweb.dto.request.StemCourseDuplicateRequest;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StemCurriculumService {
    private final StemCourseRepository courseRepository;
    private final StemLessonRepository lessonRepository;
    private final StemPlanRepository planRepository;
    private final com.example.beprojectweb.repository.StemPlanVersionRepository planVersionRepository;
    private final LessonProgressRepository progressRepository;

    @Transactional(readOnly = true)
    public List<StemCourseResponse> getCourses(StemProgram program, StemSubject subject, Integer grade) {
        if (grade != null && (grade < 1 || grade > 12)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "grade must be between 1 and 12");
        }
        List<StemCourse> courses = courseRepository.findCatalog(program, subject, grade);
        if (courses.isEmpty()) return List.of();
        List<String> courseIds = courses.stream().map(StemCourse::getId).toList();
        Map<String, List<StemLessonResponse>> lessonsByCourse = lessonRepository
                .findAllByCourse_IdInOrderByCourse_IdAscLessonOrderAsc(courseIds).stream()
                .filter(lesson -> !isStemEventLesson(lesson))
                .collect(Collectors.groupingBy(lesson -> lesson.getCourse().getId(), HashMap::new,
                        Collectors.mapping(this::toLessonResponse, Collectors.toList())));

        return courses.stream()
                .map(course -> toCourseResponse(course, lessonsByCourse.getOrDefault(course.getId(), List.of())))
                .toList();
    }

    private boolean isStemEventLesson(StemLesson lesson) {
        return isStemEventTitle(lesson.getTitle());
    }

    private boolean isStemEventTitle(String title) {
        return title != null && title.trim().equalsIgnoreCase("Event tổng kết học phần");
    }

    @PreAuthorize("hasAnyRole('ADMIN','ADMIN_SCHOOL')")
    @Transactional
    public StemLessonResponse renameLesson(String id, LessonTitleRequest request) {
        StemLesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "STEM lesson not found"));
        if (isStemEventTitle(request.getTitle())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Event tổng kết được quản lý dùng chung theo khối, không thêm Event vào từng khóa.");
        }
        lesson.setTitle(request.getTitle().trim());
        if (request.getNote() != null) lesson.setNote(normalizeOptional(request.getNote()));
        if (request.getDriveImageUrlAig() != null) lesson.setDriveImageUrlAig(normalizeOptional(request.getDriveImageUrlAig()));
        else if (request.getDriveImageUrl() != null) lesson.setDriveImageUrlAig(normalizeOptional(request.getDriveImageUrl()));
        if (request.getDriveImageUrlEtech() != null) lesson.setDriveImageUrlEtech(normalizeOptional(request.getDriveImageUrlEtech()));
        if (request.getDriveImageUrl() != null) lesson.setDriveImageUrl(normalizeOptional(request.getDriveImageUrl()));
        else if (request.getDriveImageUrlAig() != null) lesson.setDriveImageUrl(normalizeOptional(request.getDriveImageUrlAig()));
        lesson = lessonRepository.save(lesson);
        return toLessonResponse(lesson);
    }

    @PreAuthorize("hasAnyRole('ADMIN','ADMIN_SCHOOL')")
    @Transactional
    public StemCourseResponse createCourse(StemCourseRequest request) {
        String code = request.getCode().trim();
        if (courseRepository.existsByProgramAndCodeIgnoreCase(request.getProgram(), code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã khóa học đã tồn tại trong chương trình này");
        }
        StemCourse course = courseFrom(request, new StemCourse());
        course.setId(UUID.randomUUID().toString());
        course = courseRepository.save(course);
        return toCourseResponse(course, List.of());
    }

    @PreAuthorize("hasAnyRole('ADMIN','ADMIN_SCHOOL')")
    @Transactional
    public StemCourseResponse duplicateCourse(String sourceId, StemCourseDuplicateRequest request) {
        StemCourse source = courseRepository.findById(sourceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "STEM course not found"));
        String code = request.getCode().trim();
        if (courseRepository.existsByProgramAndCodeIgnoreCase(source.getProgram(), code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã khóa học đã tồn tại trong chương trình này");
        }

        Set<Integer> orders = new HashSet<>();
        for (int index = 0; index < request.getLessons().size(); index++) {
            StemLessonCreateRequest lesson = request.getLessons().get(index);
            if (isStemEventTitle(lesson.getTitle())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Event được quản lý dùng chung, không sao chép vào khóa STEM thường.");
            }
            if (!orders.add(lesson.getOrder()) || lesson.getOrder() != index + 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Thứ tự bài trong bản sao phải liên tiếp từ 1.");
            }
        }

        StemCourse copy = StemCourse.builder()
                .id(UUID.randomUUID().toString())
                .code(code)
                .program(source.getProgram())
                .subject(source.getSubject())
                .grade(source.getGrade())
                .module(source.getModule())
                .part(source.getPart())
                .build();
        copy = courseRepository.save(copy);
        StemCourse savedCourse = copy;

        List<StemLesson> copiedLessons = request.getLessons().stream().map(lesson -> {
            String aigImageUrl = lesson.getDriveImageUrlAig() != null
                    ? normalizeOptional(lesson.getDriveImageUrlAig()) : normalizeOptional(lesson.getDriveImageUrl());
            String legacyImageUrl = lesson.getDriveImageUrl() != null
                    ? normalizeOptional(lesson.getDriveImageUrl()) : aigImageUrl;
            return StemLesson.builder()
                    .id(UUID.randomUUID().toString())
                    .course(savedCourse)
                    .lessonOrder(lesson.getOrder())
                    .title(lesson.getTitle().trim())
                    .note(normalizeOptional(lesson.getNote()))
                    .driveImageUrl(legacyImageUrl)
                    .driveImageUrlAig(aigImageUrl)
                    .driveImageUrlEtech(normalizeOptional(lesson.getDriveImageUrlEtech()))
                    .build();
        }).toList();
        List<StemLessonResponse> savedLessons = lessonRepository.saveAll(copiedLessons).stream()
                .map(this::toLessonResponse).toList();
        return toCourseResponse(savedCourse, savedLessons);
    }

    @PreAuthorize("hasAnyRole('ADMIN','ADMIN_SCHOOL')")
    @Transactional
    public StemCourseResponse updateCourse(String id, StemCourseRequest request) {
        StemCourse course = courseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "STEM course not found"));
        String code = request.getCode().trim();
        if (courseRepository.existsByProgramAndCodeIgnoreCaseAndIdNot(request.getProgram(), code, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã khóa học đã tồn tại trong chương trình này");
        }
        course = courseFrom(request, course);
        course = courseRepository.save(course);
        return toCourseResponse(course, lessonRepository.findAllByCourse_IdOrderByLessonOrderAsc(id).stream()
                .filter(lesson -> !isStemEventLesson(lesson))
                .map(this::toLessonResponse).toList());
    }

    @PreAuthorize("hasAnyRole('ADMIN','ADMIN_SCHOOL')")
    @Transactional
    public void deleteCourse(String id) {
        StemCourse course = courseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "STEM course not found"));
        if (planRepository.existsByCourse_Id(id) || planVersionRepository.existsByCourse_Id(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Khóa học đang được kế hoạch học sử dụng, không thể xóa.");
        }
        List<StemLesson> lessons = lessonRepository.findAllByCourse_IdOrderByLessonOrderAsc(id);
        if (lessons.stream().anyMatch(lesson -> progressRepository.existsByPlanTypeAndLessonId("STEM", lesson.getId()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Khóa học có bài đã ghi nhận tiến độ, không thể xóa.");
        }
        lessonRepository.deleteAll(lessons);
        courseRepository.delete(course);
    }

    @PreAuthorize("hasAnyRole('ADMIN','ADMIN_SCHOOL')")
    @Transactional
    public List<StemLessonResponse> createLessons(String courseId, List<StemLessonCreateRequest> requests) {
        StemCourse course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "STEM course not found"));
        Set<Integer> requestedOrders = new HashSet<>();
        for (StemLessonCreateRequest request : requests) {
            if (isStemEventTitle(request.getTitle())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Event tổng kết được quản lý dùng chung theo khối, không thêm Event vào từng khóa.");
            }
            if (!requestedOrders.add(request.getOrder())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Vị trí bài học " + request.getOrder() + " bị trùng trong danh sách gửi lên.");
            }
            if (lessonRepository.existsByCourse_IdAndLessonOrder(courseId, request.getOrder())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Vị trí bài học " + request.getOrder() + " đã được sử dụng trong khóa này.");
            }
        }
        List<StemLesson> lessons = requests.stream().map(request -> {
            String aigImageUrl = request.getDriveImageUrlAig() != null
                    ? normalizeOptional(request.getDriveImageUrlAig()) : normalizeOptional(request.getDriveImageUrl());
            String legacyImageUrl = request.getDriveImageUrl() != null
                    ? normalizeOptional(request.getDriveImageUrl()) : aigImageUrl;
            return StemLesson.builder()
                    .id(UUID.randomUUID().toString()).course(course)
                    .lessonOrder(request.getOrder()).title(request.getTitle().trim())
                    .note(normalizeOptional(request.getNote())).driveImageUrl(legacyImageUrl)
                    .driveImageUrlAig(aigImageUrl)
                    .driveImageUrlEtech(normalizeOptional(request.getDriveImageUrlEtech())).build();
        })
                .toList();
        return lessonRepository.saveAll(lessons).stream().map(this::toLessonResponse).toList();
    }

    @PreAuthorize("hasAnyRole('ADMIN','ADMIN_SCHOOL')")
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
                .driveImageUrlAig(lesson.getDriveImageUrlAig())
                .driveImageUrlEtech(lesson.getDriveImageUrlEtech())
                .build();
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
