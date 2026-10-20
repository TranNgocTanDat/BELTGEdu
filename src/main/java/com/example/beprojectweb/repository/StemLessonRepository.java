package com.example.beprojectweb.repository;

import com.example.beprojectweb.entity.StemLesson;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StemLessonRepository extends JpaRepository<StemLesson, String> {
    List<StemLesson> findAllByOrderByCourse_IdAscLessonOrderAsc();
    boolean existsByIdAndCourse_Id(String id, String courseId);
    boolean existsByCourse_IdAndLessonOrder(String courseId, Integer lessonOrder);
    List<StemLesson> findAllByCourse_IdOrderByLessonOrderAsc(String courseId);
}


