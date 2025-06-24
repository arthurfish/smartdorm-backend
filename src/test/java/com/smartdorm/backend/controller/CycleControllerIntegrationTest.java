// src/test/java/com/smartdorm/backend/controller/CycleControllerIntegrationTest.java
package com.smartdorm.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartdorm.backend.dto.CycleDtos.*;
import com.smartdorm.backend.dto.LoginRequest;
import com.smartdorm.backend.dto.LoginResponse;
import com.smartdorm.backend.entity.User;
import com.smartdorm.backend.repository.MatchingCycleRepository;
import com.smartdorm.backend.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
@DisplayName("集成测试: CycleController")
@Transactional
class CycleControllerIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private MatchingCycleRepository cycleRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String adminToken;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");
    }

    @BeforeEach
    void setUp() throws Exception {
        userRepository.deleteAll();
        cycleRepository.deleteAll();

        User admin = new User();
        admin.setStudentId("admin02");
        admin.setName("Cycle Admin");
        admin.setPassword(passwordEncoder.encode("cyclepass"));
        admin.setRole("ADMIN");
        admin.setGender("FEMALE");
        admin.setCollege("Management");
        userRepository.save(admin);

        adminToken = getAdminToken("admin02", "cyclepass");
    }

    private String getAdminToken(String username, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest(username, password);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        LoginResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), LoginResponse.class);
        return "Bearer " + response.token();
    }

    @Test
    @DisplayName("Admin可以完整地管理一个分配周期和其问卷维度")
    void adminCanManageFullLifecycleOfCycleAndDimensions() throws Exception {
        // 1. Create a cycle
        MatchingCycleCreateDto createDto = new MatchingCycleCreateDto("2024秋季新生分配", null, null);
        MvcResult cycleResult = mockMvc.perform(post("/api/admin/cycles")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("2024秋季新生分配")))
                .andExpect(jsonPath("$.status", is("DRAFT")))
                .andReturn();
        MatchingCycleDto createdCycle = objectMapper.readValue(cycleResult.getResponse().getContentAsString(), MatchingCycleDto.class);
        UUID cycleId = createdCycle.id();

        // 2. Add a dimension with options to the cycle
        // [关键修复] 使用 setter 方法来构建 SurveyDimensionCreateDto 对象
        List<OptionCreateDto> options = List.of(
                new OptionCreateDto("早睡早起", 1.0),
                new OptionCreateDto("晚睡晚起", 5.0)
        );
        SurveyDimensionCreateDto dimensionDto = new SurveyDimensionCreateDto();
        dimensionDto.setDimensionKey("rest_habit");
        dimensionDto.setPrompt("你的作息习惯是？");
        dimensionDto.setDimensionType("SOFT_FACTOR");
        dimensionDto.setResponseType("SINGLE_CHOICE");
        dimensionDto.setWeight(2.0);
        dimensionDto.setReverseScored(false);
        dimensionDto.setOptions(options);

        // 发送请求
        mockMvc.perform(post("/api/admin/cycles/" + cycleId + "/dimensions")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dimensionDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dimensionKey", is("rest_habit")))
                .andExpect(jsonPath("$.options", hasSize(2)))
                .andExpect(jsonPath("$.options[0].optionText", is("早睡早起")));

        // 3. Get dimensions for the cycle and verify
        mockMvc.perform(get("/api/admin/cycles/" + cycleId + "/dimensions").header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].prompt", is("你的作息习惯是？")));

        // 4. Update status to OPEN, then attempt to delete (should fail)
        MatchingCycleUpdateDto updateStatusDto = new MatchingCycleUpdateDto(null, null, null, "OPEN");
        mockMvc.perform(put("/api/admin/cycles/" + cycleId)
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateStatusDto)))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/admin/cycles/" + cycleId).header("Authorization", adminToken))
                .andExpect(status().isConflict());

        // 5. Change status back to DRAFT and delete successfully
        MatchingCycleUpdateDto revertStatusDto = new MatchingCycleUpdateDto(null, null, null, "DRAFT");
        mockMvc.perform(put("/api/admin/cycles/" + cycleId)
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(revertStatusDto)))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/admin/cycles/" + cycleId).header("Authorization", adminToken))
                .andExpect(status().isNoContent());
    }
}