package com.example.beprojectweb.controller;

import com.example.beprojectweb.dto.response.APIResponse;
import com.example.beprojectweb.dto.request.LessonTitleRequest;
import com.example.beprojectweb.dto.request.DigitalLessonCreateRequest;
import com.example.beprojectweb.dto.request.DigitalLessonBulkReplaceRequest;
import com.example.beprojectweb.dto.request.DigitalLessonUpdateRequest;
import com.example.beprojectweb.dto.response.DigitalLessonResponse;
import com.example.beprojectweb.enums.DigitalProgram;
import com.example.beprojectweb.service.DigitalCurriculumService;
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
@RequestMapping("/curriculum/digital")
@RequiredArgsConstructor
public class DigitalCurriculumController {
    private final DigitalCurriculumService curriculumService;

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'ADMIN_SCHOOL')")
    public APIResponse<List<DigitalLessonResponse>> getLessons(
            @RequestParam(required = false) DigitalProgram program,
            @RequestParam(required = false) Integer grade) {
        return APIResponse.<List<DigitalLessonResponse>>builder()
                .result(curriculumService.getLessons(program, grade)).build();
    }

    @PostMapping("/lessons")
    public APIResponse<DigitalLessonResponse> createLesson(@RequestBody @jakarta.validation.Valid DigitalLessonCreateRequest request) {
        return APIResponse.<DigitalLessonResponse>builder().result(curriculumService.createLesson(request)).build();
    }

    @PutMapping("/lessons/bulk-replace")
    public APIResponse<List<DigitalLessonResponse>> replaceLessons(
            @RequestBody @jakarta.validation.Valid DigitalLessonBulkReplaceRequest request) {
        return APIResponse.<List<DigitalLessonResponse>>builder()
                .result(curriculumService.replaceLessons(request)).build();
    }

    @PutMapping("/lessons/{id}")
    public APIResponse<DigitalLessonResponse> updateLesson(@PathVariable String id,
            @RequestBody @jakarta.validation.Valid DigitalLessonUpdateRequest request) {
        return APIResponse.<DigitalLessonResponse>builder().result(curriculumService.updateLesson(id, request)).build();
    }

    @DeleteMapping("/lessons/{id}")
    public APIResponse<Void> deleteLesson(@PathVariable String id) {
        curriculumService.deleteLesson(id);
        return APIResponse.<Void>builder().build();
    }

    @PatchMapping("/lessons/{id}")
    public APIResponse<DigitalLessonResponse> renameLesson(@PathVariable String id,
            @RequestBody @jakarta.validation.Valid LessonTitleRequest request) {
        return APIResponse.<DigitalLessonResponse>builder()
                .result(curriculumService.renameLesson(id, request)).build();
    }
}
