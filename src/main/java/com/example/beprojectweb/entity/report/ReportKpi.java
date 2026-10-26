package com.example.beprojectweb.entity.report;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Entity
@Table(name = "report_kpis", uniqueConstraints = @UniqueConstraint(name = "uk_report_kpi_period_key", columnNames = {"period", "kpi_key"}))
@Getter
@Setter
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReportKpi {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(nullable = false, length = 8)
    String period;

    @Column(name = "kpi_key", nullable = false, length = 30)
    String kpiKey;

    @Column(nullable = false)
    Integer target;

    @Column(nullable = false)
    Integer actual;
}
