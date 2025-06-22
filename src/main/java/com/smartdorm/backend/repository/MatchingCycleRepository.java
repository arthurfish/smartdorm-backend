package com.smartdorm.backend.repository;

import com.smartdorm.backend.entity.MatchingCycle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface MatchingCycleRepository extends JpaRepository<MatchingCycle, UUID> {
}