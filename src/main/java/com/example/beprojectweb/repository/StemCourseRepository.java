package com.example.beprojectweb.repository;

import com.example.beprojectweb.entity.StemCourse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StemCourseRepository extends JpaRepository<StemCourse, String> {
    boolean existsById(String id);
}
