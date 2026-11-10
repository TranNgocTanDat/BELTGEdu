package com.example.beprojectweb.service;

import com.example.beprojectweb.dto.response.ScheduleWeekResponse;
import com.example.beprojectweb.entity.*;
import com.example.beprojectweb.enums.Role;
import com.example.beprojectweb.exception.AppException;
import com.example.beprojectweb.exception.ErrorCode;
import com.example.beprojectweb.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service @RequiredArgsConstructor
public class ScheduleService {
 private final RegistrationRepository registrations; private final StemPlanRepository stemPlans; private final StemPlanVersionRepository stemPlanVersions; private final StemPlanLessonOrderRepository stemLessonOrders; private final DigitalPlanRepository digitalPlans;
 private final StemCourseRepository courses; private final StemLessonRepository stemLessons; private final DigitalLessonRepository digitalLessons;
 private final StemEventLessonRepository stemEventLessons;
 private final SchoolClosureRepository closures; private final AnnouncementRepository announcements; private final UserRepository users;
 private final HolidayRepository holidays;
 private record MutableWeek(LocalDate start,LinkedHashSet<String> labels,List<ScheduleWeekResponse.Item> items){}

 @PreAuthorize("hasAnyRole('ADMIN','ADMIN_SCHOOL','USER')") @Transactional(readOnly=true)
 public List<ScheduleWeekResponse> weeks(UUID registrationId,String service){
  Registration reg=registrations.findById(registrationId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Registration not found")); assertAccess(current(),reg.getSchool());
  String type=service==null?"":service.toLowerCase(Locale.ROOT); if(!Set.of("stem","digital").contains(type)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"service must be stem or digital");
  if(type.equals("stem")&&reg.getStemProgram()==null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Registration has no STEM program"); if(type.equals("digital")&&reg.getDigitalProgram()==null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Registration has no Digital program");
  Map<LocalDate,MutableWeek> map=new TreeMap<>(); Set<LocalDate> closed=new HashSet<>(); for(SchoolClosure c:closures.findAllBySchool_IdOrderByWeekStartAsc(reg.getSchool().getId())) closed.add(c.getWeekStart());
  if(type.equals("stem")) addHolidayWeeks(closed,reg.getStemStudyDay()); else addHolidayWeeks(closed,reg.getDigitalStudyDay());
  if(type.equals("stem")) buildStem(reg,map,closed); else buildDigital(reg,map,closed);
  List<Announcement> schoolAnnouncements=announcements.findAllBySchool_IdOrderByWeekStartAsc(reg.getSchool().getId()); Set<LocalDate> announced=new HashSet<>(); String regId=reg.getId().toString(); schoolAnnouncements.stream().filter(a->a.getRegistrationId().equals(regId)&&a.getService().equals(type)).forEach(a->announced.add(a.getWeekStart()));
  Set<Integer> unlocked=new HashSet<>(); if(type.equals("stem")){unlocked.add(1); for(int t=1;t<4;t++){ if(!unlocked.contains(t)) break; final int term=t; LocalDate weekSeven=null; for(MutableWeek w:map.values()) if(w.items.stream().anyMatch(i->Objects.equals(i.term(),term)&&Objects.equals(i.lessonNo(),7))){weekSeven=w.start;break;} if(weekSeven==null||!announced.contains(weekSeven)) break; unlocked.add(t+1); }}
  LocalDate today=LocalDate.now(); List<ScheduleWeekResponse> out=new ArrayList<>(); for(MutableWeek w:map.values()){
   List<ScheduleWeekResponse.Item> items=w.items.stream().filter(i->type.equals("digital")||unlocked.contains(i.term())).sorted(Comparator.comparing(ScheduleWeekResponse.Item::grade)).toList(); if(items.isEmpty()) continue;
   List<String> labels=w.labels.stream().filter(label->{if(type.equals("digital"))return true;String[] parts=label.split(" · ",2);try{return unlocked.contains(Integer.parseInt(parts[0].replace("Học phần ","")));}catch(Exception e){return true;}}).toList(); LocalDate deadline=w.start.minusDays(4); boolean hasAnn=announced.contains(w.start); out.add(new ScheduleWeekResponse(w.start,w.start.plusDays(4),String.join(" + ",labels),hasAnn,true,!hasAnn&&today.isAfter(deadline),deadline,items));
  } return out;
 }
 private record SchedulePlan(UUID id,int effectiveWeek,StemCourse course){}
 private void buildStem(Registration r,Map<LocalDate,MutableWeek> map,Set<LocalDate> closed){
  List<StemPlan> basePlans=stemPlans.findAllByRegistration_IdOrderByGradeAscTermAsc(r.getId());
  List<StemPlanVersion> versions=stemPlanVersions.findAllByRegistration_IdInAndActiveTrueOrderByGradeAscTermAscEffectiveFromWeekAsc(List.of(r.getId()));
  List<UUID> planIds=new ArrayList<>();
  basePlans.forEach(p->planIds.add(p.getId()));versions.forEach(p->planIds.add(p.getId()));
  Map<UUID,List<String>> lessonOrders=new HashMap<>();
  if(!planIds.isEmpty())stemLessonOrders.findAllByPlanItemIdInOrderByPlanItemIdAscPositionAsc(planIds)
          .forEach(item->lessonOrders.computeIfAbsent(item.getPlanItemId(),key->new ArrayList<>()).add(item.getLessonId()));
  List<String> courseIds=new ArrayList<>();
  for(StemPlan plan:basePlans)courseIds.add(plan.getCourse().getId());
  for(StemPlanVersion plan:versions)courseIds.add(plan.getCourse().getId());
  List<String> distinctCourseIds=courseIds.stream().distinct().toList();
  Map<String,List<StemLesson>> lessonsByCourse=distinctCourseIds.isEmpty()?Map.of():stemLessons.findAllByCourse_IdInOrderByCourse_IdAscLessonOrderAsc(distinctCourseIds)
          .stream().collect(java.util.stream.Collectors.groupingBy(l->l.getCourse().getId()));
  Map<Integer,List<StemPlan>> baseByTerm=basePlans.stream().collect(java.util.stream.Collectors.groupingBy(StemPlan::getTerm));
  Map<Integer,List<StemPlanVersion>> versionsByTerm=versions.stream().collect(java.util.stream.Collectors.groupingBy(StemPlanVersion::getTerm));
  Map<Integer,StemEventLesson> eventsByGrade=stemEventLessons.findAllByOrderByGradeAsc().stream()
          .collect(java.util.stream.Collectors.toMap(StemEventLesson::getGrade,event->event));
  LocalDate nextTerm=null;LocalDate[] starts={r.getTerm1Start(),r.getTerm2Start(),r.getTerm3Start(),r.getTerm4Start()};
  for(int term=1;term<=4;term++){
   LocalDate raw=starts[term-1];if(raw==null)continue;
   int termLength=termWeeks(r,term);LocalDate originalStart=weekStart(raw);
   LocalDate scheduled=nextTerm!=null&&nextTerm.isAfter(originalStart)?nextTerm:originalStart;
   for(int weekNo=1;weekNo<=termLength;weekNo++){
    scheduled=skipClosed(scheduled,closed);LocalDate original=originalStart.plusWeeks(weekNo-1L);
    MutableWeek row=week(map,scheduled,"Học phần "+term+" · Tuần "+weekNo);
    for(Integer grade:r.getStemGrades()){
     List<SchedulePlan> candidates=new ArrayList<>();
     for(StemPlan p:baseByTerm.getOrDefault(term,List.of()))
      if(p.getGrade().equals(grade))candidates.add(new SchedulePlan(p.getId(),p.getEffectiveFromWeek()==null?1:p.getEffectiveFromWeek(),p.getCourse()));
     for(StemPlanVersion p:versionsByTerm.getOrDefault(term,List.of()))
      if(p.getGrade().equals(grade))candidates.add(new SchedulePlan(p.getId(),p.getEffectiveFromWeek(),p.getCourse()));
     SchedulePlan assigned=null;
     for(SchedulePlan candidate:candidates)
      if(candidate.effectiveWeek()<=weekNo&&(assigned==null||candidate.effectiveWeek()>=assigned.effectiveWeek()))assigned=candidate;
     if(assigned==null)continue;
     List<StemLesson> catalog=lessonsByCourse.getOrDefault(assigned.course().getId(),List.of());
     catalog=catalog.stream().filter(lesson->!isStemEventLesson(lesson)).toList();
     Map<String,StemLesson> byId=catalog.stream().collect(java.util.stream.Collectors.toMap(StemLesson::getId,item->item));
     List<String> ids=lessonOrders.getOrDefault(assigned.id(),List.of());List<StemLesson> ordered=new ArrayList<>();
     for(String lessonId:ids)if(byId.containsKey(lessonId)&&ordered.stream().noneMatch(item->item.getId().equals(lessonId)))ordered.add(byId.get(lessonId));
     for(StemLesson lesson:catalog)if(ordered.stream().noneMatch(item->item.getId().equals(lesson.getId())))ordered.add(lesson);
     if(termLength==9&&weekNo==9){
      StemEventLesson event=eventsByGrade.get(grade);
      if(event!=null)row.items.add(new ScheduleWeekResponse.Item(grade,null,event.getTitle(),weekNo,term,original));
      continue;
     }
     int assignedIndex=weekNo-assigned.effectiveWeek();
     List<StemLesson> scheduledLessons=ordered.stream().limit(8).toList();
     if(assignedIndex>=0&&assignedIndex<scheduledLessons.size()){
      StemLesson lesson=scheduledLessons.get(assignedIndex);
      row.items.add(new ScheduleWeekResponse.Item(grade,assigned.course().getCode(),lesson.getTitle(),weekNo,term,original));
     }
    }
    scheduled=scheduled.plusWeeks(1);
   }
   nextTerm=scheduled;
  }
 }
 private int termWeeks(Registration registration,int term){
  Integer configured=registration.getStemTermWeeks()==null?null:registration.getStemTermWeeks().get(term);
  return Integer.valueOf(8).equals(configured)?8:9;
 }
 private boolean isStemEventLesson(StemLesson lesson){
  return lesson.getTitle()!=null&&lesson.getTitle().trim().toLowerCase(Locale.ROOT).equals("event tổng kết học phần");
 }
 private void buildDigital(Registration r,Map<LocalDate,MutableWeek> map,Set<LocalDate> closed){
  if(r.getDigitalStart()==null)return;LocalDate start=weekStart(r.getDigitalStart()),scheduled=start;
  List<DigitalPlan> plans=digitalPlans.findAllByRegistration_IdOrderByGradeAsc(r.getId()).stream().filter(p->r.getDigitalGrades().contains(p.getGrade())).toList();
  List<Integer> grades=plans.stream().map(DigitalPlan::getGrade).distinct().toList();
  Map<Integer,List<DigitalLesson>> byGrade=grades.isEmpty()?Map.of():digitalLessons.findAllByProgramAndGradeInOrderByGradeAscLessonOrderAsc(r.getDigitalProgram(),grades).stream().collect(java.util.stream.Collectors.groupingBy(DigitalLesson::getGrade));
  int max=byGrade.values().stream().mapToInt(List::size).max().orElse(0);
  for(int i=0;i<max;i++){scheduled=skipClosed(scheduled,closed);MutableWeek w=week(map,scheduled,"Tuần "+(i+1));for(DigitalPlan p:plans){List<DigitalLesson> ls=byGrade.getOrDefault(p.getGrade(),List.of());if(i<ls.size())w.items.add(new ScheduleWeekResponse.Item(p.getGrade(),null,ls.get(i).getTitle(),i+1,null,start.plusWeeks(i)));}scheduled=scheduled.plusWeeks(1);}
 }
 private MutableWeek week(Map<LocalDate,MutableWeek> map,LocalDate date,String label){MutableWeek w=map.computeIfAbsent(date,d->new MutableWeek(d,new LinkedHashSet<>(),new ArrayList<>()));w.labels.add(label);return w;}
 private LocalDate skipClosed(LocalDate date,Set<LocalDate> closed){while(closed.contains(date))date=date.plusWeeks(1);return date;}
 private LocalDate weekStart(LocalDate d){int dow=d.getDayOfWeek().getValue();return d.plusDays(dow>=6?8-dow:1-dow);}
 private void addHolidayWeeks(Set<LocalDate> closed,DayOfWeek studyDay){if(studyDay==null)return;for(Holiday holiday:holidays.findAll()){LocalDate day=holiday.getStartDate();while(!day.isAfter(holiday.getEndDate())){if(day.getDayOfWeek()==studyDay)closed.add(weekStart(day));day=day.plusDays(1);}}}
 private User current(){String username=SecurityContextHolder.getContext().getAuthentication().getName();return users.findByUsername(username).orElseThrow(()->new AppException(ErrorCode.USER_NOT_EXISTS));}
 private boolean isAdmin(User u){return u.getRoles()!=null&&(u.getRoles().contains(Role.ADMIN.name())||u.getRoles().contains(Role.ADMIN_SCHOOL.name()));}
 private void assertAccess(User u,School s){if(!isAdmin(u)&&(u.getRegion()==null||!u.getRegion().getId().equals(s.getRegion().getId())))throw new AccessDeniedException("You can only view schedules in your assigned region");}
}

