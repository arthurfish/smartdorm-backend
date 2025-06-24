// src/test/java/com/smartdorm/backend/controller/AuthControllerIntegrationTest.java
package com.smartdorm.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartdorm.backend.dto.LoginRequest;
import com.smartdorm.backend.dto.LoginResponse;
import com.smartdorm.backend.entity.User;
import com.smartdorm.backend.repository.UserRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK) // 加载完整的Spring应用上下文
@Testcontainers // 启用Testcontainers
@AutoConfigureMockMvc // 自动配置MockMvc以模拟HTTP请求
@DisplayName("集成测试: AuthController 和安全流程")
class AuthControllerIntegrationTest {

    // 声明一个PostgreSQL容器，使用与docker-compose.yml中相同的镜像
    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private MockMvc mockMvc; // 用于执行HTTP请求

    @Autowired
    private ObjectMapper objectMapper; // 用于序列化Java对象为JSON字符串

    @Autowired
    private UserRepository userRepository; // 用于直接操作数据库以准备测试数据

    @Autowired
    private PasswordEncoder passwordEncoder; // 用于加密测试用户的密码

    // 动态配置数据源，使其指向由Testcontainers启动的数据库实例
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        // 设置ddl-auto为create，确保为每个测试类运行时都创建一个干净的表结构
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");
    }

    // 在每个测试方法运行前，清理数据库并创建一个标准测试用户
    @BeforeEach
    void setUp() {
        userRepository.deleteAll(); // 清空数据，保证测试独立性
        createTestUser("S001", "password123", "STUDENT", "John Doe");
    }

    // 辅助方法，用于在数据库中创建测试用户
    private void createTestUser(String studentId, String rawPassword, String role, String name) {
        User user = new User();
        user.setStudentId(studentId);
        user.setName(name);
        user.setPassword(passwordEncoder.encode(rawPassword)); // 密码必须加密存储
        user.setRole(role);
        user.setGender("MALE");
        user.setCollege("Test College");
        userRepository.save(user);
    }

    @Test
    @DisplayName("成功登录: 使用有效凭据应返回JWT和用户信息")
    void login_withValidCredentials_shouldReturnTokenAndUserDto() throws Exception {
        LoginRequest loginRequest = new LoginRequest("S001", "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.studentId").value("S001"))
                .andExpect(jsonPath("$.user.name").value("John Doe"))
                .andExpect(jsonPath("$.user.role").value("STUDENT"))
                .andExpect(jsonPath("$.user.password").doesNotExist()); // 关键断言: 确保密码字段未在响应中返回
    }

    @Test
    @DisplayName("登录失败: 密码错误应返回401 Unauthorized")
    void login_withInvalidPassword_shouldReturnUnauthorized() throws Exception {
        LoginRequest loginRequest = new LoginRequest("S001", "wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Invalid student ID or password"));
    }

    @Test
    @DisplayName("登录失败: 用户不存在应返回401 Unauthorized")
    void login_withNonExistentUser_shouldReturnUnauthorized() throws Exception {
        LoginRequest loginRequest = new LoginRequest("S999", "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("公开端点访问: /ping 应无需认证即可访问")
    void ping_publicEndpoint_shouldBeAccessible() throws Exception {
        mockMvc.perform(get("/api/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    @DisplayName("受保护端点访问失败: 无Token应返回401 Unauthorized")
    void accessSecuredEndpoint_withoutToken_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("受保护端点访问成功: 使用有效Token应返回用户信息")
    void accessSecuredEndpoint_withValidToken_shouldReturnOkAndUserData() throws Exception {
        // 步骤 1: 登录以获取有效的JWT
        LoginRequest loginRequest = new LoginRequest("S001", "password123");
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        // 从登录响应中解析出token
        String responseBody = result.getResponse().getContentAsString();
        LoginResponse loginResponse = objectMapper.readValue(responseBody, LoginResponse.class);
        String token = loginResponse.token();
        assertThat(token).isNotNull();

        // 步骤 2: 使用获取到的token访问受保护的 /users/me 端点
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + token)) // 在请求头中附带token
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value("S001"))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }
}