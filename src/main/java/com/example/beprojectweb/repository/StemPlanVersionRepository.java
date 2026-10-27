package com.example.beprojectweb.repository;

import com.example.beprojectweb.entity.StemPlanVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface StemPlanVersionRepository extends JpaRepository<StemPlanVersion, UUID> {
    List<StemPlanVersion> findAllByRegistration_IdAndGradeAndTermOrderByEffectiveFromWeekAsc(UUID registrationId, Integer grade, Integer term);
    List<StemPlanVersion> findAllByRegistration_IdInAndActiveTrueOrderByGradeAscTermAscEffectiveFromWeekAsc(Collection<UUID> registrationIds);
    List<StemPlanVersion> findAllByRegistration_IdOrderByGradeAscTermAscEffectiveFromWeekAsc(UUID registrationId);
    List<StemPlanVersion> findAllByRegistration_IdAndGrade(UUID registrationId, Integer grade);
    boolean existsByCourse_Id(String courseId);
}
