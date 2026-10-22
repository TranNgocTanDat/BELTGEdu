package com.example.beprojectweb.repository;

import com.example.beprojectweb.entity.DigitalLesson;
import com.example.beprojectweb.enums.DigitalProgram;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Collection;

public interface DigitalLessonRepository extends JpaRepository<DigitalLesson, String> {
    List<DigitalLesson> findAllByOrderByProgramAscGradeAscLessonOrderAsc();
    List<DigitalLesson> findAllByProgramOrderByGradeAscLessonOrderAsc(DigitalProgram program);
    List<DigitalLesson> findAllByProgramAndGradeOrderByLessonOrderAsc(DigitalProgram program, Integer grade);
    List<DigitalLesson> findAllByProgramAndGradeInOrderByGradeAscLessonOrderAsc(DigitalProgram program, Collection<Integer> grades);
    boolean existsByIdAndProgramAndGrade(String id, DigitalProgram program, Integer grade);
    boolean existsByProgramAndGradeAndLessonOrder(DigitalProgram program, Integer grade, Integer lessonOrder);
    boolean existsByProgramAndGradeAndLessonOrderAndIdNot(DigitalProgram program, Integer grade, Integer lessonOrder, String id);
}

