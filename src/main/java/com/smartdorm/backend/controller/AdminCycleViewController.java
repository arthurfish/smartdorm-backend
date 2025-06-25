// src/main/java/com/smartdorm/backend/controller/AdminCycleViewController.java
package com.smartdorm.backend.controller;

import com.smartdorm.backend.dto.CycleDtos;
import com.smartdorm.backend.exception.ResourceNotFoundException;
import com.smartdorm.backend.service.AdminAssignmentService;
import com.smartdorm.backend.service.CycleManagementService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Controller
@RequestMapping("/view/admin/cycles")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCycleViewController {

    private final CycleManagementService cycleService;

    // 注入新的 Service
    private final AdminAssignmentService adminAssignmentService;

    // 更新构造函数
    public AdminCycleViewController(CycleManagementService cycleService, AdminAssignmentService adminAssignmentService) {
        this.cycleService = cycleService;
        this.adminAssignmentService = adminAssignmentService;
    }

    @GetMapping
    public String listCycles(Model model) {
        model.addAttribute("cycles", cycleService.getAllCycles());
        return "admin/cycle/cycles-list";
    }

    @GetMapping("/new")
    public String showNewCycleForm(Model model) {
        if (!model.containsAttribute("cycleDto")) {
            model.addAttribute("cycleDto", new CycleDtos.MatchingCycleCreateDto("", null, null));
        }
        model.addAttribute("pageTitle", "新建匹配周期");
        return "admin/cycle/cycle-form";
    }
    @PostMapping("/create")
    public String createCycle(@Valid @ModelAttribute("cycleDto") CycleDtos.MatchingCycleCreateDto dto,
                              BindingResult result, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.cycleDto", result);
            redirectAttributes.addFlashAttribute("cycleDto", dto);
            return "redirect:/view/admin/cycles/new";
        }
        CycleDtos.MatchingCycleDto created = cycleService.createCycle(dto);
        redirectAttributes.addFlashAttribute("successMessage", "周期 '" + created.name() + "' 创建成功！");
        return "redirect:/view/admin/cycles";
    }

    @GetMapping("/{id}/edit")
    public String showEditCycleForm(@PathVariable UUID id, Model model) {
        if (!model.containsAttribute("cycleDto")) {
            CycleDtos.MatchingCycleDto cycle = cycleService.getCycleById(id);
            CycleDtos.MatchingCycleUpdateDto updateDto = new CycleDtos.MatchingCycleUpdateDto(cycle.name(), cycle.startDate(), cycle.endDate(), cycle.status());
            model.addAttribute("cycleDto", updateDto);
        }
        model.addAttribute("cycleId", id);
        model.addAttribute("pageTitle", "编辑匹配周期");
        return "admin/cycle/cycle-form";
    }

    @PostMapping("/{id}/update")
    public String updateCycle(@PathVariable UUID id, @Valid @ModelAttribute("cycleDto") CycleDtos.MatchingCycleUpdateDto dto,
                              BindingResult result, RedirectAttributes redirectAttributes, Model model) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.cycleDto", result);
            redirectAttributes.addFlashAttribute("cycleDto", dto);
            return "redirect:/view/admin/cycles/" + id + "/edit";
        }
        cycleService.updateCycle(id, dto);
        redirectAttributes.addFlashAttribute("successMessage", "周期信息更新成功！");
        return "redirect:/view/admin/cycles";
    }

    @PostMapping("/{id}/delete")
    public String deleteCycle(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            cycleService.deleteCycle(id);
            redirectAttributes.addFlashAttribute("successMessage", "周期已成功删除。");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "删除失败: " + e.getMessage());
        }
        return "redirect:/view/admin/cycles";
    }

    @GetMapping("/{cycleId}/dimensions")
    public String listDimensions(@PathVariable UUID cycleId, Model model) {
        model.addAttribute("cycle", cycleService.getCycleById(cycleId));
        model.addAttribute("dimensions", cycleService.getDimensionsForCycle(cycleId));
        return "admin/cycle/dimensions-list";
    }

    @GetMapping("/{cycleId}/dimensions/new")
    public String showNewDimensionForm(@PathVariable UUID cycleId, Model model) {
        if (!model.containsAttribute("dimensionDto")) {
            CycleDtos.SurveyDimensionCreateDto dto = new CycleDtos.SurveyDimensionCreateDto();
            // 预填充4个空的选项行
            dto.setOptions(IntStream.range(0, 4)
                    .mapToObj(i -> new CycleDtos.OptionCreateDto("", null))
                    .collect(Collectors.toList()));
            model.addAttribute("dimensionDto", dto);
        }
        model.addAttribute("cycle", cycleService.getCycleById(cycleId));
        model.addAttribute("pageTitle", "新建问卷维度");
        return "admin/cycle/dimension-form";
    }

    @PostMapping("/{cycleId}/dimensions/create")
    public String createDimension(@PathVariable UUID cycleId,
                                  @Valid @ModelAttribute("dimensionDto") CycleDtos.SurveyDimensionCreateDto dto,
                                  BindingResult result, RedirectAttributes redirectAttributes) {

        // 1. 过滤掉用户未填写或未完整填写的空选项行。
        // [修复] 使用 .getOptionText() 和 .getOptionValue()
        List<CycleDtos.OptionCreateDto> filteredOptions = dto.getOptions().stream()
                .filter(option -> StringUtils.hasText(option.getOptionText()) && Objects.nonNull(option.getOptionValue()))
                .collect(Collectors.toList());

        // 2.【关键验证】检查过滤后的有效选项是否为空。
        if (filteredOptions.isEmpty()) {
            result.rejectValue("options", "options.notempty", "必须至少提供一个有效的选项（描述和分值都需填写）。");
        }

        // 3. 检查是否存在任何验证错误
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.dimensionDto", result);
            redirectAttributes.addFlashAttribute("dimensionDto", dto);
            return "redirect:/view/admin/cycles/" + cycleId + "/dimensions/new";
        }

        // 4. 【核心步骤】用过滤后的干净选项列表更新DTO
        dto.setOptions(filteredOptions);
        cycleService.createDimensionForCycle(cycleId, dto);

        // 5. 操作成功
        redirectAttributes.addFlashAttribute("successMessage", "问卷维度 '" + dto.getPrompt() + "' 创建成功！");
        return "redirect:/view/admin/cycles/" + cycleId + "/dimensions";
    }

    /**
     * [Phase 5] 触发一键分配
     * 对应 use case: ADM-03
     */
    @PostMapping("/{cycleId}/trigger-assignment")
    public String triggerAssignment(@PathVariable UUID cycleId, RedirectAttributes redirectAttributes) {
        try {
            adminAssignmentService.triggerAssignment(cycleId);
            redirectAttributes.addFlashAttribute("successMessage", "分配流程已成功启动并完成！");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "分配失败: " + e.getMessage());
        }
        // 分配完成后，重定向到结果页面
        return "redirect:/view/admin/cycles/" + cycleId + "/results";
    }

    /**
     * [Phase 5] 显示分配结果列表
     * 对应 use case: ADM-04
     */
    @GetMapping("/{cycleId}/results")
    public String showResultsList(@PathVariable UUID cycleId, Model model) {
        model.addAttribute("cycle", cycleService.getCycleById(cycleId));
        model.addAttribute("results", adminAssignmentService.getAssignmentResults(cycleId));
        return "admin/cycle/results-list";
    }

    /**
     * [Phase 5] 显示分配质量报告
     * 对应 use case: ADM-08
     */
    @GetMapping("/{cycleId}/quality-report")
    public String showQualityReport(@PathVariable UUID cycleId, Model model) {
        model.addAttribute("cycle", cycleService.getCycleById(cycleId));
        model.addAttribute("report", adminAssignmentService.validateResults(cycleId));
        return "admin/cycle/quality-report";
    }
}
