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
 private final SchoolRepository schools; private final RegistrationRepository registrations; private final StemPlanRepository stemPlans; private final StemPlanVersionRepository stemPlanVersions; private final StemPlanLessonOrderRepository stemLessonOrders; private final DigitalPlanRepository digitalPlans;
 private final StemLessonRepository stemLessons; private final StemEventLessonRepository stemEventLessons; private final DigitalLessonRepository digitalLessons; private final LessonProgressRepository progress; private final UserRepository users; private final ScheduleService schedule;
 private record StemAssignment(UUID id,int effectiveWeek,String courseId){}
 private static class Counts {int done,doing,total; void add(String status){total++;if("DONE".equals(status))done++;else if("DOING".equals(status))doing++;} DashboardSummaryResponse.Progress dto(){return new DashboardSummaryResponse.Progress(done,doing,total);}}
 @PreAuthorize("hasAnyRole('ADMIN','ADMIN_SCHOOL','USER')") @Transactional(readOnly=true)
 public DashboardSummaryResponse summary(){ User user=current(); List<School> visibleSchools=isAdmin(user)?schools.findAll():(user.getRegion()==null?List.of():schools.findAllByRegion_Id(user.getRegion().getId())); Set<UUID> schoolIds=new HashSet<>();visibleSchools.forEach(s->schoolIds.add(s.getId())); List<Registration> regs=isAdmin(user)?registrations.findAll():user.getRegion()==null?List.of():registrations.findAllBySchool_Region_Id(user.getRegion().getId());
  int stemOnly=0,digitalOnly=0,both=0;Counts stem=new Counts(),digital=new Counts();Map<String,String> status=new HashMap<>();for(LessonProgress p:progress.findAll())status.put(p.getPlanType()+":"+p.getPlanItemId()+":"+p.getLessonId(),p.getStatus());
  List<UUID> registrationIds=regs.stream().map(Registration::getId).toList();
  List<StemPlan> sPlans=registrationIds.isEmpty()?List.of():stemPlans.findAllByRegistration_IdInOrderByGradeAscTermAsc(registrationIds);
  List<StemPlanVersion> sVersions=registrationIds.isEmpty()?List.of():stemPlanVersions.findAllByRegistration_IdInAndActiveTrueOrderByGradeAscTermAscEffectiveFromWeekAsc(registrationIds);
  List<UUID> stemPlanIds=new ArrayList<>();sPlans.forEach(p->stemPlanIds.add(p.getId()));sVersions.forEach(p->stemPlanIds.add(p.getId()));
  Map<UUID,List<String>> orderByPlan=new HashMap<>();if(!stemPlanIds.isEmpty())stemLessonOrders.findAllByPlanItemIdInOrderByPlanItemIdAscPositionAsc(stemPlanIds).forEach(row->orderByPlan.computeIfAbsent(row.getPlanItemId(),key->new ArrayList<>()).add(row.getLessonId()));
  List<String> courseIds=new ArrayList<>();sPlans.forEach(p->courseIds.add(p.getCourse().getId()));sVersions.forEach(p->courseIds.add(p.getCourse().getId()));
  Map<String,List<StemLesson>> lessonsByCourse=courseIds.isEmpty()?Map.of():stemLessons.findAllByCourse_IdInOrderByCourse_IdAscLessonOrderAsc(courseIds.stream().distinct().toList()).stream().collect(java.util.stream.Collectors.groupingBy(l->l.getCourse().getId()));
  Map<Integer,StemEventLesson> eventsByGrade=stemEventLessons.findAllByOrderByGradeAsc().stream().collect(java.util.stream.Collectors.toMap(StemEventLesson::getGrade,event->event));
  Map<String,List<StemAssignment>> assignments=new HashMap<>();
  sPlans.forEach(p->assignments.computeIfAbsent(stemPlanKey(p.getRegistration().getId(),p.getGrade(),p.getTerm()),key->new ArrayList<>()).add(new StemAssignment(p.getId(),p.getEffectiveFromWeek()==null?1:p.getEffectiveFromWeek(),p.getCourse().getId())));
  sVersions.forEach(p->assignments.computeIfAbsent(stemPlanKey(p.getRegistration().getId(),p.getGrade(),p.getTerm()),key->new ArrayList<>()).add(new StemAssignment(p.getId(),p.getEffectiveFromWeek(),p.getCourse().getId())));
  for(Registration r:regs)if(r.getStemProgram()!=null)for(Integer grade:r.getStemGrades())for(int term=1;term<=4;term++){
   int termLength=termWeeks(r,term);List<StemAssignment> termAssignments=assignments.getOrDefault(stemPlanKey(r.getId(),grade,term),List.of());
   for(int weekNo=1;weekNo<=termLength;weekNo++){
    StemAssignment assigned=null;for(StemAssignment candidate:termAssignments)if(candidate.effectiveWeek()<=weekNo&&(assigned==null||candidate.effectiveWeek()>=assigned.effectiveWeek()))assigned=candidate;
    if(assigned==null)continue;
    if(termLength==9&&weekNo==9){StemEventLesson event=eventsByGrade.get(grade);if(event!=null)stem.add(status.get("STEM:"+assigned.id()+":"+event.getId()));continue;}
    List<StemLesson> ordered=orderedLessons(assigned,lessonsByCourse,orderByPlan);
    List<StemLesson> scheduledLessons=ordered.stream().filter(lesson->!isEventLesson(lesson)).limit(8).toList();
    int lessonIndex=weekNo-assigned.effectiveWeek();if(lessonIndex>=0&&lessonIndex<scheduledLessons.size()){StemLesson lesson=scheduledLessons.get(lessonIndex);stem.add(status.get("STEM:"+assigned.id()+":"+lesson.getId()));}
   }
  }
  List<DigitalPlan> dPlans=digitalPlans.findAll().stream().filter(p->schoolIds.contains(p.getRegistration().getSchool().getId())).toList();for(DigitalPlan p:dPlans){Registration r=p.getRegistration();if(r.getDigitalProgram()!=null)for(DigitalLesson l:digitalLessons.findAllByProgramAndGradeOrderByLessonOrderAsc(r.getDigitalProgram(),p.getGrade()))digital.add(status.get("DIGITAL:"+p.getId()+":"+l.getId()));}
  for(School school:visibleSchools){boolean hasStem=regs.stream().anyMatch(r->r.getSchool().getId().equals(school.getId())&&r.getStemProgram()!=null);boolean hasDigital=regs.stream().anyMatch(r->r.getSchool().getId().equals(school.getId())&&r.getDigitalProgram()!=null);if(hasStem&&hasDigital)both++;else if(hasStem)stemOnly++;else if(hasDigital)digitalOnly++;}
  LocalDate today=LocalDate.now(),nextMonday=today.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusWeeks(1);List<DashboardSummaryResponse.Task> upcoming=new ArrayList<>(),urgent=new ArrayList<>();
  for(Registration r:regs){School s=r.getSchool();for(String service:List.of("stem","digital")){if((service.equals("stem")?r.getStemProgram():r.getDigitalProgram())==null)continue;for(ScheduleWeekResponse w:schedule.weeks(r.getId(),service)){if(w.announced())continue;String program=service.equals("stem")?r.getStemProgram().name():r.getDigitalProgram().name();List<Integer> grades=w.items().stream().map(ScheduleWeekResponse.Item::grade).distinct().sorted().toList();DashboardSummaryResponse.Task task=new DashboardSummaryResponse.Task(r.getId(),s.getId(),s.getName(),s.getCode(),r.getAcademicYear(),service,program,w.weekStart(),w.weekEnd(),w.deadline(),w.label(),grades,w.urgent());if(w.weekStart().equals(nextMonday)&&!today.isAfter(w.deadline()))upcoming.add(task);if(w.urgent())urgent.add(task);}}
  }
  Comparator<DashboardSummaryResponse.Task> byDate=Comparator.comparing(DashboardSummaryResponse.Task::weekStart).thenComparing(DashboardSummaryResponse.Task::schoolName);upcoming.sort(byDate);urgent.sort(byDate);return new DashboardSummaryResponse(visibleSchools.size(),stemOnly,digitalOnly,both,stem.dto(),digital.dto(),upcoming,urgent);
 }
 private String stemPlanKey(UUID registrationId,Integer grade,Integer term){return registrationId+":"+grade+":"+term;}
 private int termWeeks(Registration registration,int term){Integer value=registration.getStemTermWeeks()==null?null:registration.getStemTermWeeks().get(term);return Integer.valueOf(8).equals(value)?8:9;}
 private List<StemLesson> orderedLessons(StemAssignment assignment,Map<String,List<StemLesson>> lessonsByCourse,Map<UUID,List<String>> orderByPlan){
  List<StemLesson> canonical=lessonsByCourse.getOrDefault(assignment.courseId(),List.of()).stream().filter(lesson->!isEventLesson(lesson)).toList();Map<String,StemLesson> byId=new HashMap<>();canonical.forEach(lesson->byId.put(lesson.getId(),lesson));List<StemLesson> ordered=new ArrayList<>();
  for(String id:orderByPlan.getOrDefault(assignment.id(),List.of())){StemLesson lesson=byId.get(id);if(lesson!=null&&ordered.stream().noneMatch(item->item.getId().equals(id)))ordered.add(lesson);}
  for(StemLesson lesson:canonical)if(ordered.stream().noneMatch(item->item.getId().equals(lesson.getId())))ordered.add(lesson);return ordered;
 }
 private boolean isEventLesson(StemLesson lesson){return lesson.getTitle()!=null&&lesson.getTitle().trim().toLowerCase(Locale.ROOT).equals("event tổng kết học phần");}
 private User current(){String username=SecurityContextHolder.getContext().getAuthentication().getName();return users.findByUsername(username).orElseThrow(()->new AppException(ErrorCode.USER_NOT_EXISTS));}private boolean isAdmin(User u){return u.getRoles()!=null&&(u.getRoles().contains(Role.ADMIN.name())||u.getRoles().contains(Role.ADMIN_SCHOOL.name()));}
}
