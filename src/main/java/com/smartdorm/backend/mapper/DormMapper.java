package com.smartdorm.backend.mapper;

import com.smartdorm.backend.dto.DormDtos.*;
import com.smartdorm.backend.entity.Bed;
import com.smartdorm.backend.entity.DormBuilding;
import com.smartdorm.backend.entity.DormRoom;
import org.springframework.stereotype.Component;

@Component
public class DormMapper {

    public DormBuildingDto toDto(DormBuilding building) {
        return new DormBuildingDto(building.getId(), building.getName());
    }

    public DormRoomDto toDto(DormRoom room) {
        return new DormRoomDto(
                room.getId(),
                room.getBuilding().getId(),
                room.getRoomNumber(),
                room.getCapacity(),
                room.getGenderType()
        );
    }

    public BedDto toDto(Bed bed) {
        return new BedDto(bed.getId(), bed.getRoom().getId(), bed.getBedNumber());
    }
}