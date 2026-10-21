package com.example.beprojectweb.repository;

import com.example.beprojectweb.entity.StemCourse;
import com.example.beprojectweb.enums.StemProgram;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StemCourseRepository extends JpaRepository<StemCourse, String> {
    boolean existsById(String id);
    boolean existsByProgramAndCodeIgnoreCase(StemProgram program, String code);
    boolean existsByProgramAndCodeIgnoreCaseAndIdNot(StemProgram program, String code, String id);
}
