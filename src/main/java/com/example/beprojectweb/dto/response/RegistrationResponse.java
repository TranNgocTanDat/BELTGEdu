package com.example.beprojectweb.dto.response;
import com.example.beprojectweb.enums.*; import java.time.LocalDate; import java.time.DayOfWeek; import java.util.*;
public record RegistrationResponse(UUID id,UUID schoolId,String academicYear,Stem stem,Digital digital) {
 public record Stem(StemProgram program,List<Integer> grades,Map<Integer,LocalDate> termStarts,Map<Integer,Integer> termWeeks,DayOfWeek studyDay){}
 public record Digital(DigitalProgram program,List<Integer> grades,LocalDate startDate,DayOfWeek studyDay){}
}
