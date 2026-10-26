package com.example.beprojectweb.repository.report;

import com.example.beprojectweb.entity.report.ReportKpi;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReportKpiRepository extends JpaRepository<ReportKpi, UUID> {
    List<ReportKpi> findAllByOrderByPeriodDescKpiKeyAsc();
    boolean existsByPeriodAndKpiKey(String period, String kpiKey);
    boolean existsByPeriodAndKpiKeyAndIdNot(String period, String kpiKey, UUID id);
}
