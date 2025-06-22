package com.smartdorm.backend.repository;

import com.smartdorm.backend.entity.UserResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserResponseRepository extends JpaRepository<UserResponse, UUID> {
    Optional<UserResponse> findByUserIdAndDimensionId(UUID userId, UUID dimensionId);

    List<UserResponse> findByUserId(UUID userId);
    List<UserResponse> findByDimensionId(UUID userId);

}