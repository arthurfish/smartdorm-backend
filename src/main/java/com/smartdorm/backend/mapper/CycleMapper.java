package com.smartdorm.backend.mapper;

import com.smartdorm.backend.dto.CycleDtos.*;
import com.smartdorm.backend.entity.DimensionOption;
import com.smartdorm.backend.entity.MatchingCycle;
import com.smartdorm.backend.entity.SurveyDimension;
import org.springframework.stereotype.Component;
import java.util.stream.Collectors;

@Component
public class CycleMapper {

    public MatchingCycleDto toDto(MatchingCycle cycle) {
        return new MatchingCycleDto(cycle.getId(), cycle.getName(), cycle.getStartDate(), cycle.getEndDate(), cycle.getStatus());
    }

    public DimensionOptionDto toDto(DimensionOption option) {
        return new DimensionOptionDto(option.getId(), option.getOptionText(), option.getOptionValue());
    }

    public SurveyDimensionDto toDto(SurveyDimension dimension) {
        return new SurveyDimensionDto(
                dimension.getId(),
                dimension.getDimensionKey(),
                dimension.getPrompt(),
                dimension.getDimensionType(),
                dimension.getResponseType(),
                dimension.getWeight(),
                dimension.getParentDimensionKey(),
                dimension.isReverseScored(),
                dimension.getOptions().stream().map(this::toDto).collect(Collectors.toList())
        );
    }
}