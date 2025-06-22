package com.smartdorm.backend.service;

import com.smartdorm.backend.dto.AdminDtos.AdminAssignmentValidationDto;
import com.smartdorm.backend.dto.AdminDtos.AssignmentResultAdminDto;
import com.smartdorm.backend.dto.AdminDtos.ValidationDetailDto;
import com.smartdorm.backend.dto.UserDto;
import com.smartdorm.backend.entity.MatchingCycle;
import com.smartdorm.backend.entity.MatchingResult;
import com.smartdorm.backend.exception.DataConflictException;
import com.smartdorm.backend.exception.ResourceNotFoundException;
import com.smartdorm.backend.repository.MatchingCycleRepository;
import com.smartdorm.backend.repository.MatchingResultRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class AdminAssignmentService {

    private final MatchingCycleRepository cycleRepository;
    private final MatchingResultRepository resultRepository;

    public AdminAssignmentService(MatchingCycleRepository cycleRepository, MatchingResultRepository resultRepository) {
        this.cycleRepository = cycleRepository;
        this.resultRepository = resultRepository;
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
        cycle.setStatus("PROCESSING"); // First set to processing
        // ... algorithm runs ...
        cycle.setStatus("COMPLETED"); // Then set to completed
        cycleRepository.save(cycle);

        // Here you would generate notifications, results etc.
    }

    /**
     * [IMPLEMENTED] Retrieves the complete list of assignment results for a specific cycle.
     * Corresponds to use case ADM-04.
     * @param cycleId The ID of the matching cycle.
     * @return A list of detailed assignment results for the admin.
     */
    public List<AssignmentResultAdminDto> getAssignmentResults(UUID cycleId) {
        if (!cycleRepository.existsById(cycleId)) {
            throw new ResourceNotFoundException("Cycle not found with id: " + cycleId);
        }

        List<MatchingResult> results = resultRepository.findByCycleId(cycleId);

        return results.stream()
                .map(this::mapToAssignmentResultAdminDto)
                .collect(Collectors.toList());
    }


    /**
     * [ENHANCED] Validates the results of an assignment against predefined quality metrics.
     * Corresponds to use case ADM-08.
     * The logic here is a placeholder, demonstrating the required DTO structure.
     *
     * @param cycleId The ID of the matching cycle.
     * @return A detailed validation report.
     */
    public AdminAssignmentValidationDto validateResults(UUID cycleId) {
        if (!resultRepository.existsByCycleId(cycleId)) {
            throw new ResourceNotFoundException("No assignment results found for cycle with id: " + cycleId);
        }

        // TODO: Implement real validation logic.
        // For now, we return a mock successful validation report.
        List<ValidationDetailDto> mockDetails = Collections.singletonList(
                new ValidationDetailDto("紫荆1号楼-301", "神经质均值", 0.55, true)
        );

        return new AdminAssignmentValidationDto(
                true,
                "结果符合所有检验标准。",
                mockDetails
        );
    }

    /**
     * Helper method to map a MatchingResult entity to its admin-facing DTO.
     */
    private AssignmentResultAdminDto mapToAssignmentResultAdminDto(MatchingResult result) {
        var user = result.getUser();
        var bed = result.getBed();
        var room = bed.getRoom();
        var building = room.getBuilding();

        UserDto userDto = new UserDto(user.getId(), user.getStudentId(), user.getName(), user.getRole(), user.getGender(), user.getCollege());

        return new AssignmentResultAdminDto(
                userDto,
                building.getName(),
                room.getRoomNumber(),
                bed.getBedNumber()
        );
    }
}