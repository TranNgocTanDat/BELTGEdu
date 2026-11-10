package com.example.beprojectweb.repository;

import com.example.beprojectweb.entity.StemEventTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StemEventTemplateRepository extends JpaRepository<StemEventTemplate, String> {
    List<StemEventTemplate> findAllByOrderByNameAsc();
    boolean existsByNameIgnoreCaseAndIdNot(String name, String id);
}
