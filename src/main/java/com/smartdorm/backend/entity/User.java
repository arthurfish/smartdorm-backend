package com.smartdorm.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Data
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false, length = 50)
    private String studentId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "hashed_password", nullable = false)
    private String password;

    @Column(nullable = false, length = 20)
    private String role; // "STUDENT" or "ADMIN"

    @Column(nullable = false, length = 10)
    private String gender; // "MALE" or "FEMALE"

    @Column(nullable = false, length = 100)
    private String college;

    @Column(nullable = false)
    private boolean isSpecialNeeds = false;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}