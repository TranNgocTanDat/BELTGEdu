package com.example.beprojectweb.dto.request;
import com.example.beprojectweb.enums.*; import lombok.Data; import java.time.LocalDate; import java.time.DayOfWeek; import java.util.Set; import java.util.Map; import java.util.UUID;
@Data public class RegistrationRequest { private UUID schoolId; private String academicYear; private StemConfig stem; private DigitalConfig digital;
 @Data public static class StemConfig { private StemProgram program; private Set<Integer> grades; private LocalDate term1Start,term2Start,term3Start,term4Start; private Map<Integer,Integer> termWeeks; private DayOfWeek studyDay; }
 @Data public static class DigitalConfig { private DigitalProgram program; private Set<Integer> grades; private LocalDate startDate; private DayOfWeek studyDay; }
}
