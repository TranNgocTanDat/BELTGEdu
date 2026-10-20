package com.example.beprojectweb.repository;
import com.example.beprojectweb.entity.Registration; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface RegistrationRepository extends JpaRepository<Registration,UUID> { List<Registration> findAllBySchool_Region_Id(UUID regionId); List<Registration> findAllBySchool_Id(UUID schoolId); boolean existsBySchool_IdAndAcademicYear(UUID schoolId,String academicYear); boolean existsBySchool_IdAndAcademicYearAndIdNot(UUID schoolId,String academicYear,UUID id); }
