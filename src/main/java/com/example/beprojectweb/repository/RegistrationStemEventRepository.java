package com.example.beprojectweb.repository;

import com.example.beprojectweb.entity.RegistrationStemEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RegistrationStemEventRepository extends JpaRepository<RegistrationStemEvent, UUID> {
    List<RegistrationStemEvent> findAllByRegistration_IdOrderByTermAscEventDateAsc(UUID registrationId);
    List<RegistrationStemEvent> findAllByRegistration_IdInOrderByRegistration_IdAscTermAscEventDateAsc(Collection<UUID> registrationIds);
    List<RegistrationStemEvent> findAllByRegistration_IdAndTermOrderByEventDateAsc(UUID registrationId, Integer term);
    Optional<RegistrationStemEvent> findByRegistration_IdAndTermAndEvent_Id(UUID registrationId, Integer term, String eventId);
    void deleteAllByRegistration_Id(UUID registrationId);
}
