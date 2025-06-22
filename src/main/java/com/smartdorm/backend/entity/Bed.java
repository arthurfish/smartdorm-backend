package com.smartdorm.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.UUID;

@Data
@Entity
@Table(name = "beds", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"room_id", "bed_number"})
})
public class Bed {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private DormRoom room;

    @Column(name = "bed_number", nullable = false)
    private int bedNumber;
}