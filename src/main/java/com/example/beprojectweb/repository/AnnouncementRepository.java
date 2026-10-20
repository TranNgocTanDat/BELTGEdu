package com.example.beprojectweb.repository;

import com.example.beprojectweb.entity.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AnnouncementRepository extends JpaRepository<Announcement, UUID> {
    List<Announcement> findAllByOrderByWeekStartAsc();
    List<Announcement> findAllBySchool_IdOrderByWeekStartAsc(UUID schoolId);
    List<Announcement> findAllBySchool_Region_IdOrderByWeekStartAsc(UUID regionId);
    Optional<Announcement> findByKey(String key);
    void deleteAllBySchool_Id(UUID schoolId);
    void deleteAllByRegistrationId(String registrationId);
}

