package com.smartdorm.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.UUID;

@Data
@Entity
@Table(name = "matching_results")
public class MatchingResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cycle_id", nullable = false)
    private MatchingCycle cycle;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @OneToOne
    @JoinColumn(name = "bed_id", nullable = false, unique = true)
    private Bed bed;

    @Column(name = "match_group_id", nullable = false)
    private UUID matchGroupId; // Used to group roommates
}