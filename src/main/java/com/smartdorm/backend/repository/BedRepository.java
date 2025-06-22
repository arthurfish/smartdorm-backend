package com.smartdorm.backend.repository;

import com.smartdorm.backend.entity.Bed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BedRepository extends JpaRepository<Bed, UUID> {
    // 用于检查房间下是否有床位
    boolean existsByRoomId(UUID roomId);
}