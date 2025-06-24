package com.smartdorm.backend.controller;

import com.smartdorm.backend.dto.DormDtos;
import com.smartdorm.backend.service.DormResourceService;
import com.smartdorm.backend.service.DormResourceService.RoomDetailDto;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/view/admin/dorms")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDormViewController {

    private final DormResourceService dormResourceService;

    public AdminDormViewController(DormResourceService dormResourceService) {
        this.dormResourceService = dormResourceService;
    }

    // --- Building Management Views ---

    @GetMapping("/buildings")
    public String listBuildings(Model model) {
        model.addAttribute("buildings", dormResourceService.getAllBuildings());
        return "admin/dorm/buildings-list";
    }

    @GetMapping("/buildings/new")
    public String showNewBuildingForm(Model model) {
        model.addAttribute("buildingDto", new DormDtos.BuildingCreateUpdateDto(""));
        model.addAttribute("pageTitle", "新建楼栋");
        return "admin/dorm/building-form";
    }

    @PostMapping("/buildings/create")
    public String createBuilding(@Valid @ModelAttribute("buildingDto") DormDtos.BuildingCreateUpdateDto dto,
                                 BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("pageTitle", "新建楼栋");
            return "admin/dorm/building-form";
        }
        dormResourceService.createBuilding(dto);
        redirectAttributes.addFlashAttribute("successMessage", "楼栋 '" + dto.name() + "' 创建成功！");
        return "redirect:/view/admin/dorms/buildings";
    }

    @GetMapping("/buildings/{id}/edit")
    public String showEditBuildingForm(@PathVariable UUID id, Model model) {
        DormDtos.DormBuildingDto building = dormResourceService.getAllBuildings().stream()
                .filter(b -> b.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid building Id:" + id));

        model.addAttribute("buildingDto", new DormDtos.BuildingCreateUpdateDto(building.name()));
        model.addAttribute("buildingId", id);
        model.addAttribute("pageTitle", "编辑楼栋");
        return "admin/dorm/building-form";
    }

    @PostMapping("/buildings/{id}/update")
    public String updateBuilding(@PathVariable UUID id, @Valid @ModelAttribute("buildingDto") DormDtos.BuildingCreateUpdateDto dto,
                                 BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("buildingId", id);
            model.addAttribute("pageTitle", "编辑楼栋");
            return "admin/dorm/building-form";
        }
        dormResourceService.updateBuilding(id, dto);
        redirectAttributes.addFlashAttribute("successMessage", "楼栋信息更新成功！");
        return "redirect:/view/admin/dorms/buildings";
    }

    @PostMapping("/buildings/{id}/delete")
    public String deleteBuilding(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            dormResourceService.deleteBuilding(id);
            redirectAttributes.addFlashAttribute("successMessage", "楼栋删除成功！");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "删除失败：" + e.getMessage());
        }
        return "redirect:/view/admin/dorms/buildings";
    }

    @GetMapping("/rooms")
    public String listRooms(Model model, @RequestParam(required = false) UUID buildingId) {
        List<RoomDetailDto> allRoomDetails = dormResourceService.getRoomDetails();

        // **BUG修复**: 直接在获取到的 roomDetails 列表上进行过滤
        List<RoomDetailDto> filteredRoomDetails = allRoomDetails;
        if (buildingId != null) {
            filteredRoomDetails = allRoomDetails.stream()
                    .filter(room -> room.buildingId().equals(buildingId))
                    .collect(Collectors.toList());
        }

        model.addAttribute("roomDetails", filteredRoomDetails);
        model.addAttribute("buildings", dormResourceService.getAllBuildings());
        model.addAttribute("selectedBuildingId", buildingId);

        return "admin/dorm/rooms-list";
    }

    @GetMapping("/rooms/new")
    public String showNewRoomForm(Model model) {
        if (!model.containsAttribute("roomDto")) {
            model.addAttribute("roomDto", new DormDtos.RoomCreateUpdateDto(null, "", 1, "MALE"));
        }
        model.addAttribute("buildings", dormResourceService.getAllBuildings());
        model.addAttribute("pageTitle", "新建房间");
        return "admin/dorm/room-form";
    }

    @PostMapping("/rooms/create")
    public String createRoom(@Valid @ModelAttribute("roomDto") DormDtos.RoomCreateUpdateDto dto,
                             BindingResult result, RedirectAttributes redirectAttributes, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("buildings", dormResourceService.getAllBuildings());
            model.addAttribute("pageTitle", "新建房间");
            return "admin/dorm/room-form";
        }
        dormResourceService.createRoom(dto);
        redirectAttributes.addFlashAttribute("successMessage", "房间 '" + dto.roomNumber() + "' 创建成功！");
        return "redirect:/view/admin/dorms/rooms";
    }

    @GetMapping("/rooms/{id}/edit")
    public String showEditRoomForm(@PathVariable UUID id, Model model) {
        // **【核心修复】**
        // 确保这里也调用的是 getRoomDetailById
        RoomDetailDto room = dormResourceService.getRoomDetailById(id);

        DormDtos.RoomCreateUpdateDto dto = new DormDtos.RoomCreateUpdateDto(room.buildingId(), room.roomNumber(), room.capacity(), room.genderType());

        model.addAttribute("roomDto", dto);
        model.addAttribute("roomId", id);
        model.addAttribute("buildings", dormResourceService.getAllBuildings());
        model.addAttribute("pageTitle", "编辑房间");
        return "admin/dorm/room-form";
    }

    @PostMapping("/rooms/{id}/update")
    public String updateRoom(@PathVariable UUID id, @Valid @ModelAttribute("roomDto") DormDtos.RoomCreateUpdateDto dto,
                             BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("roomId", id);
            model.addAttribute("buildings", dormResourceService.getAllBuildings());
            model.addAttribute("pageTitle", "编辑房间");
            return "admin/dorm/room-form";
        }
        dormResourceService.updateRoom(id, dto);
        redirectAttributes.addFlashAttribute("successMessage", "房间信息更新成功！");
        return "redirect:/view/admin/dorms/rooms";
    }

    @PostMapping("/rooms/{id}/delete")
    public String deleteRoom(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            dormResourceService.deleteRoom(id);
            redirectAttributes.addFlashAttribute("successMessage", "房间删除成功！");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "删除失败：" + e.getMessage());
        }
        return "redirect:/view/admin/dorms/rooms";
    }

    @GetMapping("/rooms/{roomId}/details")
    public String showRoomDetailsAndBeds(@PathVariable UUID roomId, Model model) {
        // **【核心修复】**
        // 确保这里调用的是 getRoomDetailById，而不是 getRoomDetails().stream()...
        RoomDetailDto roomDetails = dormResourceService.getRoomDetailById(roomId);

        List<DormDtos.BedDto> beds = dormResourceService.getBedsForRoom(roomId);

        model.addAttribute("room", roomDetails);
        model.addAttribute("beds", beds);
        model.addAttribute("bedCreateDto", new DormDtos.BedCreateRequestDto(1));
        model.addAttribute("pageTitle", "房间详情与床位管理");
        return "admin/dorm/room-details";
    }
    @PostMapping("/rooms/{roomId}/beds/create-batch")
    public String createBedsBatch(@PathVariable UUID roomId,
                                  @Valid @ModelAttribute("bedCreateDto") DormDtos.BedCreateRequestDto dto,
                                  BindingResult result, RedirectAttributes redirectAttributes, Model model) {
        if (result.hasErrors()) {
            // If validation fails, we need to reload the page with the error
            RoomDetailDto roomDetails = dormResourceService.getRoomDetails().stream()
                    .filter(r -> r.id().equals(roomId)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Invalid room Id:" + roomId));
            List<DormDtos.BedDto> beds = dormResourceService.getBedsForRoom(roomId);
            model.addAttribute("room", roomDetails);
            model.addAttribute("beds", beds);
            model.addAttribute("pageTitle", "房间详情与床位管理");
            return "admin/dorm/room-details";
        }

        dormResourceService.createBedsForRoom(roomId, dto);
        redirectAttributes.addFlashAttribute("successMessage", "成功添加 " + dto.bedCount() + " 个床位！");
        return "redirect:/view/admin/dorms/rooms/" + roomId + "/details";
    }

    @PostMapping("/beds/{bedId}/delete")
    public String deleteBed(@PathVariable UUID bedId, @RequestParam UUID roomId, RedirectAttributes redirectAttributes) {
        try {
            dormResourceService.deleteBed(bedId);
            redirectAttributes.addFlashAttribute("successMessage", "床位删除成功！");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "删除失败：" + e.getMessage());
        }
        return "redirect:/view/admin/dorms/rooms/" + roomId + "/details";
    }
}