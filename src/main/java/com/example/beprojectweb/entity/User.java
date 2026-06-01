package com.example.beprojectweb.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Auditable;
import org.springframework.data.jpa.domain.AbstractAuditable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User{
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;
    String username;
    String password;
    String firstName;
    String lastName;
//    @Column(unique = true, nullable = false)
    String email;
    LocalDate dob;
    @Column(nullable = false)
    boolean enabled = false;
    @Column(name = "verification_code")
    String verificationCode;
    @Column(name = "verification_expiration")
    LocalDateTime verificationCodeExpiredAt;

    @ElementCollection(fetch = FetchType.EAGER)
    Set<String> roles;

}
