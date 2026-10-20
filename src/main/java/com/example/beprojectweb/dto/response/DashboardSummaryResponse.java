package com.example.beprojectweb.dto.response;
import java.time.LocalDate; import java.util.List; import java.util.UUID;
public record DashboardSummaryResponse(int totalSchools,int stemOnly,int digitalOnly,int both,Progress stem,Progress digital,List<Task> nextWeekTasks,List<Task> urgentTasks) {
 public record Progress(int done,int doing,int total){}
 public record Task(UUID registrationId,UUID schoolId,String schoolName,String schoolCode,String academicYear,String service,String program,LocalDate weekStart,LocalDate weekEnd,LocalDate deadline,String label,List<Integer> grades,boolean urgent){}
}
