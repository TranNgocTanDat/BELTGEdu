package com.example.beprojectweb.repository.report;

import com.example.beprojectweb.entity.report.ReportWorkExtension;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReportWorkExtensionRepository extends JpaRepository<ReportWorkExtension, UUID> {
    List<ReportWorkExtension> findAllByWork_IdOrderByCreatedAtAsc(UUID workId);
}
