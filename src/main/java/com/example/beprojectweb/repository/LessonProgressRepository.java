package com.example.beprojectweb.repository;
import com.example.beprojectweb.entity.LessonProgress; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface LessonProgressRepository extends JpaRepository<LessonProgress,UUID> {
 List<LessonProgress> findAllByPlanTypeAndPlanItemId(String planType,UUID planItemId);
 List<LessonProgress> findAllByPlanItemIdIn(Collection<UUID> planItemIds);
 Optional<LessonProgress> findByPlanTypeAndPlanItemIdAndLessonId(String planType,UUID planItemId,String lessonId);
 boolean existsByPlanTypeAndPlanItemIdAndLessonId(String planType,UUID planItemId,String lessonId);
 void deleteByPlanTypeAndPlanItemIdAndLessonId(String planType,UUID planItemId,String lessonId);
 void deleteAllByPlanTypeAndPlanItemId(String planType,UUID planItemId);
 boolean existsByPlanTypeAndLessonId(String planType,String lessonId);
}
