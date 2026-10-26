package com.example.beprojectweb.repository.report;

import com.example.beprojectweb.entity.report.ReportWorkGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReportWorkGroupRepository extends JpaRepository<ReportWorkGroup, UUID> {
    List<ReportWorkGroup> findAllByOrderByNameAsc();
    java.util.Optional<ReportWorkGroup> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);
}
