package com.example.beprojectweb.repository;

import com.example.beprojectweb.entity.StemEventGradeLesson;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StemEventGradeLessonRepository extends JpaRepository<StemEventGradeLesson, String> {
    List<StemEventGradeLesson> findAllByEvent_IdInOrderByEvent_IdAscGradeAsc(Collection<String> eventIds);
    List<StemEventGradeLesson> findAllByEvent_IdOrderByGradeAsc(String eventId);
    Optional<StemEventGradeLesson> findByEvent_IdAndGrade(String eventId, Integer grade);
}
