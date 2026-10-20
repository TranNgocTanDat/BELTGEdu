package com.example.beprojectweb.repository;
import com.example.beprojectweb.entity.DigitalPlan; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface DigitalPlanRepository extends JpaRepository<DigitalPlan,UUID> { List<DigitalPlan> findAllByRegistration_IdOrderByGradeAsc(UUID registrationId); Optional<DigitalPlan> findByRegistration_IdAndGrade(UUID registrationId,Integer grade); }
