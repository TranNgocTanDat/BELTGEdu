package com.example.beprojectweb.dto.response;
import java.time.LocalDate; import java.util.List;
public record ScheduleWeekResponse(LocalDate weekStart,LocalDate weekEnd,String label,boolean announced,boolean reportable,boolean urgent,LocalDate deadline,List<Item> items) {
 public record Item(Integer grade,String code,String title,Integer lessonNo,Integer term,LocalDate originalWeekStart){}
}
