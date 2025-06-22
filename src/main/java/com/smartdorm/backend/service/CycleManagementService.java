package com.smartdorm.backend.service;

import com.smartdorm.backend.dto.CycleDtos.*;
import com.smartdorm.backend.entity.DimensionOption;
import com.smartdorm.backend.entity.MatchingCycle;
import com.smartdorm.backend.entity.SurveyDimension;
import com.smartdorm.backend.exception.DataConflictException;
import com.smartdorm.backend.exception.ResourceNotFoundException;
import com.smartdorm.backend.mapper.CycleMapper;
import com.smartdorm.backend.repository.MatchingCycleRepository;
import com.smartdorm.backend.repository.SurveyDimensionRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class CycleManagementService {

    private final MatchingCycleRepository cycleRepository;
    private final SurveyDimensionRepository dimensionRepository;
    private final CycleMapper cycleMapper;

    public CycleManagementService(MatchingCycleRepository cycleRepository, SurveyDimensionRepository dimensionRepository, CycleMapper cycleMapper) {
        this.cycleRepository = cycleRepository;
        this.dimensionRepository = dimensionRepository;
        this.cycleMapper = cycleMapper;
    }

    // --- Cycle Methods ---

    public MatchingCycleDto createCycle(MatchingCycleCreateDto dto) {
        MatchingCycle cycle = new MatchingCycle();
        cycle.setName(dto.name());
        cycle.setStartDate(dto.startDate());
        cycle.setEndDate(dto.endDate());
        return cycleMapper.toDto(cycleRepository.save(cycle));
    }

    public List<MatchingCycleDto> getAllCycles() {
        return cycleRepository.findAll().stream().map(cycleMapper::toDto).collect(Collectors.toList());
    }

    public MatchingCycleDto getCycleById(UUID cycleId) {
        return cycleRepository.findById(cycleId)
                .map(cycleMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Cycle not found with id: " + cycleId));
    }

    public MatchingCycleDto updateCycle(UUID cycleId, MatchingCycleUpdateDto dto) {
        MatchingCycle cycle = cycleRepository.findById(cycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Cycle not found with id: " + cycleId));
        if(dto.name() != null) cycle.setName(dto.name());
        if(dto.startDate() != null) cycle.setStartDate(dto.startDate());
        if(dto.endDate() != null) cycle.setEndDate(dto.endDate());
        if(dto.status() != null) cycle.setStatus(dto.status());
        return cycleMapper.toDto(cycleRepository.save(cycle));
    }

    public void deleteCycle(UUID cycleId) {
        MatchingCycle cycle = cycleRepository.findById(cycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Cycle not found with id: " + cycleId));
        if (!"DRAFT".equals(cycle.getStatus())) {
            throw new DataConflictException("Cannot delete cycle in status '" + cycle.getStatus() + "'. Only 'DRAFT' cycles can be deleted.");
        }
        cycleRepository.delete(cycle);
    }

    // --- Dimension Methods ---

    public SurveyDimensionDto createDimensionForCycle(UUID cycleId, SurveyDimensionCreateDto dto) {
        MatchingCycle cycle = cycleRepository.findById(cycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Cycle not found with id: " + cycleId));

        SurveyDimension dimension = new SurveyDimension();
        dimension.setCycle(cycle);
        dimension.setDimensionKey(dto.dimensionKey());
        dimension.setPrompt(dto.prompt());
        dimension.setDimensionType(dto.dimensionType());
        dimension.setResponseType(dto.responseType());
        dimension.setWeight(dto.weight());
        dimension.setParentDimensionKey(dto.parentDimensionKey());
        dimension.setReverseScored(dto.isReverseScored());

        if (dto.options() != null) {
            List<DimensionOption> options = dto.options().stream().map(optDto -> {
                DimensionOption option = new DimensionOption();
                option.setOptionText(optDto.optionText());
                option.setOptionValue(optDto.optionValue());
                option.setDimension(dimension); // Link back to parent
                return option;
            }).collect(Collectors.toList());
            dimension.getOptions().addAll(options);
        }

        return cycleMapper.toDto(dimensionRepository.save(dimension));
    }

    public List<SurveyDimensionDto> getDimensionsForCycle(UUID cycleId) {
        if (!cycleRepository.existsById(cycleId)) {
            throw new ResourceNotFoundException("Cycle not found with id: " + cycleId);
        }
        return dimensionRepository.findByCycleId(cycleId).stream()
                .map(cycleMapper::toDto)
                .collect(Collectors.toList());
    }

    public SurveyDimensionDto updateDimension(UUID dimensionId, SurveyDimensionUpdateDto dto) {
        SurveyDimension dimension = dimensionRepository.findById(dimensionId)
                .orElseThrow(() -> new ResourceNotFoundException("Dimension not found with id: " + dimensionId));

        dimension.setPrompt(dto.prompt());
        dimension.setWeight(dto.weight());
        dimension.setReverseScored(dto.isReverseScored());

        return cycleMapper.toDto(dimensionRepository.save(dimension));
    }

    public void deleteDimension(UUID dimensionId) {
        if (!dimensionRepository.existsById(dimensionId)) {
            throw new ResourceNotFoundException("Dimension not found with id: " + dimensionId);
        }
        dimensionRepository.deleteById(dimensionId);
    }
}