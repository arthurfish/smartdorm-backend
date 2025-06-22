package com.smartdorm.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartdorm.backend.dto.LoginRequest;
import com.smartdorm.backend.dto.LoginResponse;
import com.smartdorm.backend.dto.SupportDtos.*;
import com.smartdorm.backend.entity.MatchingCycle;
import com.smartdorm.backend.entity.Notification;
import com.smartdorm.backend.entity.User;
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

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
@DisplayName("集成测试: P5 - 支持性功能 (Feedback, Swap, Article, Notification)")
@Transactional
public class SupportFeaturesIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UserRepository userRepository;
    @Autowired private MatchingCycleRepository cycleRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private SwapRequestRepository swapRequestRepository;
    @Autowired private ContentArticleRepository articleRepository;

    private String adminToken;
    private String studentToken;
    private User studentUser;
    private User adminUser;
    private MatchingCycle completedCycle;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");
    }

    @BeforeEach
    void setUp() throws Exception {
        // Clean slate
        notificationRepository.deleteAll();
        swapRequestRepository.deleteAll();
        articleRepository.deleteAll();
        cycleRepository.deleteAll();
        userRepository.deleteAll();

        // Create users
        adminUser = createUser("admin-p5", "pass", "ADMIN", "Support Admin");
        studentUser = createUser("student-p5", "pass", "STUDENT", "Support Student");

        // Get tokens
        adminToken = getToken("admin-p5", "pass");
        studentToken = getToken("student-p5", "pass");

        // Create a completed cycle for context
        completedCycle = new MatchingCycle();
        completedCycle.setName("Completed Cycle For Support");
        completedCycle.setStatus("COMPLETED");
        cycleRepository.save(completedCycle);
    }

    @Test
    @DisplayName("学生可以提交反馈和调宿申请，管理员可以查看和处理申请")
    void testFeedbackAndSwapRequestFlow() throws Exception {
        // 1. Student submits feedback
        FeedbackCreateDto feedbackDto = new FeedbackCreateDto(false, 5, "Great experience!");
        mockMvc.perform(post("/student/feedback")
                        .header("Authorization", studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(feedbackDto)))
                .andExpect(status().isCreated());

        // 2. Student submits a swap request
        SwapRequestCreateDto swapDto = new SwapRequestCreateDto("Circumstantial reasons.");
        mockMvc.perform(post("/student/swap-requests")
                        .header("Authorization", studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(swapDto)))
                .andExpect(status().isCreated());

        // 3. Admin gets the list of swap requests
        MvcResult getResult = mockMvc.perform(get("/admin/swap-requests")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].reason", is("Circumstantial reasons.")))
                .andExpect(jsonPath("$[0].status", is("PENDING")))
                .andReturn();
        String jsonResponse = getResult.getResponse().getContentAsString();
        SwapRequestDto[] requests = objectMapper.readValue(jsonResponse, SwapRequestDto[].class);
        UUID requestId = requests[0].id();

        // 4. Admin processes (approves) the swap request
        SwapRequestUpdateDto updateDto = new SwapRequestUpdateDto("APPROVED", "Approved after review.");
        mockMvc.perform(put("/admin/swap-requests/" + requestId + "/process")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("APPROVED")))
                .andExpect(jsonPath("$.adminComment", is("Approved after review.")));
    }

    @Test
    @DisplayName("管理员可以完整管理文章，学生可以查看文章")
    void testArticleManagementFlow() throws Exception {
        // 1. Admin creates an article
        ArticleCreateDto createDto = new ArticleCreateDto("指南", "宿舍生活指南", "生活技巧");
        MvcResult createResult = mockMvc.perform(post("/admin/articles")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("指南")))
                .andExpect(jsonPath("$.authorName", is("Support Admin")))
                .andReturn();
        ArticleDto createdArticle = objectMapper.readValue(createResult.getResponse().getContentAsString(), ArticleDto.class);
        UUID articleId = createdArticle.id();

        // 2. Student gets the list of articles
        mockMvc.perform(get("/student/articles").header("Authorization", studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(articleId.toString())));

        // 3. Student gets a specific article
        mockMvc.perform(get("/student/articles/" + articleId).header("Authorization", studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", is("宿舍生活指南")));

        // 4. Admin updates the article
        ArticleUpdateDto updateDto = new ArticleUpdateDto("更新版指南", null, "心理健康");
        mockMvc.perform(put("/admin/articles/" + articleId)
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("更新版指南")))
                .andExpect(jsonPath("$.category", is("心理健康")));

        // 5. Admin deletes the article
        mockMvc.perform(delete("/admin/articles/" + articleId).header("Authorization", adminToken))
                .andExpect(status().isNoContent());

        // 6. Student cannot find the article anymore
        mockMvc.perform(get("/student/articles/" + articleId).header("Authorization", studentToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("学生可以获取通知列表并将通知标记为已读")
    void testNotificationFlow() throws Exception {
        // Manually create a notification for the student
        Notification notification = new Notification();
        notification.setUser(studentUser);
        notification.setMessage("您的分配结果已出炉！");
        notification.setLinkUrl("/student/result");
        notification = notificationRepository.save(notification);
        UUID notificationId = notification.getId();

        // 1. Student gets their notifications
        mockMvc.perform(get("/student/notifications").header("Authorization", studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].isRead", is(false)))
                .andExpect(jsonPath("$[0].message", is("您的分配结果已出炉！")));

        // 2. Student marks the notification as read
        mockMvc.perform(post("/student/notifications/" + notificationId + "/read")
                        .header("Authorization", studentToken))
                .andExpect(status().isNoContent());

        // 3. Student gets notifications again and verifies it's read
        mockMvc.perform(get("/student/notifications").header("Authorization", studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].isRead", is(true)));
    }


    // Helper methods
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
}