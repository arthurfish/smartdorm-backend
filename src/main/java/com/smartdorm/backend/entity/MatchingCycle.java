package com.smartdorm.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Entity
@Table(name = "matching_cycles")
public class MatchingCycle {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String name;

    private Instant startDate;
    private Instant endDate;

    @Column(nullable = false, length = 20)
    private String status = "DRAFT"; // DRAFT, OPEN, PROCESSING, COMPLETED

    @CreationTimestamp
    private Instant createdAt;

    @OneToMany(mappedBy = "cycle", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<SurveyDimension> dimensions = new ArrayList<>();
}