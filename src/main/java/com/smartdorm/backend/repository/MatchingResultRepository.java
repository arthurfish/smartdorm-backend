package com.smartdorm.backend.repository;

import com.smartdorm.backend.entity.MatchingResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MatchingResultRepository extends JpaRepository<MatchingResult, UUID> {
    Optional<MatchingResult> findByUserId(UUID userId);

    List<MatchingResult> findByBed_Room_Id(UUID roomId);
}