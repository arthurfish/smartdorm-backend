package com.smartdorm.backend.repository;

import com.smartdorm.backend.entity.DormRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DormRoomRepository extends JpaRepository<DormRoom, UUID> {
    // 用于检查楼栋下是否有房间，比加载整个列表更高效
    boolean existsByBuildingId(UUID buildingId);
}