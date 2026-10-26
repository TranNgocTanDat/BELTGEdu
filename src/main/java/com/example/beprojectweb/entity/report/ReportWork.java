package com.example.beprojectweb.entity.report;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;
import org.hibernate.annotations.BatchSize;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Entity
@Table(name = "report_work_items")
@Getter
@Setter
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReportWork {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(nullable = false, length = 8)
    String period;

    @Column(name = "work_group", nullable = false, length = 80)
    String group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    ReportWorkGroup workGroup;

    @Column(nullable = false, length = 500)
    String title;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    ReportStaff owner;

    @Column(nullable = false)
    Integer progress;

    @Column(nullable = false, length = 30)
    String status;

    @Column(nullable = false, length = 2000)
    String impact = "";

    @Column(name = "start_date")
    LocalDate startDate;

    @Column(name = "original_due_date")
    LocalDate originalDueDate;

    @Column(name = "current_due_date")
    LocalDate currentDueDate;

    @Column(name = "completed_date")
    LocalDate completedDate;

    @Column(name = "created_at")
    LocalDateTime createdAt;

    @OneToMany(mappedBy = "work", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 64)
    List<ReportWorkUpdate> updates = new ArrayList<>();

    @OneToMany(mappedBy = "work", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 64)
    List<ReportWorkExtension> extensions = new ArrayList<>();
}
