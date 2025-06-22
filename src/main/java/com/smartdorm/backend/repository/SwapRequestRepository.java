package com.smartdorm.backend.repository;

import com.smartdorm.backend.entity.SwapRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SwapRequestRepository extends JpaRepository<SwapRequest, UUID> {
}