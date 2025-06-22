package com.smartdorm.backend.service;

import com.smartdorm.backend.dto.StudentDtos.*;
import com.smartdorm.backend.entity.*;
import com.smartdorm.backend.exception.ResourceNotFoundException;
import com.smartdorm.backend.mapper.CycleMapper;
import com.smartdorm.backend.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class StudentService {

    private final MatchingCycleRepository cycleRepository;
    private final SurveyDimensionRepository dimensionRepository;
    private final UserResponseRepository responseRepository;
    private final MatchingResultRepository resultRepository;
    private final CycleMapper cycleMapper;

    public StudentService(MatchingCycleRepository cycleRepository, SurveyDimensionRepository dimensionRepository, UserResponseRepository responseRepository, MatchingResultRepository resultRepository, CycleMapper cycleMapper) {
        this.cycleRepository = cycleRepository;
        this.dimensionRepository = dimensionRepository;
        this.responseRepository = responseRepository;
        this.resultRepository = resultRepository;
        this.cycleMapper = cycleMapper;
    }

    public SurveyForStudentDto getSurveyForStudent() {
        MatchingCycle openCycle = cycleRepository.findAll().stream()
                .filter(c -> "OPEN".equals(c.getStatus()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No open survey is available at the moment."));

        List<SurveyDimension> dimensions = dimensionRepository.findByCycleId(openCycle.getId());
        return new SurveyForStudentDto(
                openCycle.getId(),
                dimensions.stream().map(cycleMapper::toDto).collect(Collectors.toList())
        );
    }

    public void submitResponses(UserResponseSubmitDto dto, User currentUser) {
        List<UserResponse> responsesToSave = new ArrayList<>();
        for (ResponseItem item : dto.responses()) {
            UserResponse response = responseRepository.findByUserIdAndDimensionId(currentUser.getId(), item.dimensionId())
                    .orElse(new UserResponse());

            // Check if dimension exists (optional but good practice)
            SurveyDimension dimension = dimensionRepository.findById(item.dimensionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Dimension not found with id: " + item.dimensionId()));

            response.setUser(currentUser);
            response.setDimension(dimension);
            response.setRawValue(item.rawValue());
            responsesToSave.add(response);
        }
        responseRepository.saveAll(responsesToSave);
    }

    public AssignmentResultStudentDto getStudentResult(User currentUser) {
        MatchingResult result = resultRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Your assignment result is not available yet."));

        Bed bed = result.getBed();
        DormRoom room = bed.getRoom();
        DormBuilding building = room.getBuilding();

        AssignmentDetails assignmentDetails = new AssignmentDetails(building.getName(), room.getRoomNumber(), bed.getBedNumber());

        // Find roommates in the same room, excluding the current user
        List<RoommateDto> roommates = resultRepository.findByBed_Room_Id(room.getId()).stream()
                .map(MatchingResult::getUser)
                .filter(user -> !user.getId().equals(currentUser.getId()))
                .map(user -> new RoommateDto(user.getName(), user.getStudentId()))
                .collect(Collectors.toList());

        return new AssignmentResultStudentDto(assignmentDetails, roommates);
    }
}