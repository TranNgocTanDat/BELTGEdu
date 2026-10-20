package com.example.beprojectweb.dto.request;
import lombok.Data; import java.util.UUID;
@Data public class StemPlanRequest { private UUID registrationId; private Integer grade,term; private String courseId; }
