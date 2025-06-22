package com.smartdorm.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Entity
@Table(name = "dorm_rooms", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"building_id", "room_number"})
})
public class DormRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id", nullable = false)
    private DormBuilding building;

    @Column(name = "room_number", nullable = false, length = 20)
    private String roomNumber;

    @Column(nullable = false)
    private int capacity;

    @Column(name = "gender_type", nullable = false, length = 20)
    private String genderType; // "MALE" or "FEMALE"

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Bed> beds = new ArrayList<>();
}