package com.smartdorm.backend.service;

import com.smartdorm.backend.entity.MatchingCycle;
import com.smartdorm.backend.exception.DataConflictException;
import com.smartdorm.backend.exception.ResourceNotFoundException;
import com.smartdorm.backend.repository.MatchingCycleRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
@Transactional
public class AdminAssignmentService {

    private final MatchingCycleRepository cycleRepository;

    public AdminAssignmentService(MatchingCycleRepository cycleRepository) {
        this.cycleRepository = cycleRepository;
    }

    /**
     * Placeholder for the real assignment logic.
     * Currently, it just updates the cycle status.
     */
    public void triggerAssignment(UUID cycleId) {
        MatchingCycle cycle = cycleRepository.findById(cycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Cycle not found with id: " + cycleId));

        if (!"OPEN".equals(cycle.getStatus())) {
            throw new DataConflictException("Cannot trigger assignment for a cycle that is not in 'OPEN' status.");
        }

        // In a real scenario, you'd trigger an async job here.
        // For now, we simulate the completion of the process.
        cycle.setStatus("COMPLETED");
        cycleRepository.save(cycle);

        // Here you would generate notifications, results etc.
    }
}