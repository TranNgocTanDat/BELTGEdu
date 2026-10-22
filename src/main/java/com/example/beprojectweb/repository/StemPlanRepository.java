package com.example.beprojectweb.repository;
import com.example.beprojectweb.entity.StemPlan; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface StemPlanRepository extends JpaRepository<StemPlan,UUID> {
 List<StemPlan> findAllByRegistration_IdOrderByGradeAscTermAsc(UUID registrationId);
 List<StemPlan> findAllByRegistration_IdInOrderByGradeAscTermAsc(Collection<UUID> registrationIds);
 Optional<StemPlan> findByRegistration_IdAndGradeAndTerm(UUID registrationId,Integer grade,Integer term);
 List<StemPlan> findAllByRegistration_IdAndGrade(UUID registrationId,Integer grade);
 void deleteAllByRegistration_IdAndGrade(UUID registrationId,Integer grade);
 boolean existsByCourse_Id(String courseId);
}
