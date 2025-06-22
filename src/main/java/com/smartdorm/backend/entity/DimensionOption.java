package com.smartdorm.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import java.util.UUID;

@Data
@Entity
@Table(name = "dimension_options")
public class DimensionOption {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dimension_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private SurveyDimension dimension;

    @Column(name = "option_text", nullable = false, length = 255)
    private String optionText;

    @Column(name = "option_value", nullable = false)
    private double optionValue;
}