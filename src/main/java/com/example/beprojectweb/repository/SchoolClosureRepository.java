package com.example.beprojectweb.repository;

import com.example.beprojectweb.entity.SchoolClosure;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SchoolClosureRepository extends JpaRepository<SchoolClosure, UUID> {
    List<SchoolClosure> findAllByOrderByWeekStartAsc();
    List<SchoolClosure> findAllBySchool_IdOrderByWeekStartAsc(UUID schoolId);
    List<SchoolClosure> findAllBySchool_Region_IdOrderByWeekStartAsc(UUID regionId);
    Optional<SchoolClosure> findBySchool_IdAndWeekStart(UUID schoolId, java.time.LocalDate weekStart);
    void deleteAllBySchool_Id(UUID schoolId);
}
