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

    // ... 其他字段保持不变 ...
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cycle_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private MatchingCycle cycle;

    @Column(name = "dimension_key", nullable = false, length = 100)
    private String dimensionKey;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String prompt;

    @Column(name = "dimension_type", nullable = false, length = 20)
    private String dimensionType;

    @Column(name = "response_type", nullable = false, length = 20)
    private String responseType;

    @Column(nullable = false)
    private double weight = 1.0;

    @Column(name = "parent_dimension_key", length = 100)
    private String parentDimensionKey;

    // [关键修改] 将字段名从 isReverseScored 改为 reverseScored
    // JPA 和 Lombok 会将其映射到数据库的 is_reverse_scored 或 reverse_scored 列
    @Column(name = "reverse_scored", nullable = false)
    private boolean reverseScored = false;

    @OneToMany(mappedBy = "dimension", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<DimensionOption> options = new ArrayList<>();
}