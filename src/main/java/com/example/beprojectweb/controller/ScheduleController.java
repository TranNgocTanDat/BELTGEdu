package com.example.beprojectweb.controller;
import com.example.beprojectweb.dto.response.APIResponse; import com.example.beprojectweb.dto.response.ScheduleWeekResponse; import com.example.beprojectweb.service.ScheduleService; import lombok.RequiredArgsConstructor; import org.springframework.web.bind.annotation.*; import java.util.List; import java.util.UUID;
@RestController @RequestMapping("/schedule") @RequiredArgsConstructor public class ScheduleController { private final ScheduleService service;
 @GetMapping("/weeks") public APIResponse<List<ScheduleWeekResponse>> weeks(@RequestParam UUID registrationId,@RequestParam String serviceType){return APIResponse.<List<ScheduleWeekResponse>>builder().result(service.weeks(registrationId,serviceType)).build();}
}
