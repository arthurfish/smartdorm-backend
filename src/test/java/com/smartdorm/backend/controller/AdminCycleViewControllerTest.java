// src/test/java/com/smartdorm/backend/controller/AdminCycleViewControllerTest.java
package com.smartdorm.backend.controller;

import com.smartdorm.backend.dto.AdminDtos;
import com.smartdorm.backend.dto.CycleDtos;
import com.smartdorm.backend.service.AdminAssignmentService;
import com.smartdorm.backend.service.CycleManagementService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("TDD for AdminCycleViewController")
@WithMockUser(roles = "ADMIN") // Apply to all tests in this class
public class AdminCycleViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CycleManagementService cycleService;

    @Test
    @DisplayName("GET /view/admin/cycles - Should return list of cycles")
    void whenListCycles_thenReturnsListView() throws Exception {
        when(cycleService.getAllCycles()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/view/admin/cycles"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/cycle/cycles-list"))
                .andExpect(model().attributeExists("cycles"));
    }

    @Test
    @DisplayName("POST /view/admin/cycles/create - Should create cycle and redirect")
    void whenCreateCycle_thenRedirects() throws Exception {
        CycleDtos.MatchingCycleCreateDto createDto = new CycleDtos.MatchingCycleCreateDto("Test Cycle", Instant.now(), Instant.now());
        when(cycleService.createCycle(any())).thenReturn(new CycleDtos.MatchingCycleDto(UUID.randomUUID(), "Test Cycle", null, null, "DRAFT"));

        mockMvc.perform(post("/view/admin/cycles/create")
                        .flashAttr("cycleDto", createDto)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/view/admin/cycles"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(cycleService, times(1)).createCycle(any(CycleDtos.MatchingCycleCreateDto.class));
    }

    @Test
    @DisplayName("GET /view/admin/cycles/{id}/dimensions - Should return dimensions list for a cycle")
    void whenListDimensions_thenReturnsDimensionsView() throws Exception {
        UUID cycleId = UUID.randomUUID();
        CycleDtos.MatchingCycleDto mockCycle = new CycleDtos.MatchingCycleDto(cycleId, "Test Cycle", null, null, "DRAFT");
        List<CycleDtos.SurveyDimensionDto> mockDimensions = Collections.emptyList();

        when(cycleService.getCycleById(cycleId)).thenReturn(mockCycle);
        when(cycleService.getDimensionsForCycle(cycleId)).thenReturn(mockDimensions);

        mockMvc.perform(get("/view/admin/cycles/" + cycleId + "/dimensions"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/cycle/dimensions-list"))
                .andExpect(model().attribute("cycle", mockCycle))
                .andExpect(model().attribute("dimensions", mockDimensions));
    }

    @Test
    @DisplayName("POST /view/admin/cycles/{id}/dimensions/create - Should create dimension and redirect")
    void whenCreateDimension_thenRedirectsToDimensionsList() throws Exception {
// [修复] 使用默认构造函数和 setter 方法来构建 DTO 对象
        UUID cycleId = UUID.randomUUID();

        CycleDtos.SurveyDimensionCreateDto createDto = new CycleDtos.SurveyDimensionCreateDto();
        createDto.setDimensionKey("rest_habit");
        createDto.setPrompt("Your rest habit?");
        createDto.setDimensionType("SOFT_FACTOR");
        createDto.setResponseType("SINGLE_CHOICE");
        createDto.setWeight(1.0);
        createDto.setParentDimensionKey(null); // 也可以省略这行，因为对象属性默认为 null
        createDto.setReverseScored(false);
        createDto.setOptions(List.of(new CycleDtos.OptionCreateDto("Early Bird", 1.0)));
        when(cycleService.createDimensionForCycle(eq(cycleId), any())).thenReturn(mock(CycleDtos.SurveyDimensionDto.class));

        mockMvc.perform(post("/view/admin/cycles/" + cycleId + "/dimensions/create")
                        .flashAttr("dimensionDto", createDto)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/view/admin/cycles/" + cycleId + "/dimensions"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(cycleService, times(1)).createDimensionForCycle(eq(cycleId), any(CycleDtos.SurveyDimensionCreateDto.class));
    }

    @MockBean
    private AdminAssignmentService adminAssignmentService; // 注入 mock

    @Test
    @DisplayName("[P5] POST /trigger-assignment - Should trigger assignment and redirect")
    void whenTriggerAssignment_thenRedirectsToResults() throws Exception {
        UUID cycleId = UUID.randomUUID();
        doNothing().when(adminAssignmentService).triggerAssignment(cycleId);

        mockMvc.perform(post("/view/admin/cycles/" + cycleId + "/trigger-assignment")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/view/admin/cycles/" + cycleId + "/results"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @DisplayName("[P5] GET /results - Should display assignment results list")
    void whenGetResults_thenReturnsResultsListView() throws Exception {
        UUID cycleId = UUID.randomUUID();
        when(cycleService.getCycleById(cycleId)).thenReturn(new CycleDtos.MatchingCycleDto(cycleId, "Test Cycle", null, null, "COMPLETED"));
        when(adminAssignmentService.getAssignmentResults(cycleId)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/view/admin/cycles/" + cycleId + "/results"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/cycle/results-list"))
                .andExpect(model().attributeExists("cycle", "results"));
    }

    @Test
    @DisplayName("[P5] GET /quality-report - Should display quality report")
    void whenGetQualityReport_thenReturnsReportView() throws Exception {
        UUID cycleId = UUID.randomUUID();
        AdminDtos.AdminAssignmentValidationDto mockReport = new AdminDtos.AdminAssignmentValidationDto(true, "OK", Collections.emptyList());

        when(cycleService.getCycleById(cycleId)).thenReturn(new CycleDtos.MatchingCycleDto(cycleId, "Test Cycle", null, null, "COMPLETED"));
        when(adminAssignmentService.validateResults(cycleId)).thenReturn(mockReport);

        mockMvc.perform(get("/view/admin/cycles/" + cycleId + "/quality-report"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/cycle/quality-report"))
                .andExpect(model().attribute("report", mockReport));
    }
}