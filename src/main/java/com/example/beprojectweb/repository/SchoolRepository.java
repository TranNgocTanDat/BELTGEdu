package com.example.beprojectweb.repository;

import com.example.beprojectweb.entity.School;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SchoolRepository extends JpaRepository<School, UUID> {
    List<School> findAllByRegion_Id(UUID regionId);
    boolean existsByRegion_Id(UUID regionId);
    boolean existsByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCaseAndIdNot(String code, UUID id);
}
