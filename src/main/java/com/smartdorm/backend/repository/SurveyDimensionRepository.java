package com.smartdorm.backend.repository;

import com.smartdorm.backend.entity.SurveyDimension;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SurveyDimensionRepository extends JpaRepository<SurveyDimension, UUID> {
    List<SurveyDimension> findByCycleId(UUID cycleId);
}