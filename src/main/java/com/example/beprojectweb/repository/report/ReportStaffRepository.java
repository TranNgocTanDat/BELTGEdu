package com.example.beprojectweb.repository.report;

import com.example.beprojectweb.entity.report.ReportStaff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReportStaffRepository extends JpaRepository<ReportStaff, UUID> {
    List<ReportStaff> findAllByOrderByNameAsc();
}
