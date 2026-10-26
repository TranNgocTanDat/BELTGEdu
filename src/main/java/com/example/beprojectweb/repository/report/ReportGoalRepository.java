package com.example.beprojectweb.repository.report;

import com.example.beprojectweb.entity.report.ReportGoal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReportGoalRepository extends JpaRepository<ReportGoal, UUID> {
    List<ReportGoal> findAllByOrderByPeriodDescTitleAsc();
}
