package com.example.beprojectweb.service;

import com.example.beprojectweb.dto.response.*;
import com.example.beprojectweb.entity.*;
import com.example.beprojectweb.enums.Role;
import com.example.beprojectweb.exception.AppException;
import com.example.beprojectweb.exception.ErrorCode;
import com.example.beprojectweb.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor
public class DashboardService {
 private final SchoolRepository schools; private final RegistrationRepository registrations; private final StemPlanRepository stemPlans; private final DigitalPlanRepository digitalPlans;
 private final StemLessonRepository stemLessons; private final DigitalLessonRepository digitalLessons; private final LessonProgressRepository progress; private final UserRepository users; private final ScheduleService schedule;
 private static class Counts {int done,doing,total; void add(String status){total++;if("DONE".equals(status))done++;else if("DOING".equals(status))doing++;} DashboardSummaryResponse.Progress dto(){return new DashboardSummaryResponse.Progress(done,doing,total);}}
 @PreAuthorize("hasAnyRole('ADMIN','ADMIN_SCHOOL','USER')") @Transactional(readOnly=true)
 public DashboardSummaryResponse summary(){ User user=current(); List<School> visibleSchools=isAdmin(user)?schools.findAll():(user.getRegion()==null?List.of():schools.findAllByRegion_Id(user.getRegion().getId())); Set<UUID> schoolIds=new HashSet<>();visibleSchools.forEach(s->schoolIds.add(s.getId())); List<Registration> regs=isAdmin(user)?registrations.findAll():user.getRegion()==null?List.of():registrations.findAllBySchool_Region_Id(user.getRegion().getId());
  int stemOnly=0,digitalOnly=0,both=0;Counts stem=new Counts(),digital=new Counts();Map<String,String> status=new HashMap<>();for(LessonProgress p:progress.findAll())status.put(p.getPlanType()+":"+p.getPlanItemId()+":"+p.getLessonId(),p.getStatus());
  List<StemPlan> sPlans=stemPlans.findAll().stream().filter(p->schoolIds.contains(p.getRegistration().getSchool().getId())).toList();for(StemPlan p:sPlans)for(StemLesson l:stemLessons.findAllByCourse_IdOrderByLessonOrderAsc(p.getCourse().getId()))stem.add(status.get("STEM:"+p.getId()+":"+l.getId()));
  List<DigitalPlan> dPlans=digitalPlans.findAll().stream().filter(p->schoolIds.contains(p.getRegistration().getSchool().getId())).toList();for(DigitalPlan p:dPlans){Registration r=p.getRegistration();if(r.getDigitalProgram()!=null)for(DigitalLesson l:digitalLessons.findAllByProgramAndGradeOrderByLessonOrderAsc(r.getDigitalProgram(),p.getGrade()))digital.add(status.get("DIGITAL:"+p.getId()+":"+l.getId()));}
  for(School school:visibleSchools){boolean hasStem=regs.stream().anyMatch(r->r.getSchool().getId().equals(school.getId())&&r.getStemProgram()!=null);boolean hasDigital=regs.stream().anyMatch(r->r.getSchool().getId().equals(school.getId())&&r.getDigitalProgram()!=null);if(hasStem&&hasDigital)both++;else if(hasStem)stemOnly++;else if(hasDigital)digitalOnly++;}
  LocalDate today=LocalDate.now(),nextMonday=today.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusWeeks(1);List<DashboardSummaryResponse.Task> upcoming=new ArrayList<>(),urgent=new ArrayList<>();
  for(Registration r:regs){School s=r.getSchool();for(String service:List.of("stem","digital")){if((service.equals("stem")?r.getStemProgram():r.getDigitalProgram())==null)continue;for(ScheduleWeekResponse w:schedule.weeks(r.getId(),service)){if(w.announced())continue;String program=service.equals("stem")?r.getStemProgram().name():r.getDigitalProgram().name();List<Integer> grades=w.items().stream().map(ScheduleWeekResponse.Item::grade).distinct().sorted().toList();DashboardSummaryResponse.Task task=new DashboardSummaryResponse.Task(r.getId(),s.getId(),s.getName(),s.getCode(),r.getAcademicYear(),service,program,w.weekStart(),w.weekEnd(),w.deadline(),w.label(),grades,w.urgent());if(w.weekStart().equals(nextMonday)&&!today.isAfter(w.deadline()))upcoming.add(task);if(w.urgent())urgent.add(task);}}
  }
  Comparator<DashboardSummaryResponse.Task> byDate=Comparator.comparing(DashboardSummaryResponse.Task::weekStart).thenComparing(DashboardSummaryResponse.Task::schoolName);upcoming.sort(byDate);urgent.sort(byDate);return new DashboardSummaryResponse(visibleSchools.size(),stemOnly,digitalOnly,both,stem.dto(),digital.dto(),upcoming,urgent);
 }
 private User current(){String username=SecurityContextHolder.getContext().getAuthentication().getName();return users.findByUsername(username).orElseThrow(()->new AppException(ErrorCode.USER_NOT_EXISTS));}private boolean isAdmin(User u){return u.getRoles()!=null&&(u.getRoles().contains(Role.ADMIN.name())||u.getRoles().contains(Role.ADMIN_SCHOOL.name()));}
}
