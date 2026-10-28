package com.example.beprojectweb.repository;

import com.example.beprojectweb.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    boolean existsUserByUsername(String username); // check user
    boolean existsByEmail(String email);
    boolean existsByUsernameAndIdNot(String username, UUID id);
    boolean existsByEmailAndIdNot(String email, UUID id);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByVerificationCode(String verificationCode);
    @EntityGraph(attributePaths = {"region", "reportStaff"})
    List<User> findAllByRegion_Id(UUID regionId);
    @Override
    @EntityGraph(attributePaths = {"region", "reportStaff"})
    List<User> findAll();
    boolean existsByRegion_Id(UUID regionId);
    boolean existsByReportStaff_Id(UUID reportStaffId);
    boolean existsByReportStaff_IdAndIdNot(UUID reportStaffId, UUID userId);

}
