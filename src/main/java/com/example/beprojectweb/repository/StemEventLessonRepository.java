package com.example.beprojectweb.repository;

import com.example.beprojectweb.entity.StemEventLesson;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StemEventLessonRepository extends JpaRepository<StemEventLesson, String> {
    List<StemEventLesson> findAllByOrderByGradeAsc();
    Optional<StemEventLesson> findByGrade(Integer grade);
    boolean existsByGrade(Integer grade);
    boolean existsByIdAndGrade(String id, Integer grade);
}
