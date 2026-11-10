package com.example.beprojectweb.service;

import com.example.beprojectweb.dto.request.*;
import com.example.beprojectweb.dto.response.*;
import com.example.beprojectweb.entity.*;
import com.example.beprojectweb.enums.*;
import com.example.beprojectweb.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.*;

@Service @RequiredArgsConstructor
public class StudyPlanService {
 private final RegistrationRepository registrations; private final SchoolRepository schools; private final UserRepository users;
 private final StemPlanRepository stemPlans; private final StemPlanVersionRepository stemPlanVersions; private final StemPlanLessonOrderRepository stemLessonOrders; private final DigitalPlanRepository digitalPlans; private final LessonProgressRepository progress;
 private final StemCourseRepository courses; private final StemLessonRepository stemLessons; private final DigitalLessonRepository digitalLessons;
 private final StemEventLessonRepository stemEventLessons;
 private final StemEventGradeLessonRepository stemEventGradeLessons; private final RegistrationStemEventRepository registrationStemEvents;
 private final ScheduleService scheduleService;
 private final AnnouncementRepository announcements; private final SchoolClosureRepository closures; private final HolidayRepository holidays;

 @PreAuthorize("hasAnyRole('ADMIN','ADMIN_SCHOOL','USER')") @Transactional(readOnly=true)
 public List<RegistrationResponse> list(UUID schoolId){ User u=current(); List<Registration> all=isAdmin(u)?(schoolId==null?registrations.findAll():registrations.findAllBySchool_Id(schoolId)):(u.getRegion()==null?List.of():registrations.findAllBySchool_Region_Id(u.getRegion().getId())); return all.stream().filter(r->schoolId==null||r.getSchool().getId().equals(schoolId)).map(this::response).toList(); }
 @PreAuthorize("hasAnyRole('ADMIN','ADMIN_SCHOOL','USER')") @Transactional(readOnly=true)
 public RegistrationResponse get(UUID id){ Registration r=findRegistration(id); assertAccess(current(),r.getSchool()); return response(r); }
 @PreAuthorize("hasAnyRole('ADMIN','ADMIN_SCHOOL','USER')") @Transactional
 public RegistrationResponse create(RegistrationRequest req){ validate(req); School school=schools.findById(req.getSchoolId()).orElseThrow(()->notFound("School not found")); assertAccess(current(),school); if(registrations.existsBySchool_IdAndAcademicYear(school.getId(),req.getAcademicYear().trim())) throw new ResponseStatusException(HttpStatus.CONFLICT,"This school already has a registration for the academic year"); Registration r=map(req,school,new Registration()); r=registrations.save(r); if(r.getDigitalProgram()!=null) for(Integer g:r.getDigitalGrades()) digitalPlans.save(DigitalPlan.builder().registration(r).grade(g).build()); return response(r); }
 @PreAuthorize("hasAnyRole('ADMIN','ADMIN_SCHOOL','USER')") @Transactional
 public RegistrationResponse update(UUID id,RegistrationRequest req){ validate(req); Registration r=findRegistration(id); assertAccess(current(),r.getSchool()); School school=req.getSchoolId()==null?r.getSchool():schools.findById(req.getSchoolId()).orElseThrow(()->notFound("School not found")); assertAccess(current(),school); if(registrations.existsBySchool_IdAndAcademicYearAndIdNot(school.getId(),req.getAcademicYear().trim(),id)) throw new ResponseStatusException(HttpStatus.CONFLICT,"This school already has a registration for the academic year"); validateStemTermWeekChange(r,req); reconcile(r,req); map(req,school,r); r=registrations.save(r); if(r.getDigitalProgram()!=null) for(Integer g:r.getDigitalGrades()) if(digitalPlans.findByRegistration_IdAndGrade(r.getId(),g).isEmpty()) digitalPlans.save(DigitalPlan.builder().registration(r).grade(g).build()); return response(r); }
 @PreAuthorize("hasAnyRole('ADMIN','ADMIN_SCHOOL','USER')") @Transactional
 public void delete(UUID id){
  Registration r=findRegistration(id); assertAccess(current(),r.getSchool());
  registrationStemEvents.deleteAllByRegistration_Id(id);
  for(StemPlanVersion p:stemPlanVersions.findAllByRegistration_IdOrderByGradeAscTermAscEffectiveFromWeekAsc(id)){progress.deleteAllByPlanTypeAndPlanItemId("STEM",p.getId());stemLessonOrders.deleteAllByPlanItemId(p.getId());stemPlanVersions.delete(p);}
  for(StemPlan p:stemPlans.findAllByRegistration_IdOrderByGradeAscTermAsc(id)){progress.deleteAllByPlanTypeAndPlanItemId("STEM",p.getId());stemLessonOrders.deleteAllByPlanItemId(p.getId());stemPlans.delete(p);}
  for(DigitalPlan p:digitalPlans.findAllByRegistration_IdOrderByGradeAsc(id)){progress.deleteAllByPlanTypeAndPlanItemId("DIGITAL",p.getId());digitalPlans.delete(p);}
  announcements.deleteAllByRegistrationId(id.toString()); registrations.delete(r);
 }

 @PreAuthorize("hasAnyRole('ADMIN','ADMIN_SCHOOL','USER')") @Transactional(readOnly=true)
 public List<StemPlanResponse> getStemPlans(UUID registrationId){
  User user=current();
  List<Registration> visible=registrationId==null?visibleRegistrations(user):List.of(accessibleRegistration(user,registrationId));
  List<UUID> ids=visible.stream().map(Registration::getId).toList();
  if(ids.isEmpty())return List.of();
  List<StemPlanResponse> result=new ArrayList<>();
  stemPlans.findAllByRegistration_IdInOrderByGradeAscTermAsc(ids).forEach(p->result.add(stemResponse(p)));
  stemPlanVersions.findAllByRegistration_IdInAndActiveTrueOrderByGradeAscTermAscEffectiveFromWeekAsc(ids).forEach(p->result.add(stemResponse(p)));
  result.sort(Comparator.comparing(StemPlanResponse::grade).thenComparing(StemPlanResponse::term)
          .thenComparing(StemPlanResponse::effectiveFromWeek).thenComparing(StemPlanResponse::version));
  List<UUID> planIds=result.stream().map(StemPlanResponse::id).toList();
  if(planIds.isEmpty())return result;
  Map<UUID,List<String>> orderByPlan=new HashMap<>();
  stemLessonOrders.findAllByPlanItemIdInOrderByPlanItemIdAscPositionAsc(planIds).forEach(item->
          orderByPlan.computeIfAbsent(item.getPlanItemId(),key->new ArrayList<>()).add(item.getLessonId()));
  return result.stream().map(item->new StemPlanResponse(item.id(),item.registrationId(),item.grade(),item.term(),
          item.courseId(),item.effectiveFromWeek(),item.changeNote(),item.version(),
          orderByPlan.getOrDefault(item.id(),List.of()))).toList();
 }

 @PreAuthorize("hasAnyRole('ADMIN','ADMIN_SCHOOL','USER')") @Transactional
 public StemPlanResponse setStemPlan(StemPlanRequest req){
  Registration r=findRegistration(req.getRegistrationId()); assertAccess(current(),r.getSchool());
  int grade=req.getGrade()==null?0:req.getGrade(), term=req.getTerm()==null?0:req.getTerm();
  if(r.getStemProgram()==null||!r.getStemGrades().contains(grade))throw bad("Grade is not enrolled in STEM");
  if(term<1||term>4)throw bad("term must be between 1 and 4");
  StemPlan base=stemPlans.findByRegistration_IdAndGradeAndTerm(r.getId(),grade,term).orElse(null);
  List<StemPlanVersion> versions=stemPlanVersions.findAllByRegistration_IdAndGradeAndTermOrderByEffectiveFromWeekAsc(r.getId(),grade,term);
  int safeWeek=firstEditableStemWeek(r,grade,term);
  int requestedWeek=req.getEffectiveFromWeek()==null?safeWeek:req.getEffectiveFromWeek();
  if(requestedWeek<1||requestedWeek>termWeeks(r,term))throw bad("effectiveFromWeek must be between 1 and "+termWeeks(r,term));
  if(requestedWeek!=safeWeek)throw new ResponseStatusException(HttpStatus.CONFLICT,
          "Khóa học chỉ được đổi từ tuần chưa báo/hoàn thành tiếp theo: tuần "+safeWeek+".");

  if(req.getCourseId()==null||req.getCourseId().isBlank()){
   boolean hasAnyProgress=base!=null&&!progress.findAllByPlanTypeAndPlanItemId("STEM",base.getId()).isEmpty()
           ||versions.stream().anyMatch(v->!progress.findAllByPlanTypeAndPlanItemId("STEM",v.getId()).isEmpty());
   if(base==null&&versions.isEmpty())return null;
   if(safeWeek>1||hasAnyProgress||!versions.isEmpty())
    throw new ResponseStatusException(HttpStatus.CONFLICT,"Kế hoạch đã có lịch sử hoặc tiến độ. Hãy chọn khóa học thay thế để giữ nguyên dữ liệu.");
   if(base!=null){progress.deleteAllByPlanTypeAndPlanItemId("STEM",base.getId());stemLessonOrders.deleteAllByPlanItemId(base.getId());stemPlans.delete(base);}
   return null;
  }

  StemCourse course=courses.findById(req.getCourseId()).orElseThrow(()->notFound("STEM course not found"));
  boolean usedInOtherTerm=stemPlans.findAllByRegistration_IdAndGrade(r.getId(),grade).stream()
          .anyMatch(x->x.getTerm()!=term&&x.getCourse().getId().equals(course.getId()))
          ||stemPlanVersions.findAllByRegistration_IdAndGrade(r.getId(),grade).stream()
          .anyMatch(x->x.isActive()&&x.getTerm()!=term&&x.getCourse().getId().equals(course.getId()));
  if(usedInOtherTerm)throw new ResponseStatusException(HttpStatus.CONFLICT,"This course is already assigned to another term for the grade");
  if(course.getProgram()!=r.getStemProgram()||(course.getGrade()!=null&&!course.getGrade().equals(grade)))
   throw bad("Course does not match the registered program and grade");

  StemPlanVersion activeAtBoundary=activeAssignmentAt(base,versions,requestedWeek);
  String previousCourseId=activeAtBoundary==null?null:activeAtBoundary.getCourse().getId();
  boolean futureVersion=versions.stream().anyMatch(v->v.isActive()&&v.getEffectiveFromWeek()>=requestedWeek);
  if(course.getId().equals(previousCourseId)&&!futureVersion)return stemResponse(activeAtBoundary);
  String note=changeNote(previousCourseId,course.getId(),requestedWeek);
  if(req.getChangeNote()!=null&&!req.getChangeNote().isBlank())note+=" Ghi chú: "+req.getChangeNote().trim();

  if(base==null&&versions.isEmpty()){
   StemPlan initial=stemPlans.save(StemPlan.builder().registration(r).grade(grade).term(term).course(course)
           .effectiveFromWeek(requestedWeek).changeNote(note).build());
   return stemResponse(initial);
  }

  for(StemPlanVersion version:versions)
   if(version.isActive()&&version.getEffectiveFromWeek()>=requestedWeek){
    if(!progress.findAllByPlanTypeAndPlanItemId("STEM",version.getId()).isEmpty())
     throw new ResponseStatusException(HttpStatus.CONFLICT,"Không thể thay khóa đã có tiến độ ở các tuần phía sau.");
    version.setActive(false);stemPlanVersions.save(version);
   }

  boolean baseCanStartHere=base!=null&&base.getEffectiveFromWeek()>=requestedWeek
          &&progress.findAllByPlanTypeAndPlanItemId("STEM",base.getId()).isEmpty();
  if(baseCanStartHere){
   if(!base.getCourse().getId().equals(course.getId()))stemLessonOrders.deleteAllByPlanItemId(base.getId());
   base.setEffectiveFromWeek(requestedWeek);base.setCourse(course);base.setChangeNote(note);
   return stemResponse(stemPlans.save(base));
  }

  StemPlanVersion reusable=versions.stream().filter(v->v.getEffectiveFromWeek().equals(requestedWeek))
          .max(Comparator.comparing(StemPlanVersion::getId)).orElse(null);
  if(reusable!=null){
   if(!reusable.getCourse().getId().equals(course.getId()))stemLessonOrders.deleteAllByPlanItemId(reusable.getId());
   reusable.setCourse(course);reusable.setChangeNote(note);reusable.setActive(true);
   return stemResponse(stemPlanVersions.save(reusable));
  }
  return stemResponse(stemPlanVersions.save(StemPlanVersion.builder().registration(r).grade(grade).term(term)
          .course(course).effectiveFromWeek(requestedWeek).changeNote(note).active(true).build()));
 }

 @PreAuthorize("hasAnyRole('ADMIN','ADMIN_SCHOOL','USER')") @Transactional
 public StemPlanResponse setStemLessonOrder(StemPlanLessonOrderRequest req){
  if(req==null||req.getRegistrationId()==null||req.getGrade()==null||req.getTerm()==null||req.getPlanItemId()==null
          ||req.getLessonIds()==null||req.getLessonIds().isEmpty())throw bad("registrationId, grade, term, planItemId and lessonIds are required");
  Registration r=findRegistration(req.getRegistrationId());assertAccess(current(),r.getSchool());
  int grade=req.getGrade(),term=req.getTerm();
  if(term<1||term>4)throw bad("term must be between 1 and 4");
  if(r.getStemProgram()==null||!r.getStemGrades().contains(grade))throw bad("Grade is not enrolled in STEM");
  StemPlan base=stemPlans.findByRegistration_IdAndGradeAndTerm(r.getId(),grade,term).orElse(null);
  List<StemPlanVersion> versions=stemPlanVersions.findAllByRegistration_IdAndGradeAndTermOrderByEffectiveFromWeekAsc(r.getId(),grade,term);
  StemPlanVersion assignment=null;
  if(base!=null&&base.getId().equals(req.getPlanItemId()))
   assignment=StemPlanVersion.builder().id(base.getId()).registration(base.getRegistration()).grade(base.getGrade()).term(base.getTerm())
           .course(base.getCourse()).effectiveFromWeek(base.getEffectiveFromWeek()==null?1:base.getEffectiveFromWeek()).changeNote(base.getChangeNote()).active(true).build();
  if(assignment==null)assignment=versions.stream().filter(v->v.getId().equals(req.getPlanItemId())&&v.isActive()).findFirst()
          .orElseThrow(()->notFound("STEM plan assignment not found or inactive"));
  if(!assignment.getGrade().equals(grade)||!assignment.getTerm().equals(term))throw bad("The STEM plan assignment does not match grade and term");
  List<StemLesson> lessons=stemLessons.findAllByCourse_IdOrderByLessonOrderAsc(assignment.getCourse().getId()).stream()
          .filter(lesson->!isStemEventLesson(lesson)).toList();
  if(lessons.isEmpty())throw new ResponseStatusException(HttpStatus.CONFLICT,"Khóa học chưa có bài học để sắp xếp.");
  int termLength=termWeeks(r,term);
  List<String> canonical=lessons.stream().map(StemLesson::getId).toList();
  if(req.getLessonIds().size()!=canonical.size()||new HashSet<>(req.getLessonIds()).size()!=canonical.size()
          ||!new HashSet<>(req.getLessonIds()).equals(new HashSet<>(canonical)))
   throw bad("lessonIds must contain each lesson in the assigned course exactly once");
  List<String> storedOrder=stemLessonOrders.findAllByPlanItemIdOrderByPositionAsc(assignment.getId()).stream()
          .map(StemPlanLessonOrder::getLessonId).toList();
  List<String> currentOrder=new ArrayList<>();
  for(String lessonId:storedOrder)if(canonical.contains(lessonId)&&!currentOrder.contains(lessonId))currentOrder.add(lessonId);
  for(String lessonId:canonical)if(!currentOrder.contains(lessonId))currentOrder.add(lessonId);
  List<String> currentScheduledOrder=scheduledLessonIds(currentOrder,lessons,termLength);
  List<String> requestedScheduledOrder=scheduledLessonIds(req.getLessonIds(),lessons,termLength);
  List<ScheduleWeekResponse> weeks=scheduleService.allWeeksForSummary(r.getId(),"stem");
  int start=assignment.getEffectiveFromWeek();
  int end=termLength;
  for(StemPlanVersion other:versions)
   if(other.isActive()&&other.getEffectiveFromWeek()>start)end=Math.min(end,other.getEffectiveFromWeek()-1);
  if(start>end)throw new ResponseStatusException(HttpStatus.CONFLICT,"Phân đoạn khóa học không có tuần hợp lệ để sắp xếp.");
  final UUID assignmentId=assignment.getId();
  for(int weekNo=start;weekNo<=end;weekNo++){
   int position=weekNo-start;
   if(position>=currentScheduledOrder.size()||position>=requestedScheduledOrder.size())break;
   final int currentWeekNo=weekNo;
   LocalDate scheduledDate=weeks.stream().filter(week->week.items().stream().anyMatch(item->!item.event()
           &&assignmentId.equals(item.planItemId())&&Objects.equals(item.term(),term)&&Objects.equals(item.lessonNo(),currentWeekNo)))
           .map(ScheduleWeekResponse::weekStart).findFirst().orElse(null);
   boolean protectedWeek=scheduledDate!=null&&announcements.findByKey(r.getId()+":stem:"+scheduledDate).isPresent();
   String lessonId=currentScheduledOrder.get(position);
   Optional<LessonProgress> saved=progress.findByPlanTypeAndPlanItemIdAndLessonId("STEM",assignment.getId(),lessonId);
   protectedWeek|=saved.isPresent()&&!"TODO".equalsIgnoreCase(saved.get().getStatus());
   if(protectedWeek&&!currentScheduledOrder.get(position).equals(requestedScheduledOrder.get(position)))
    throw new ResponseStatusException(HttpStatus.CONFLICT,"Không thể đổi thứ tự bài thuộc tuần đã báo hoặc đã có tiến độ.");
  }
  for(LessonProgress saved:progress.findAllByPlanTypeAndPlanItemId("STEM",assignment.getId()))
   if(!"TODO".equalsIgnoreCase(saved.getStatus())&&currentOrder.indexOf(saved.getLessonId())!=req.getLessonIds().indexOf(saved.getLessonId()))
    throw new ResponseStatusException(HttpStatus.CONFLICT,"Không thể di chuyển bài đã có tiến độ.");
  stemLessonOrders.deleteAllByPlanItemId(assignment.getId());
  List<StemPlanLessonOrder> rows=new ArrayList<>();
  for(int i=0;i<req.getLessonIds().size();i++)rows.add(StemPlanLessonOrder.builder()
          .planItemId(assignment.getId()).lessonId(req.getLessonIds().get(i)).position(i+1).build());
  stemLessonOrders.saveAll(rows);
  boolean isVersion=false;
  for(StemPlanVersion version:versions)if(version.getId().equals(assignment.getId())){isVersion=true;break;}
  return new StemPlanResponse(assignment.getId(),r.getId(),grade,term,assignment.getCourse().getId(),start,
          assignment.getChangeNote(),isVersion,req.getLessonIds());
 }

 @PreAuthorize("hasAnyRole('ADMIN','ADMIN_SCHOOL','USER')") @Transactional(readOnly=true)
 public List<DigitalPlanResponse> getDigitalPlans(UUID registrationId){ User user=current();List<Registration> visible=registrationId==null?visibleRegistrations(user):List.of(accessibleRegistration(user,registrationId));List<UUID> ids=visible.stream().map(Registration::getId).toList();if(ids.isEmpty())return List.of();return digitalPlans.findAllByRegistration_IdInOrderByGradeAsc(ids).stream().map(p->new DigitalPlanResponse(p.getId(),p.getRegistration().getId(),p.getGrade())).toList(); }
 @PreAuthorize("hasAnyRole('ADMIN','ADMIN_SCHOOL','USER')") @Transactional(readOnly=true)
 public List<LessonProgressResponse> getProgress(UUID registrationId){
  User user=current();
  List<Registration> visible=registrationId==null?visibleRegistrations(user):List.of(accessibleRegistration(user,registrationId));
  List<UUID> ids=visible.stream().map(Registration::getId).toList();if(ids.isEmpty())return List.of();
  Map<UUID,UUID> registrationByPlan=new HashMap<>();
  stemPlans.findAllByRegistration_IdInOrderByGradeAscTermAsc(ids).forEach(p->registrationByPlan.put(p.getId(),p.getRegistration().getId()));
  stemPlanVersions.findAllByRegistration_IdInAndActiveTrueOrderByGradeAscTermAscEffectiveFromWeekAsc(ids).forEach(p->registrationByPlan.put(p.getId(),p.getRegistration().getId()));
  digitalPlans.findAllByRegistration_IdInOrderByGradeAsc(ids).forEach(p->registrationByPlan.put(p.getId(),p.getRegistration().getId()));
  if(registrationByPlan.isEmpty())return List.of();
  return progress.findAllByPlanItemIdIn(registrationByPlan.keySet()).stream().map(p->progressResponse(p,registrationByPlan.get(p.getPlanItemId()))).toList();
 }

 @PreAuthorize("hasAnyRole('ADMIN','ADMIN_SCHOOL','USER')") @Transactional
 public LessonProgressResponse setProgress(LessonProgressRequest req){
  String type=req.getPlanType()==null?"":req.getPlanType().toUpperCase(Locale.ROOT);
  if(!Set.of("STEM","DIGITAL").contains(type))throw bad("planType must be STEM or DIGITAL");
  if(req.getPlanItemId()==null||req.getLessonId()==null||req.getLessonId().isBlank())throw bad("planItemId and lessonId are required");
  Registration reg;
  if(type.equals("STEM")){
   String courseId;int grade;int term;
   StemPlan p=stemPlans.findById(req.getPlanItemId()).orElse(null);
   if(p!=null){reg=p.getRegistration();courseId=p.getCourse().getId();grade=p.getGrade();term=p.getTerm();}
   else{
    StemPlanVersion v=stemPlanVersions.findById(req.getPlanItemId()).orElseThrow(()->notFound("STEM plan not found"));
    if(!v.isActive())throw notFound("STEM plan assignment is no longer active");
    reg=v.getRegistration();courseId=v.getCourse().getId();grade=v.getGrade();term=v.getTerm();
   }
   assertAccess(current(),reg.getSchool());
   StemLesson selectedLesson=stemLessons.findById(req.getLessonId()).orElse(null);
   boolean regularLesson=selectedLesson!=null&&!isStemEventLesson(selectedLesson)
           &&selectedLesson.getCourse().getId().equals(courseId);
   StemEventGradeLesson eventLesson=stemEventGradeLessons.findById(req.getLessonId()).orElse(null);
   boolean sharedEvent=reg.getStemGrades().contains(grade)&&eventLesson!=null&&eventLesson.getGrade().equals(grade)
           &&registrationStemEvents.findByRegistration_IdAndTermAndEvent_Id(reg.getId(),term,eventLesson.getEvent().getId()).isPresent()
           &&scheduleService.allWeeksForSummary(reg.getId(),"stem").stream().anyMatch(week->week.items().stream()
                   .anyMatch(item->item.event()&&Objects.equals(item.term(),term)
                           &&Objects.equals(item.planItemId(),req.getPlanItemId())
                           &&Objects.equals(item.lessonId(),eventLesson.getId())));
   if(!regularLesson&&!sharedEvent)throw notFound("Lesson does not belong to this course or grade Event");
  }else{
   DigitalPlan p=digitalPlans.findById(req.getPlanItemId()).orElseThrow(()->notFound("Digital plan not found"));
   reg=p.getRegistration();assertAccess(current(),reg.getSchool());
   if(reg.getDigitalProgram()==null||!digitalLessons.existsByIdAndProgramAndGrade(req.getLessonId(),reg.getDigitalProgram(),p.getGrade()))
    throw notFound("Lesson does not belong to this digital plan");
  }
  String status=req.getStatus()==null?"":req.getStatus().toUpperCase(Locale.ROOT);
  if(!Set.of("TODO","DOING","DONE").contains(status))throw bad("status must be TODO, DOING or DONE");
  if(status.equals("TODO")){progress.deleteByPlanTypeAndPlanItemIdAndLessonId(type,req.getPlanItemId(),req.getLessonId());return null;}
  LessonProgress x=progress.findByPlanTypeAndPlanItemIdAndLessonId(type,req.getPlanItemId(),req.getLessonId()).orElseGet(LessonProgress::new);
  x.setPlanType(type);x.setPlanItemId(req.getPlanItemId());x.setLessonId(req.getLessonId());x.setStatus(status);x.setDate(LocalDate.now());
  return progressResponse(progress.save(x),reg.getId());
 }



 private Registration map(RegistrationRequest q,School s,Registration r){
  r.setSchool(s);r.setAcademicYear(q.getAcademicYear().trim());
  RegistrationRequest.StemConfig st=q.getStem();
  r.setStemProgram(st==null?null:st.getProgram());
  r.setStemGrades(st==null?new LinkedHashSet<>():new LinkedHashSet<>(st.getGrades()));
  r.setTerm1Start(st==null?null:st.getTerm1Start());r.setTerm2Start(st==null?null:st.getTerm2Start());
  r.setTerm3Start(st==null?null:st.getTerm3Start());r.setTerm4Start(st==null?null:st.getTerm4Start());
  Map<Integer,Integer> configuredTermWeeks=st==null?new LinkedHashMap<>():normalizeTermWeeks(st.getTermWeeks(),r.getStemTermWeeks());
  if(r.getStemTermWeeks()==null)r.setStemTermWeeks(new LinkedHashMap<>());else r.getStemTermWeeks().clear();
  r.getStemTermWeeks().putAll(configuredTermWeeks);
  r.setStemStudyDay(st==null?null:st.getStudyDay());
  RegistrationRequest.DigitalConfig d=q.getDigital();
  r.setDigitalProgram(d==null?null:d.getProgram());r.setDigitalGrades(d==null?new LinkedHashSet<>():new LinkedHashSet<>(d.getGrades()));
  r.setDigitalStart(d==null?null:d.getStartDate());r.setDigitalStudyDay(d==null?null:d.getStudyDay());return r;
 }
 private void reconcile(Registration r,RegistrationRequest q){
  Set<Integer> newStem=q.getStem()==null?Set.of():q.getStem().getGrades();
  for(StemPlan p:stemPlans.findAllByRegistration_IdOrderByGradeAscTermAsc(r.getId()))
   if(q.getStem()==null||!newStem.contains(p.getGrade())||q.getStem().getProgram()!=r.getStemProgram()){
    progress.deleteAllByPlanTypeAndPlanItemId("STEM",p.getId());stemLessonOrders.deleteAllByPlanItemId(p.getId());stemPlans.delete(p);
   }
  for(StemPlanVersion p:stemPlanVersions.findAllByRegistration_IdOrderByGradeAscTermAscEffectiveFromWeekAsc(r.getId()))
   if(q.getStem()==null||!newStem.contains(p.getGrade())||q.getStem().getProgram()!=r.getStemProgram()){
    progress.deleteAllByPlanTypeAndPlanItemId("STEM",p.getId());stemLessonOrders.deleteAllByPlanItemId(p.getId());stemPlanVersions.delete(p);
   }
  Set<Integer> newDigital=q.getDigital()==null?Set.of():q.getDigital().getGrades();
  for(DigitalPlan p:digitalPlans.findAllByRegistration_IdOrderByGradeAsc(r.getId()))
   if(q.getDigital()==null||!newDigital.contains(p.getGrade())||q.getDigital().getProgram()!=r.getDigitalProgram()){
    progress.deleteAllByPlanTypeAndPlanItemId("DIGITAL",p.getId());digitalPlans.delete(p);
   }
 }

 private int termWeeks(Registration r,int term){
  return termWeeks(r.getStemTermWeeks(),term);
 }

 private int termWeeks(Map<Integer,Integer> termWeeks,int term){
  Integer configured=termWeeks==null?null:termWeeks.get(term);
  return Integer.valueOf(8).equals(configured)?8:9;
 }

 private Map<Integer,Integer> normalizeTermWeeks(Map<Integer,Integer> supplied,Map<Integer,Integer> fallback){
  Map<Integer,Integer> result=new LinkedHashMap<>();
  for(int term=1;term<=4;term++){
   Integer weeks=supplied==null?null:supplied.get(term);
   if(weeks==null&&fallback!=null)weeks=fallback.get(term);
   result.put(term,Integer.valueOf(8).equals(weeks)?8:9);
  }
  return result;
 }

 private List<String> scheduledLessonIds(StemPlanVersion assignment,int termLength){
  List<StemLesson> lessons=stemLessons.findAllByCourse_IdOrderByLessonOrderAsc(assignment.getCourse().getId()).stream()
          .filter(lesson->!isStemEventLesson(lesson)).toList();
  List<String> stored=stemLessonOrders.findAllByPlanItemIdOrderByPositionAsc(assignment.getId()).stream()
          .map(StemPlanLessonOrder::getLessonId).toList();
  return scheduledLessonIds(stored,lessons,termLength);
 }

 private List<String> scheduledLessonIds(UUID planItemId,List<StemLesson> lessons,int termLength){
  List<String> stored=stemLessonOrders.findAllByPlanItemIdOrderByPositionAsc(planItemId).stream()
          .map(StemPlanLessonOrder::getLessonId).toList();
  return scheduledLessonIds(stored,lessons,termLength);
 }

 private List<String> scheduledLessonIds(List<String> requestedOrder,List<StemLesson> lessons,int termLength){
  List<StemLesson> regularLessons=lessons.stream().filter(lesson->!isStemEventLesson(lesson)).toList();
  List<String> canonical=regularLessons.stream().map(StemLesson::getId).toList();
  Map<String,StemLesson> byId=regularLessons.stream().collect(java.util.stream.Collectors.toMap(StemLesson::getId,item->item));
  List<String> ordered=new ArrayList<>();
  if(requestedOrder!=null)for(String lessonId:requestedOrder)
   if(byId.containsKey(lessonId)&&!ordered.contains(lessonId))ordered.add(lessonId);
  for(String lessonId:canonical)if(!ordered.contains(lessonId))ordered.add(lessonId);
  return ordered.stream().limit(Math.min(8,termLength)).toList();
 }

 private boolean isStemEventLesson(StemLesson lesson){
  return lesson!=null&&lesson.getTitle()!=null&&lesson.getTitle().trim().toLowerCase(Locale.ROOT).equals("event tổng kết học phần");
 }

 private void validateStemTermWeekChange(Registration registration,RegistrationRequest request){
  if(registration.getStemProgram()==null||request.getStem()==null)return;
  Map<Integer,Integer> oldWeeks=normalizeTermWeeks(registration.getStemTermWeeks(),null);
  Map<Integer,Integer> newWeeks=normalizeTermWeeks(request.getStem().getTermWeeks(),oldWeeks);
  for(int term=1;term<=4;term++){
   int oldLength=oldWeeks.get(term),newLength=newWeeks.get(term);
   if(oldLength==newLength)continue;
   if(hasHistoryInStemTerm(registration,term))
    throw new ResponseStatusException(HttpStatus.CONFLICT,"Không thể đổi số tuần vì học phần này đã có báo bài hoặc tiến độ.");
   if(newLength<oldLength){
    final int changedTerm=term;
    if(stemPlans.findAllByRegistration_IdOrderByGradeAscTermAsc(registration.getId()).stream()
            .anyMatch(plan->plan.getTerm()==changedTerm&&plan.getEffectiveFromWeek()>newLength)
       ||stemPlanVersions.findAllByRegistration_IdOrderByGradeAscTermAscEffectiveFromWeekAsc(registration.getId()).stream()
            .anyMatch(plan->plan.getTerm()==changedTerm&&plan.isActive()&&plan.getEffectiveFromWeek()>newLength))
     throw new ResponseStatusException(HttpStatus.CONFLICT,"Có kế hoạch đổi khóa bắt đầu sau tuần "+newLength+"; hãy xử lý kế hoạch đó trước khi rút ngắn học phần.");
   }
   for(int later=term+1;later<=4;later++){
    final int laterTerm=later;
    boolean announcedLater=scheduleService.allWeeksForSummary(registration.getId(),"stem").stream()
            .filter(week->week.items().stream().anyMatch(item->Objects.equals(item.term(),laterTerm)))
            .anyMatch(week->announcements.findByKey(registration.getId()+":stem:"+week.weekStart()).isPresent());
    if(announcedLater)throw new ResponseStatusException(HttpStatus.CONFLICT,"Không thể đổi số tuần vì học phần "+later+" đã có báo bài; cần giữ nguyên ngày lịch sử.");
    if(hasProgressInTerm(registration,later))
     throw new ResponseStatusException(HttpStatus.CONFLICT,"Không thể đổi số tuần vì học phần "+later+" đã có tiến độ; cần giữ nguyên kế hoạch lịch sử.");
   }
  }
 }

 private boolean hasHistoryInStemTerm(Registration registration,int term){
  for(ScheduleWeekResponse week:scheduleService.allWeeksForSummary(registration.getId(),"stem")){
   List<ScheduleWeekResponse.Item> items=week.items().stream().filter(item->Objects.equals(item.term(),term)).toList();
   if(items.isEmpty())continue;
   if(announcements.findByKey(registration.getId()+":stem:"+week.weekStart()).isPresent())return true;
   for(ScheduleWeekResponse.Item item:items)if(item.planItemId()!=null&&item.lessonId()!=null
           &&progress.findByPlanTypeAndPlanItemIdAndLessonId("STEM",item.planItemId(),item.lessonId()).isPresent())return true;
  }
  return false;
 }

 private boolean hasProgressInTerm(Registration registration,int term){
  for(StemPlan plan:stemPlans.findAllByRegistration_IdOrderByGradeAscTermAsc(registration.getId()))
   if(plan.getTerm()==term&&!progress.findAllByPlanTypeAndPlanItemId("STEM",plan.getId()).isEmpty())return true;
  for(StemPlanVersion plan:stemPlanVersions.findAllByRegistration_IdOrderByGradeAscTermAscEffectiveFromWeekAsc(registration.getId()))
   if(plan.getTerm()==term&&!progress.findAllByPlanTypeAndPlanItemId("STEM",plan.getId()).isEmpty())return true;
  return false;
 }

 private int firstEditableStemWeek(Registration r,int grade,int term){
  int termLength=8;
  List<ScheduleWeekResponse> weeks=scheduleService.allWeeksForSummary(r.getId(),"stem");
  int lastProtected=0;
  for(ScheduleWeekResponse scheduledWeek:weeks){
   List<ScheduleWeekResponse.Item> gradeItems=scheduledWeek.items().stream()
           .filter(item->Objects.equals(item.term(),term)&&Objects.equals(item.grade(),grade))
           .toList();
   if(gradeItems.isEmpty())continue;
   boolean protectedWeek=announcements.findByKey(r.getId()+":stem:"+scheduledWeek.weekStart()).isPresent();
   for(ScheduleWeekResponse.Item item:gradeItems)if(item.planItemId()!=null&&item.lessonId()!=null
           &&progress.findByPlanTypeAndPlanItemIdAndLessonId("STEM",item.planItemId(),item.lessonId()).isPresent())protectedWeek=true;
   if(protectedWeek)lastProtected=Math.max(lastProtected,gradeItems.stream().mapToInt(item->item.lessonNo()==null?0:item.lessonNo()).max().orElse(0));
  }
  if(lastProtected>=termLength)throw new ResponseStatusException(HttpStatus.CONFLICT,"Cả "+termLength+" tuần của học phần đã được báo hoặc có tiến độ; không còn tuần để đổi khóa.");
  return lastProtected+1;
 }

 private List<LocalDate> scheduledStemWeeks(Registration r,int targetTerm){
  return scheduledStemWeeks(r,targetTerm,r.getStemTermWeeks());
 }

 private List<LocalDate> scheduledStemWeeks(Registration r,int targetTerm,Map<Integer,Integer> termLengths){
  Set<LocalDate> closedWeeks=new HashSet<>(closures.findAllBySchool_IdOrderByWeekStartAsc(r.getSchool().getId()).stream().map(SchoolClosure::getWeekStart).toList());
  List<Holiday> holidayList=holidays.findAll();
  List<LocalDate> result=new ArrayList<>();
  LocalDate nextTermWeek=null;
  for(int term=1;term<=targetTerm;term++){
   LocalDate configured=switch(term){case 1->r.getTerm1Start();case 2->r.getTerm2Start();case 3->r.getTerm3Start();default->r.getTerm4Start();};
   if(configured==null)continue;
   LocalDate scheduled=weekStartOf(configured);
   if(nextTermWeek!=null&&nextTermWeek.isAfter(scheduled))scheduled=nextTermWeek;
   for(int i=1;i<=termWeeks(termLengths,term);i++){
    int guard=0;
    while(isClosedStemWeek(scheduled,closedWeeks,holidayList,r.getStemStudyDay())&&guard++<52)scheduled=scheduled.plusWeeks(1);
    if(term==targetTerm)result.add(scheduled);
    scheduled=scheduled.plusWeeks(1);
   }
   nextTermWeek=scheduled;
  }
  return result;
 }

 private boolean isClosedStemWeek(LocalDate weekStart,Set<LocalDate> closedWeeks,List<Holiday> holidayList,java.time.DayOfWeek studyDay){
  if(closedWeeks.contains(weekStart))return true;
  if(studyDay==null)return false;
  for(Holiday holiday:holidayList)
   for(LocalDate day=holiday.getStartDate();!day.isAfter(holiday.getEndDate());day=day.plusDays(1))
    if(day.getDayOfWeek()==studyDay&&weekStartOf(day).equals(weekStart))return true;
  return false;
 }

 private LocalDate weekStartOf(LocalDate date){
  int day=date.getDayOfWeek().getValue();
  return day>=6?date.plusDays(8L-day):date.minusDays(day-1L);
 }

 private StemPlanVersion activeAssignmentAt(StemPlan base,List<StemPlanVersion> versions,int weekNo){
  StemPlanVersion selected=null;
  if(base!=null&&base.getEffectiveFromWeek()<=weekNo)
   selected=StemPlanVersion.builder().id(base.getId()).registration(base.getRegistration()).grade(base.getGrade()).term(base.getTerm())
           .course(base.getCourse()).effectiveFromWeek(base.getEffectiveFromWeek()).changeNote(base.getChangeNote()).active(true).build();
  for(StemPlanVersion version:versions)
   if(version.isActive()&&version.getEffectiveFromWeek()<=weekNo
           &&(selected==null||version.getEffectiveFromWeek()>=selected.getEffectiveFromWeek()))selected=version;
  return selected;
 }

 private String changeNote(String previousCourseId,String nextCourseId,int weekNo){
  if(previousCourseId==null)return "Phân công khóa "+nextCourseId+" từ tuần "+weekNo+".";
  return "Đổi khóa "+previousCourseId+" → "+nextCourseId+" từ tuần "+weekNo+". Các tuần đã báo bài hoặc có tiến độ được giữ nguyên.";
 }

 private StemPlanResponse stemResponse(StemPlan p){
  return new StemPlanResponse(p.getId(),p.getRegistration().getId(),p.getGrade(),p.getTerm(),p.getCourse().getId(),
          p.getEffectiveFromWeek()==null?1:p.getEffectiveFromWeek(),p.getChangeNote(),false,List.of());
 }
 private StemPlanResponse stemResponse(StemPlanVersion p){
  return new StemPlanResponse(p.getId(),p.getRegistration().getId(),p.getGrade(),p.getTerm(),p.getCourse().getId(),
          p.getEffectiveFromWeek(),p.getChangeNote(),true,List.of());
 }
 private void validate(RegistrationRequest q){if(q==null||q.getSchoolId()==null) throw bad("schoolId is required"); if(q.getAcademicYear()==null||!q.getAcademicYear().matches("\\d{4}-\\d{4}")) throw bad("academicYear must look like 2026-2027"); if(q.getStem()==null&&q.getDigital()==null) throw bad("At least one program must be registered"); if(q.getStem()!=null&&(q.getStem().getProgram()==null||q.getStem().getGrades()==null||q.getStem().getGrades().isEmpty())) throw bad("STEM program and grades are required"); if(q.getDigital()!=null&&(q.getDigital().getProgram()==null||q.getDigital().getGrades()==null||q.getDigital().getGrades().isEmpty())) throw bad("Digital program and grades are required"); if(q.getStem()!=null&&q.getStem().getStudyDay()!=null&&Set.of(java.time.DayOfWeek.SATURDAY,java.time.DayOfWeek.SUNDAY).contains(q.getStem().getStudyDay())) throw bad("STEM studyDay must be a weekday"); if(q.getDigital()!=null&&q.getDigital().getStudyDay()!=null&&Set.of(java.time.DayOfWeek.SATURDAY,java.time.DayOfWeek.SUNDAY).contains(q.getDigital().getStudyDay())) throw bad("Digital studyDay must be a weekday"); if(q.getStem()!=null&&q.getStem().getTermWeeks()!=null&&q.getStem().getTermWeeks().values().stream().anyMatch(value->value==null||(value!=8&&value!=9))) throw bad("Each STEM term must be 8 or 9 weeks"); School school=schools.findById(q.getSchoolId()).orElseThrow(()->notFound("School not found")); Set<Integer> allowed=switch(school.getLevel()){case PRIMARY->Set.of(1,2,3,4,5);case MIDDLE->Set.of(6,7,8,9);case HIGH->Set.of(10,11,12);}; if(q.getStem()!=null&&!allowed.containsAll(q.getStem().getGrades())) throw bad("STEM grades must match school level"); if(q.getDigital()!=null&&!allowed.containsAll(q.getDigital().getGrades())) throw bad("Digital grades must match school level"); }
 private RegistrationResponse response(Registration r){ RegistrationResponse.Stem st=r.getStemProgram()==null?null:new RegistrationResponse.Stem(r.getStemProgram(),sorted(r.getStemGrades()),starts(r),normalizeTermWeeks(r.getStemTermWeeks(),null),r.getStemStudyDay()); RegistrationResponse.Digital d=r.getDigitalProgram()==null?null:new RegistrationResponse.Digital(r.getDigitalProgram(),sorted(r.getDigitalGrades()),r.getDigitalStart(),r.getDigitalStudyDay());return new RegistrationResponse(r.getId(),r.getSchool().getId(),r.getAcademicYear(),st,d); }
 private Map<Integer,LocalDate> starts(Registration r){Map<Integer,LocalDate> m=new LinkedHashMap<>(); if(r.getTerm1Start()!=null)m.put(1,r.getTerm1Start());if(r.getTerm2Start()!=null)m.put(2,r.getTerm2Start());if(r.getTerm3Start()!=null)m.put(3,r.getTerm3Start());if(r.getTerm4Start()!=null)m.put(4,r.getTerm4Start());return m;}
 private List<Integer> sorted(Set<Integer> x){return x==null?List.of():x.stream().sorted().toList();} 
 private LessonProgressResponse progressResponse(LessonProgress x,UUID reg){return new LessonProgressResponse(x.getId(),x.getPlanItemId()+":"+x.getLessonId(),reg,x.getPlanItemId(),x.getPlanType(),x.getLessonId(),x.getStatus(),x.getDate());}
 private Registration findRegistration(UUID id){return registrations.findById(id).orElseThrow(()->notFound("Registration not found"));} private User current(){String username=SecurityContextHolder.getContext().getAuthentication().getName();return users.findByUsername(username).orElseThrow(()->notFound("User not found"));} private boolean isAdmin(User u){return u.getRoles()!=null&&(u.getRoles().contains(Role.ADMIN.name())||u.getRoles().contains(Role.ADMIN_SCHOOL.name()));}
 private Registration accessibleRegistration(User user,UUID id){Registration registration=findRegistration(id);assertAccess(user,registration.getSchool());return registration;}
 private List<Registration> visibleRegistrations(User user){return isAdmin(user)?registrations.findAll():user.getRegion()==null?List.of():registrations.findAllBySchool_Region_Id(user.getRegion().getId());}
 private void assertAccess(User u,School s){if(!isAdmin(u)&&(u.getRegion()==null||!u.getRegion().getId().equals(s.getRegion().getId())))throw new AccessDeniedException("You can only manage registrations in your assigned region");}
 private ResponseStatusException bad(String m){return new ResponseStatusException(HttpStatus.BAD_REQUEST,m);} private ResponseStatusException notFound(String m){return new ResponseStatusException(HttpStatus.NOT_FOUND,m);}
}


