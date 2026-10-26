package com.example.beprojectweb.repository.report;

import com.example.beprojectweb.entity.report.ReportWork;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReportWorkRepository extends JpaRepository<ReportWork, UUID> {
    @EntityGraph(attributePaths = {"owner", "workGroup"})
    List<ReportWork> findAllByOrderByPeriodDescTitleAsc();
    boolean existsByOwner_Id(UUID ownerId);
    boolean existsByWorkGroup_Id(UUID groupId);
}
