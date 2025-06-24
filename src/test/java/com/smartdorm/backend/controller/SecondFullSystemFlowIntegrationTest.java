// src/test/java/com/smartdorm/backend/controller/FullSystemFlowIntegrationTest.java
package com.smartdorm.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartdorm.backend.dto.AdminDtos;
import com.smartdorm.backend.dto.CycleDtos.*;
import com.smartdorm.backend.dto.DormDtos.*;
import com.smartdorm.backend.dto.LoginRequest;
import com.smartdorm.backend.dto.LoginResponse;
import com.smartdorm.backend.dto.StudentDtos.*;
import com.smartdorm.backend.dto.SupportDtos;
import com.smartdorm.backend.entity.*;
import com.smartdorm.backend.repository.*;
import jakarta.transaction.Transactional;
import org.antlr.v4.runtime.misc.LogManager;
import org.junit.jupiter.api.*;
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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
@DisplayName("超大规模集成测试: 端到端系统全流程验证")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SecondFullSystemFlowIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");
    static {
        postgres.start();
    }
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PasswordEncoder passwordEncoder;

    // Repositories
    @Autowired private UserRepository userRepository;
    @Autowired private MatchingCycleRepository cycleRepository;
    @Autowired private SurveyDimensionRepository dimensionRepository;
    @Autowired private DormBuildingRepository buildingRepository;
    @Autowired private DormRoomRepository roomRepository;
    @Autowired private BedRepository bedRepository;
    @Autowired private MatchingResultRepository resultRepository;
    @Autowired private UserResponseRepository responseRepository;
    @Autowired private SwapRequestRepository swapRequestRepository;
    @Autowired private ContentArticleRepository articleRepository;
    @Autowired private FeedbackRepository feedbackRepository;
    @Autowired NotificationRepository  notificationRepository;

    // State
    private String adminToken;
    private String studentToken;
    private String roommateToken;
    private User studentUser;
    private User roommateUser;
    private UUID cycleId;
    private UUID buildingId;
    private UUID roomId;
    private UUID bed1Id;
    private UUID bed2Id;
    private UUID cleanlinessDimensionId;
    private UUID atmosphereDimensionId;
    private UUID swapRequestId;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");
    }

    @BeforeAll
    void setupAll() throws Exception {
        // Clean slate
        resultRepository.deleteAllInBatch();
        responseRepository.deleteAllInBatch();
        notificationRepository.deleteAllInBatch();
        swapRequestRepository.deleteAllInBatch();
        feedbackRepository.deleteAllInBatch();
        articleRepository.deleteAllInBatch();
        bedRepository.deleteAllInBatch();
        roomRepository.deleteAllInBatch();
        buildingRepository.deleteAllInBatch();
        dimensionRepository.deleteAllInBatch();
        cycleRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();

        // Create Users
        User admin = createUser("full_admin", "password", "ADMIN", "系统管理员", "MALE", "管理学院");
        studentUser = createUser("student_01", "password", "STUDENT", "张三", "MALE", "计算机学院");
        roommateUser = createUser("student_02", "password", "STUDENT", "李四", "MALE", "计算机学院");

        // Get Tokens
        adminToken = getToken(admin.getStudentId(), "password");
        studentToken = getToken(studentUser.getStudentId(), "password");
        roommateToken = getToken(roommateUser.getStudentId(), "password");

        assertNotNull(adminToken);
        assertNotNull(studentToken);
        assertNotNull(roommateToken);
    }

    @Test
    @Order(1)
    @DisplayName("步骤1 [ADM-05]: 管理员设置基础物理资源（楼栋、房间、床位）")
    void step1_AdminManagesDormResources() throws Exception {
        BuildingCreateUpdateDto createBuildingDto = new BuildingCreateUpdateDto("紫荆公寓");
        MvcResult buildingResult = mockMvc.perform(post("/api/admin/dorm-buildings")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createBuildingDto)))
                .andExpect(status().isCreated())
                .andReturn();
        this.buildingId = objectMapper.readValue(buildingResult.getResponse().getContentAsString(), DormBuildingDto.class).id();
        assertNotNull(this.buildingId);

        RoomCreateUpdateDto createRoomDto = new RoomCreateUpdateDto(this.buildingId, "401", 4, "MALE");
        MvcResult roomResult = mockMvc.perform(post("/api/admin/dorm-rooms")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRoomDto)))
                .andExpect(status().isCreated())
                .andReturn();
        DormRoomDto createdRoom = objectMapper.readValue(roomResult.getResponse().getContentAsString(), DormRoomDto.class);
        this.roomId = createdRoom.id();
        assertNotNull(this.roomId);

        BedCreateRequestDto createBedDto = new BedCreateRequestDto(2);
        MvcResult bedsResult = mockMvc.perform(post("/api/admin/rooms/" + this.roomId + "/beds")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createBedDto)))
                .andExpect(status().isCreated())
                .andReturn();
        BedsCreatedResponseDto beds = objectMapper.readValue(bedsResult.getResponse().getContentAsString(), BedsCreatedResponseDto.class);
        this.bed1Id = beds.beds().get(0).id();
        this.bed2Id = beds.beds().get(1).id();
        assertNotNull(this.bed1Id);
        assertNotNull(this.bed2Id);
    }

    @Test
    @Order(2)
    @DisplayName("步骤2 [ADM-01, ADM-02]: 管理员创建分配周期并设计问卷")
    void step2_AdminCreatesCycleAndDesignsSurvey() throws Exception {
        MatchingCycleCreateDto createCycleDto = new MatchingCycleCreateDto("2024级计算机学院新生分配", Instant.now(), Instant.now().plusSeconds(86400 * 7));
        MvcResult cycleResult = mockMvc.perform(post("/api/admin/cycles")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCycleDto)))
                .andExpect(status().isCreated())
                .andReturn();
        this.cycleId = objectMapper.readValue(cycleResult.getResponse().getContentAsString(), MatchingCycleDto.class).id();
        assertNotNull(this.cycleId);

        List<OptionCreateDto> cleanlinessOptions = List.of(new OptionCreateDto("每日打扫", 1.0), new OptionCreateDto("每周打扫", 3.0), new OptionCreateDto("有空再打扫", 5.0));
        SurveyDimensionCreateDto cleanlinessDim = new SurveyDimensionCreateDto("cleanliness", "你对宿舍的整洁度要求是？", "SOFT_FACTOR", "SINGLE_CHOICE", 1.5, null, false, cleanlinessOptions);
        MvcResult dim1Result = mockMvc.perform(post("/api/admin/cycles/" + this.cycleId + "/dimensions").header("Authorization", adminToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(cleanlinessDim))).andExpect(status().isCreated()).andReturn();
        this.cleanlinessDimensionId = objectMapper.readValue(dim1Result.getResponse().getContentAsString(), SurveyDimensionDto.class).id();
        assertNotNull(this.cleanlinessDimensionId);

        List<OptionCreateDto> atmosphereOptions = List.of(new OptionCreateDto("希望安静学习", 1.0), new OptionCreateDto("希望热闹活跃", 2.0));
        SurveyDimensionCreateDto atmosphereDim = new SurveyDimensionCreateDto("atmosphere", "你期望的宿舍氛围是？", "HARD_FILTER", "SINGLE_CHOICE", 1.0, null, false, atmosphereOptions);
        MvcResult dim2Result = mockMvc.perform(post("/api/admin/cycles/" + this.cycleId + "/dimensions").header("Authorization", adminToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(atmosphereDim))).andExpect(status().isCreated()).andReturn();
        this.atmosphereDimensionId = objectMapper.readValue(dim2Result.getResponse().getContentAsString(), SurveyDimensionDto.class).id();
        assertNotNull(this.atmosphereDimensionId);
    }

    @Test
    @Order(3)
    @DisplayName("步骤3 [STU-02]: 管理员开放周期，学生获取问卷并提交")
    void step3_AdminOpensCycleAndStudentsSubmitSurvey() throws Exception {
        MatchingCycleUpdateDto updateDto = new MatchingCycleUpdateDto(null, null, null, "OPEN");
        mockMvc.perform(put("/api/admin/cycles/" + this.cycleId).header("Authorization", adminToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(updateDto))).andExpect(status().isOk());

        mockMvc.perform(get("/api/student/survey").header("Authorization", studentToken)).andExpect(status().isOk()).andExpect(jsonPath("$.dimensions", hasSize(2)));

        List<ResponseItem> student1Responses = List.of(new ResponseItem(cleanlinessDimensionId, 1.0), new ResponseItem(atmosphereDimensionId, 1.0));
        UserResponseSubmitDto submitDto1 = new UserResponseSubmitDto(student1Responses);
        mockMvc.perform(post("/api/student/responses").header("Authorization", studentToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(submitDto1))).andExpect(status().isOk());

        List<ResponseItem> student2Responses = List.of(new ResponseItem(cleanlinessDimensionId, 3.0), new ResponseItem(atmosphereDimensionId, 1.0));
        UserResponseSubmitDto submitDto2 = new UserResponseSubmitDto(student2Responses);
        mockMvc.perform(post("/api/student/responses").header("Authorization", roommateToken).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(submitDto2))).andExpect(status().isOk());
    }

    @Test
    @Order(4)
    @DisplayName("步骤4 [ADM-03, ADM-08]: 管理员触发分配并检验结果质量 (增强测试)")
    void step4_AdminTriggersAndValidatesAssignment() throws Exception {
        // 1. Admin triggers assignment
        mockMvc.perform(post("/api/admin/cycles/" + this.cycleId + "/trigger-assignment")
                        .header("Authorization", adminToken))
                .andExpect(status().isAccepted());

        // *** MOCKING THE ALGORITHM'S RESULT ***
        createMatchingResult(cycleId, studentUser.getId(), bed1Id, roomId);
        createMatchingResult(cycleId, roommateUser.getId(), bed2Id, roomId);

        // 2. Admin validates the results and we assert the full response structure
        mockMvc.perform(get("/api/admin/cycles/" + this.cycleId + "/validate-results")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isValid", is(true)))
                .andExpect(jsonPath("$.message", is("Results meet all validation criteria.")))
                .andExpect(jsonPath("$.details", hasSize(1)))
                .andExpect(jsonPath("$.details[0].dorm", is("Zijing Building 1-301")))
                .andExpect(jsonPath("$.details[0].metric", is("Neuroticism Mean")))
                .andExpect(jsonPath("$.details[0].value", is(0.55)))
                .andExpect(jsonPath("$.details[0].isCompliant", is(true)));
    }

    @Test
    @Order(5)
    @DisplayName("步骤5 [ADM-04]: 管理员查看最终分配结果清单")
    void step5_AdminViewsAssignmentResults() throws Exception {
        mockMvc.perform(get("/api/admin/cycles/" + this.cycleId + "/results")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].user.name", containsInAnyOrder("张三", "李四")))
                .andExpect(jsonPath("$[0].building", is("紫荆公寓")))
                .andExpect(jsonPath("$[0].room", is("401")));
    }

    @Test
    @Order(6)
    @DisplayName("步骤6 [STU-03, STU-04]: 学生查看分配结果并提交反馈和申请")
    void step6_StudentChecksResultAndSubmitsSupportRequests() throws Exception {
        mockMvc.perform(get("/api/student/result").header("Authorization", studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignment.building", is("紫荆公寓")))
                .andExpect(jsonPath("$.assignment.room", is("401")))
                .andExpect(jsonPath("$.roommates", hasSize(1)))
                .andExpect(jsonPath("$.roommates[0].name", is("李四")));

        // Submit Feedback
        var feedbackDto = new SupportDtos.FeedbackCreateDto(false, 5, "分配结果很满意！");
        mockMvc.perform(post("/api/student/feedback")
                        .header("Authorization", studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(feedbackDto)))
                .andExpect(status().isCreated());

        // Submit Swap Request
        var swapDto = new SupportDtos.SwapRequestCreateDto("感觉空调位置不太好，想换一个床位。");
        mockMvc.perform(post("/api/student/swap-requests")
                        .header("Authorization", studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(swapDto)))
                .andExpect(status().isCreated());

        // Save swapRequestId for next step
        this.swapRequestId = swapRequestRepository.findAll().get(0).getId();
    }

    @Test
    @Order(7)
    @DisplayName("步骤7 [ADM-06, ADM-07]: 管理员处理申请并发布内容")
    void step7_AdminProcessesRequestsAndPublishesContent() throws Exception {
        var swapUpdateDto = new SupportDtos.SwapRequestUpdateDto("APPROVED", "已与同学沟通，同意调换。");
        mockMvc.perform(put("/api/admin/swap-requests/" + this.swapRequestId + "/process")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(swapUpdateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("APPROVED")));

        var articleDto = new SupportDtos.ArticleCreateDto("宿舍文化建设小贴士", "...", "宿舍文化");
        mockMvc.perform(post("/api/admin/articles")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(articleDto)))
                .andExpect(status().isCreated());
    }

    @Test
    @Order(8)
    @DisplayName("步骤8 [STU-05]: 学生查看支持内容和通知")
    void step8_StudentViewsContentAndNotifications() throws Exception {
        mockMvc.perform(get("/api/student/articles?category=宿舍文化").header("Authorization", studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("宿舍文化建设小贴士")));

        // Manually create a notification for the test
        Notification notification = new Notification();
        notification.setUser(studentUser);
        notification.setMessage("您的调宿申请已被批准");
        notificationRepository.save(notification);

        mockMvc.perform(get("/api/student/notifications").header("Authorization", studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].message", containsString("您的调宿申请已被批准")));
    }


    // --- Helper Methods ---
    private User createUser(String studentId, String password, String role, String name, String gender, String college) {
        User user = new User();
        user.setStudentId(studentId);
        user.setName(name);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setGender(gender);
        user.setCollege(college);
        return userRepository.save(user);
    }

    private String getToken(String username, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest(username, password);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        LoginResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), LoginResponse.class);
        return "Bearer " + response.token();
    }

    @Transactional
    void createMatchingResult(UUID cycleId, UUID userId, UUID bedId, UUID groupId) {
        MatchingResult result = new MatchingResult();
        result.setCycle(cycleRepository.findById(cycleId).orElseThrow());
        result.setUser(userRepository.findById(userId).orElseThrow());
        result.setBed(bedRepository.findById(bedId).orElseThrow());
        result.setMatchGroupId(groupId);
        resultRepository.save(result);
    }
}