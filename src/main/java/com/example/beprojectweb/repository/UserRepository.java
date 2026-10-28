package com.example.beprojectweb.repository;

import com.example.beprojectweb.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;
import java.util.UUID;
import java.util.Collection;

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

    @EntityGraph(attributePaths = {"reportStaff"})
    java.util.Optional<User> findByReportStaff_IdAndEnabledTrue(UUID reportStaffId);

    @Query("select distinct u from User u join u.roles userRole where u.enabled = true and u.email is not null and u.email <> '' and userRole in :roles")
    List<User> findAllEnabledWithAnyRoleAndEmail(@Param("roles") Collection<String> roles);

}
