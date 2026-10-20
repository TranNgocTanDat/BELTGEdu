package com.example.beprojectweb.entity;
import jakarta.persistence.*; import lombok.*; import lombok.experimental.FieldDefaults; import java.util.UUID;
@Entity @Table(name="digital_plans",uniqueConstraints=@UniqueConstraint(name="uk_digital_plan_grade",columnNames={"registration_id","grade"})) @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder @FieldDefaults(level=AccessLevel.PRIVATE)
public class DigitalPlan { @Id @GeneratedValue(strategy=GenerationType.UUID) UUID id; @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="registration_id",nullable=false) Registration registration; @Column(nullable=false) Integer grade; }
