package com.example.beprojectweb.repository;

import com.example.beprojectweb.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface HolidayRepository extends JpaRepository<Holiday, UUID> {
    List<Holiday> findAllByEndDateGreaterThanEqualAndStartDateLessThanEqualOrderByStartDateAsc(
            LocalDate from, LocalDate to);
}
