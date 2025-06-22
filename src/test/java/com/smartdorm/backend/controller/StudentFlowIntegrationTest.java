package com.smartdorm.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartdorm.backend.dto.CycleDtos;
import com.smartdorm.backend.dto.LoginRequest;
import com.smartdorm.backend.dto.LoginResponse;
import com.smartdorm.backend.dto.StudentDtos;
import com.smartdorm.backend.entity.*;
import com.smartdorm.backend.repository.*;
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
@DisplayName("集成测试: 学生核心流程")
@Transactional
public class StudentFlowIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UserRepository userRepository;
    @Autowired private MatchingCycleRepository cycleRepository;
    @Autowired private SurveyDimensionRepository dimensionRepository;
    @Autowired private DormBuildingRepository buildingRepository;
    @Autowired private DormRoomRepository roomRepository;
    @Autowired private BedRepository bedRepository;
    @Autowired private MatchingResultRepository resultRepository;

    private String adminToken;
    private String studentToken;
    private User studentUser;
    private User roommateUser;
    private SurveyDimension surveyDimension;
    private MatchingCycle openCycle;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");
    }

    @BeforeEach
    void setUp() throws Exception {
        resultRepository.deleteAll();
        bedRepository.deleteAll();
        roomRepository.deleteAll();
        buildingRepository.deleteAll();
        dimensionRepository.deleteAll();
        cycleRepository.deleteAll();
        userRepository.deleteAll();

        // Create users
        User admin = createUser("admin-p4", "pass", "ADMIN", "Admin P4");
        studentUser = createUser("student-p4", "pass", "STUDENT", "Student P4");
        roommateUser = createUser("roommate-p4", "pass", "STUDENT", "Roommate P4");

        // Get tokens
        adminToken = getToken("admin-p4", "pass");
        studentToken = getToken("student-p4", "pass");

        // Admin creates a cycle and a dimension
        openCycle = new MatchingCycle();
        openCycle.setName("Test Cycle");
        openCycle.setStatus("OPEN");
        cycleRepository.save(openCycle);

        surveyDimension = new SurveyDimension();
        surveyDimension.setCycle(openCycle);
        surveyDimension.setDimensionKey("test_key");
        surveyDimension.setPrompt("Test Prompt");
        surveyDimension.setDimensionType("SOFT_FACTOR");
        surveyDimension.setResponseType("SCALE");
        dimensionRepository.save(surveyDimension);
    }

    @Test
    @DisplayName("学生可以获取问卷、提交答案、并查看模拟的分配结果")
    void studentCanPerformFullSurveyAndResultCheckFlow() throws Exception {
        // Step 1: Student gets the survey
        mockMvc.perform(get("/student/survey").header("Authorization", studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cycleId", is(openCycle.getId().toString())))
                .andExpect(jsonPath("$.dimensions", hasSize(1)))
                .andExpect(jsonPath("$.dimensions[0].prompt", is("Test Prompt")));

        // Step 2: Student submits responses
        StudentDtos.ResponseItem responseItem = new StudentDtos.ResponseItem(surveyDimension.getId(), 4.0);
        StudentDtos.UserResponseSubmitDto submitDto = new StudentDtos.UserResponseSubmitDto(List.of(responseItem));

        mockMvc.perform(post("/student/responses")
                        .header("Authorization", studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitDto)))
                .andExpect(status().isOk());

        // Step 3: Admin triggers the assignment (placeholder)
        mockMvc.perform(post("/admin/cycles/" + openCycle.getId() + "/trigger-assignment")
                        .header("Authorization", adminToken))
                .andExpect(status().isAccepted());

        // Step 4: Manually create assignment results for testing the GET /result endpoint
        createMockResults();

        // Step 5: Student checks their result
        mockMvc.perform(get("/student/result").header("Authorization", studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignment.building", is("Test Building")))
                .andExpect(jsonPath("$.assignment.room", is("101")))
                .andExpect(jsonPath("$.assignment.bed", is(1)))
                .andExpect(jsonPath("$.roommates", hasSize(1)))
                .andExpect(jsonPath("$.roommates[0].name", is("Roommate P4")));
    }

    private User createUser(String studentId, String password, String role, String name) {
        User user = new User();
        user.setStudentId(studentId);
        user.setName(name);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setGender("MALE");
        user.setCollege("Testing College");
        return userRepository.save(user);
    }

    private String getToken(String username, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest(username, password);
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        LoginResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), LoginResponse.class);
        return "Bearer " + response.token();
    }

    private void createMockResults() {
        DormBuilding building = new DormBuilding();
        building.setName("Test Building");
        buildingRepository.save(building);

        DormRoom room = new DormRoom();
        room.setBuilding(building);
        room.setRoomNumber("101");
        room.setCapacity(4);
        room.setGenderType("MALE");
        roomRepository.save(room);

        Bed bed1 = new Bed();
        bed1.setRoom(room);
        bed1.setBedNumber(1);
        bedRepository.save(bed1);

        Bed bed2 = new Bed();
        bed2.setRoom(room);
        bed2.setBedNumber(2);
        bedRepository.save(bed2);

        UUID groupId = UUID.randomUUID();

        MatchingResult result1 = new MatchingResult();
        result1.setCycle(openCycle);
        result1.setUser(studentUser);
        result1.setBed(bed1);
        result1.setMatchGroupId(groupId);
        resultRepository.save(result1);

        MatchingResult result2 = new MatchingResult();
        result2.setCycle(openCycle);
        result2.setUser(roommateUser);
        result2.setBed(bed2);
        result2.setMatchGroupId(groupId);
        resultRepository.save(result2);
    }
}