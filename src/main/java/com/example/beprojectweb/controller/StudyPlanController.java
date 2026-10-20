package com.example.beprojectweb.controller;
import com.example.beprojectweb.dto.request.*; import com.example.beprojectweb.dto.response.*; import com.example.beprojectweb.service.StudyPlanService; import lombok.RequiredArgsConstructor; import org.springframework.web.bind.annotation.*; import java.util.*;
@RestController @RequiredArgsConstructor public class StudyPlanController { private final StudyPlanService service;
 @GetMapping("/registrations") public APIResponse<List<RegistrationResponse>> registrations(@RequestParam(required=false) UUID schoolId){return APIResponse.<List<RegistrationResponse>>builder().result(service.list(schoolId)).build();}
 @GetMapping("/registrations/{id}") public APIResponse<RegistrationResponse> registration(@PathVariable UUID id){return APIResponse.<RegistrationResponse>builder().result(service.get(id)).build();}
 @PostMapping("/registrations") public APIResponse<RegistrationResponse> create(@RequestBody RegistrationRequest request){return APIResponse.<RegistrationResponse>builder().result(service.create(request)).build();}
 @PutMapping("/registrations/{id}") public APIResponse<RegistrationResponse> update(@PathVariable UUID id,@RequestBody RegistrationRequest request){return APIResponse.<RegistrationResponse>builder().result(service.update(id,request)).build();}
 @DeleteMapping("/registrations/{id}") public APIResponse<Void> delete(@PathVariable UUID id){service.delete(id);return APIResponse.<Void>builder().build();}
 @GetMapping("/stem-plans") public APIResponse<List<StemPlanResponse>> stemPlans(@RequestParam(required=false) UUID registrationId){return APIResponse.<List<StemPlanResponse>>builder().result(service.getStemPlans(registrationId)).build();}
 @PutMapping("/stem-plans") public APIResponse<StemPlanResponse> setStemPlan(@RequestBody StemPlanRequest request){return APIResponse.<StemPlanResponse>builder().result(service.setStemPlan(request)).build();}
 @GetMapping("/digital-plans") public APIResponse<List<DigitalPlanResponse>> digitalPlans(@RequestParam(required=false) UUID registrationId){return APIResponse.<List<DigitalPlanResponse>>builder().result(service.getDigitalPlans(registrationId)).build();}
 @GetMapping("/lesson-progress") public APIResponse<List<LessonProgressResponse>> progress(@RequestParam(required=false) UUID registrationId){return APIResponse.<List<LessonProgressResponse>>builder().result(service.getProgress(registrationId)).build();}
 @PutMapping("/lesson-progress") public APIResponse<LessonProgressResponse> setProgress(@RequestBody LessonProgressRequest request){return APIResponse.<LessonProgressResponse>builder().result(service.setProgress(request)).build();}
}
