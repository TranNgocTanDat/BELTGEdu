package com.example.beprojectweb.controller;

import com.example.beprojectweb.dto.response.APIResponse;
import com.example.beprojectweb.dto.request.LessonTitleRequest;
import com.example.beprojectweb.dto.request.StemCourseRequest;
import com.example.beprojectweb.dto.request.StemLessonCreateRequest;
import com.example.beprojectweb.dto.response.StemCourseResponse;
import com.example.beprojectweb.dto.response.StemLessonResponse;
import com.example.beprojectweb.enums.StemProgram;
import com.example.beprojectweb.enums.StemSubject;
import com.example.beprojectweb.service.StemCurriculumService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/curriculum/stem")
@RequiredArgsConstructor
public class StemCurriculumController {
    private final StemCurriculumService curriculumService;

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'ADMIN_SCHOOL')")
    public APIResponse<List<StemCourseResponse>> getCourses(
            @RequestParam(required = false) StemProgram program,
            @RequestParam(required = false) StemSubject subject,
            @RequestParam(required = false) Integer grade) {
        return APIResponse.<List<StemCourseResponse>>builder()
                .result(curriculumService.getCourses(program, subject, grade)).build();
    }

    @PostMapping("/courses")
    public APIResponse<StemCourseResponse> createCourse(@RequestBody @jakarta.validation.Valid StemCourseRequest request) {
        return APIResponse.<StemCourseResponse>builder().result(curriculumService.createCourse(request)).build();
    }

    @PutMapping("/courses/{id}")
    public APIResponse<StemCourseResponse> updateCourse(@PathVariable String id,
            @RequestBody @jakarta.validation.Valid StemCourseRequest request) {
        return APIResponse.<StemCourseResponse>builder().result(curriculumService.updateCourse(id, request)).build();
    }

    @DeleteMapping("/courses/{id}")
    public APIResponse<Void> deleteCourse(@PathVariable String id) {
        curriculumService.deleteCourse(id);
        return APIResponse.<Void>builder().build();
    }

    @PostMapping("/courses/{courseId}/lessons")
    public APIResponse<List<StemLessonResponse>> createLessons(@PathVariable String courseId,
            @RequestBody @jakarta.validation.Valid @jakarta.validation.constraints.NotEmpty
            @jakarta.validation.constraints.Size(max = 100) List<@jakarta.validation.Valid StemLessonCreateRequest> requests) {
        return APIResponse.<List<StemLessonResponse>>builder()
                .result(curriculumService.createLessons(courseId, requests)).build();
    }

    @DeleteMapping("/lessons/{id}")
    public APIResponse<Void> deleteLesson(@PathVariable String id) {
        curriculumService.deleteLesson(id);
        return APIResponse.<Void>builder().build();
    }

    @PatchMapping("/lessons/{id}")
    public APIResponse<com.example.beprojectweb.dto.response.StemLessonResponse> renameLesson(
            @PathVariable String id, @RequestBody @jakarta.validation.Valid LessonTitleRequest request) {
        return APIResponse.<com.example.beprojectweb.dto.response.StemLessonResponse>builder()
                .result(curriculumService.renameLesson(id, request)).build();
    }
}
