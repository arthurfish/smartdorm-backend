package com.smartdorm.backend.service;

import com.smartdorm.backend.dto.CycleDtos.*;
import com.smartdorm.backend.entity.DimensionOption;
import com.smartdorm.backend.entity.MatchingCycle;
import com.smartdorm.backend.entity.SurveyDimension;
import com.smartdorm.backend.entity.UserResponse;
import com.smartdorm.backend.exception.DataConflictException;
import com.smartdorm.backend.exception.ResourceNotFoundException;
import com.smartdorm.backend.mapper.CycleMapper;
import com.smartdorm.backend.repository.MatchingCycleRepository;
import com.smartdorm.backend.repository.SurveyDimensionRepository;
import com.smartdorm.backend.repository.UserResponseRepository;
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
    private final UserResponseRepository userResponseRepository;

    public CycleManagementService(MatchingCycleRepository cycleRepository,
                                  SurveyDimensionRepository dimensionRepository,
                                  UserResponseRepository userResponseRepository,
                                  CycleMapper cycleMapper) {
        this.cycleRepository = cycleRepository;
        this.dimensionRepository = dimensionRepository;
        this.userResponseRepository = userResponseRepository;
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
        dimension.setDimensionKey(dto.getDimensionKey());
        dimension.setPrompt(dto.getPrompt());
        dimension.setDimensionType(dto.getDimensionType());
        dimension.setResponseType(dto.getResponseType());
        dimension.setWeight(dto.getWeight());
        dimension.setParentDimensionKey(dto.getParentDimensionKey());
        dimension.setReverseScored(dto.isReverseScored());

        if (dto.getOptions() != null) {
            List<DimensionOption> options = dto.getOptions().stream().map(optDto -> {
                DimensionOption option = new DimensionOption();
                // [修复] 使用 .getOptionText() 和 .getOptionValue()
                option.setOptionText(optDto.getOptionText());
                option.setOptionValue(optDto.getOptionValue());
                option.setDimension(dimension);
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
        // [关键修改] record 的访问器就是字段名
        dimension.setReverseScored(dto.reverseScored());

        return cycleMapper.toDto(dimensionRepository.save(dimension));
    }

    public void deleteDimension(UUID dimensionId) {
        // 1. 验证维度是否存在，如果不存在，后续操作无意义
        if (!dimensionRepository.existsById(dimensionId)) {
            throw new ResourceNotFoundException("Dimension not found with id: " + dimensionId);
        }

        // 2. 查找并删除所有相关的 UserResponse 记录
        List<UserResponse> responsesToDelete = userResponseRepository.findByDimensionId(dimensionId);
        if (!responsesToDelete.isEmpty()) {
            userResponseRepository.deleteAllInBatch(responsesToDelete);
        }

        // 3. 现在可以安全地删除 SurveyDimension
        // SurveyDimension 实体与 DimensionOption 是一对多关系，并且设置了 cascade=ALL, orphanRemoval=true
        // 这意味着当我们删除 SurveyDimension 时，JPA 会自动删除其关联的所有 DimensionOption。
        // 所以我们不需要手动删除 options。
        dimensionRepository.deleteById(dimensionId);
    }
}