package com.example.beprojectweb.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "stem_event_templates", uniqueConstraints = @UniqueConstraint(
        name = "uk_stem_event_template_name", columnNames = "name"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StemEventTemplate {
    @Id
    @Column(length = 100)
    String id;

    @Column(nullable = false, length = 200)
    String name;

    @Column(name = "announcement_intro", length = 2000)
    String announcementIntro;

    @Column(name = "announcement_greeting", length = 3000)
    String announcementGreeting;
}
