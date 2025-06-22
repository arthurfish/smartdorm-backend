package com.smartdorm.backend.service;

import com.smartdorm.backend.dto.DormDtos.*;
import com.smartdorm.backend.entity.Bed;
import com.smartdorm.backend.entity.DormBuilding;
import com.smartdorm.backend.entity.DormRoom;
import com.smartdorm.backend.exception.DataConflictException;
import com.smartdorm.backend.exception.ResourceNotFoundException;
import com.smartdorm.backend.mapper.DormMapper;
import com.smartdorm.backend.repository.BedRepository;
import com.smartdorm.backend.repository.DormBuildingRepository;
import com.smartdorm.backend.repository.DormRoomRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Transactional
public class DormResourceService {

    private final DormBuildingRepository buildingRepository;
    private final DormRoomRepository roomRepository;
    private final BedRepository bedRepository;
    private final DormMapper dormMapper;

    public DormResourceService(DormBuildingRepository buildingRepository, DormRoomRepository roomRepository, BedRepository bedRepository, DormMapper dormMapper) {
        this.buildingRepository = buildingRepository;
        this.roomRepository = roomRepository;
        this.bedRepository = bedRepository;
        this.dormMapper = dormMapper;
    }

    // --- Building Methods ---

    public List<DormBuildingDto> getAllBuildings() {
        return buildingRepository.findAll().stream().map(dormMapper::toDto).collect(Collectors.toList());
    }

    public DormBuildingDto createBuilding(BuildingCreateUpdateDto dto) {
        DormBuilding building = new DormBuilding();
        building.setName(dto.name());
        return dormMapper.toDto(buildingRepository.save(building));
    }

    public DormBuildingDto updateBuilding(UUID id, BuildingCreateUpdateDto dto) {
        DormBuilding building = buildingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Building not found with id: " + id));
        building.setName(dto.name());
        return dormMapper.toDto(buildingRepository.save(building));
    }

    public void deleteBuilding(UUID id) {
        if (roomRepository.existsByBuildingId(id)) {
            throw new DataConflictException("Cannot delete building with id " + id + " because it contains rooms.");
        }
        buildingRepository.deleteById(id);
    }

    // --- Room Methods ---

    public DormRoomDto createRoom(RoomCreateUpdateDto dto) {
        DormBuilding building = buildingRepository.findById(dto.buildingId())
                .orElseThrow(() -> new ResourceNotFoundException("Building not found with id: " + dto.buildingId()));
        DormRoom room = new DormRoom();
        room.setBuilding(building);
        room.setRoomNumber(dto.roomNumber());
        room.setCapacity(dto.capacity());
        room.setGenderType(dto.genderType());
        return dormMapper.toDto(roomRepository.save(room));
    }

    public DormRoomDto updateRoom(UUID roomId, RoomCreateUpdateDto dto) {
        DormRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + roomId));
        DormBuilding building = buildingRepository.findById(dto.buildingId())
                .orElseThrow(() -> new ResourceNotFoundException("Building not found with id: " + dto.buildingId()));

        room.setBuilding(building);
        room.setRoomNumber(dto.roomNumber());
        room.setCapacity(dto.capacity());
        room.setGenderType(dto.genderType());

        return dormMapper.toDto(roomRepository.save(room));
    }

    public void deleteRoom(UUID roomId) {
        if (bedRepository.existsByRoomId(roomId)) {
            throw new DataConflictException("Cannot delete room with id " + roomId + " because it contains beds.");
        }
        roomRepository.deleteById(roomId);
    }

    // --- Bed Methods ---

    public BedsCreatedResponseDto createBedsForRoom(UUID roomId, BedCreateRequestDto dto) {
        DormRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + roomId));

        List<Bed> newBeds = new ArrayList<>();
        for (int i = 1; i <= dto.bedCount(); i++) {
            Bed bed = new Bed();
            bed.setRoom(room);
            bed.setBedNumber(i);
            newBeds.add(bed);
        }

        List<Bed> savedBeds = bedRepository.saveAll(newBeds);
        List<BedDto> bedDtos = savedBeds.stream().map(dormMapper::toDto).collect(Collectors.toList());

        return new BedsCreatedResponseDto(savedBeds.size(), bedDtos);
    }

    public void deleteBed(UUID bedId) {
        // Here you might add a check if the bed is assigned in matching_results table in later phases.
        // For now, simple deletion is fine.
        if (!bedRepository.existsById(bedId)) {
            throw new ResourceNotFoundException("Bed not found with id: " + bedId);
        }
        bedRepository.deleteById(bedId);
    }
}