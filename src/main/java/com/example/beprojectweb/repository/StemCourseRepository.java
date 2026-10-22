package com.example.beprojectweb.repository;

import com.example.beprojectweb.entity.StemCourse;
import com.example.beprojectweb.enums.StemProgram;
import com.example.beprojectweb.enums.StemSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StemCourseRepository extends JpaRepository<StemCourse, String> {
    boolean existsById(String id);
    boolean existsByProgramAndCodeIgnoreCase(StemProgram program, String code);
    boolean existsByProgramAndCodeIgnoreCaseAndIdNot(StemProgram program, String code, String id);

    @Query("select c from StemCourse c where (:program is null or c.program = :program) "
            + "and (:subject is null or c.subject = :subject) "
            + "and (:grade is null or c.grade is null or c.grade = :grade) "
            + "order by c.program, c.subject, c.grade, c.module, c.part")
    List<StemCourse> findCatalog(@Param("program") StemProgram program,
            @Param("subject") StemSubject subject, @Param("grade") Integer grade);
}
