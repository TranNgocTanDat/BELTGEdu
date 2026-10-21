package com.example.beprojectweb.repository;

import com.example.beprojectweb.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

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
    List<User> findAllByRegion_Id(UUID regionId);
    boolean existsByRegion_Id(UUID regionId);

}
