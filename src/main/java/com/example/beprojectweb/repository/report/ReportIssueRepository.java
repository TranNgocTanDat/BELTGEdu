package com.example.beprojectweb.repository.report;

import com.example.beprojectweb.entity.report.ReportIssue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReportIssueRepository extends JpaRepository<ReportIssue, UUID> {
    List<ReportIssue> findAllByOrderByLevelAscTitleAsc();
}
