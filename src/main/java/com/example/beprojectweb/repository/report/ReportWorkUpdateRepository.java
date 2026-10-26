package com.example.beprojectweb.repository.report;

import com.example.beprojectweb.entity.report.ReportWorkUpdate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReportWorkUpdateRepository extends JpaRepository<ReportWorkUpdate, UUID> {
    List<ReportWorkUpdate> findAllByWork_IdOrderByUpdatedAtAsc(UUID workId);
}
