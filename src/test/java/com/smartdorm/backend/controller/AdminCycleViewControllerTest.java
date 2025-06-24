// src/test/java/com/smartdorm/backend/controller/AdminCycleViewControllerTest.java
package com.smartdorm.backend.controller;

import com.smartdorm.backend.dto.CycleDtos;
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
}