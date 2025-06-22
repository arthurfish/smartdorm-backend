package com.smartdorm.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Entity
@Table(name = "survey_dimensions", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"cycle_id", "dimension_key"})
})
public class SurveyDimension {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cycle_id", nullable = false)
    @ToString.Exclude // Avoid circular dependency in toString
    @EqualsAndHashCode.Exclude // Avoid circular dependency in equals/hashCode
    private MatchingCycle cycle;

    @Column(name = "dimension_key", nullable = false, length = 100)
    private String dimensionKey;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String prompt;

    @Column(name = "dimension_type", nullable = false, length = 20)
    private String dimensionType; // HARD_FILTER, SOFT_FACTOR

    @Column(name = "response_type", nullable = false, length = 20)
    private String responseType; // SCALE, SINGLE_CHOICE, COMPOSITE

    @Column(nullable = false)
    private double weight = 1.0;

    @Column(name = "parent_dimension_key", length = 100)
    private String parentDimensionKey;

    @Column(name = "is_reverse_scored", nullable = false)
    private boolean isReverseScored = false;

    @OneToMany(mappedBy = "dimension", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<DimensionOption> options = new ArrayList<>();
}