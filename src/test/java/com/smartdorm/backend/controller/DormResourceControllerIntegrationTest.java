package com.smartdorm.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartdorm.backend.dto.DormDtos.*;
import com.smartdorm.backend.dto.LoginRequest;
import com.smartdorm.backend.dto.LoginResponse;
import com.smartdorm.backend.entity.DormBuilding;
import com.smartdorm.backend.entity.DormRoom;
import com.smartdorm.backend.entity.User;
import com.smartdorm.backend.repository.DormBuildingRepository;
import com.smartdorm.backend.repository.DormRoomRepository;
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

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Testcontainers
@AutoConfigureMockMvc
@DisplayName("集成测试: DormResourceController")
@Transactional // Roll back transactions after each test
class DormResourceControllerIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private DormBuildingRepository buildingRepository;
    @Autowired
    private DormRoomRepository roomRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

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
        buildingRepository.deleteAll();

        // Create an admin user
        User admin = new User();
        admin.setStudentId("admin01");
        admin.setName("Admin User");
        admin.setPassword(passwordEncoder.encode("adminpass"));
        admin.setRole("ADMIN");
        admin.setGender("MALE");
        admin.setCollege("Admin College");
        userRepository.save(admin);

        // Login as admin to get token
        adminToken = getAdminToken("admin01", "adminpass");
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
    @DisplayName("Admin可以创建、获取、更新和删除楼栋")
    void adminCanManageBuildings() throws Exception {
        // 1. Create Building
        BuildingCreateUpdateDto createDto = new BuildingCreateUpdateDto("紫荆1号楼");
        MvcResult createResult = mockMvc.perform(post("/api/admin/dorm-buildings")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("紫荆1号楼")))
                .andReturn();
        DormBuildingDto createdBuilding = objectMapper.readValue(createResult.getResponse().getContentAsString(), DormBuildingDto.class);
        UUID buildingId = createdBuilding.id();

        // 2. Get All Buildings
        mockMvc.perform(get("/api/admin/dorm-buildings").header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("紫荆1号楼")));

        // 3. Update Building
        BuildingCreateUpdateDto updateDto = new BuildingCreateUpdateDto("紫荆1号楼 (新)");
        mockMvc.perform(put("/api/admin/dorm-buildings/" + buildingId)
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("紫荆1号楼 (新)")));

        // 4. Delete Building
        mockMvc.perform(delete("/api/admin/dorm-buildings/" + buildingId).header("Authorization", adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("当楼栋下有房间时，删除楼栋应返回409 Conflict")
    void deleteBuilding_withRooms_shouldReturnConflict() throws Exception {
        DormBuilding building = new DormBuilding();
        building.setName("测试楼");
        building = buildingRepository.save(building);

        DormRoom room = new DormRoom();
        room.setBuilding(building);
        room.setRoomNumber("101");
        room.setCapacity(4);
        room.setGenderType("MALE");
        roomRepository.save(room);

        mockMvc.perform(delete("/api/admin/dorm-buildings/" + building.getId()).header("Authorization", adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("Cannot delete building with id " + building.getId() + " because it contains rooms.")));
    }

    @Test
    @DisplayName("Admin可以为房间批量添加床位")
    void adminCanCreateBedsForRoom() throws Exception {
        // Setup a building and a room
        DormBuilding building = new DormBuilding();
        building.setName("宿舍楼A");
        building = buildingRepository.save(building);

        RoomCreateUpdateDto roomDto = new RoomCreateUpdateDto(building.getId(), "201", 4, "FEMALE");
        MvcResult roomResult = mockMvc.perform(post("/api/admin/dorm-rooms")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roomDto)))
                .andExpect(status().isCreated())
                .andReturn();
        DormRoomDto createdRoom = objectMapper.readValue(roomResult.getResponse().getContentAsString(), DormRoomDto.class);

        // Create beds for the room
        BedCreateRequestDto bedRequest = new BedCreateRequestDto(4);
        mockMvc.perform(post("/api/admin/rooms/" + createdRoom.id() + "/beds")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bedRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.count", is(4)))
                .andExpect(jsonPath("$.beds", hasSize(4)))
                .andExpect(jsonPath("$.beds[0].bedNumber", is(1)));
    }

    @Test
    @DisplayName("非Admin用户访问宿舍资源API应返回403 Forbidden")
    void nonAdminAccess_shouldReturnForbidden() throws Exception {
        // Create a student user and get token
        User student = new User();
        student.setStudentId("student01");
        student.setName("Student User");
        student.setPassword(passwordEncoder.encode("studentpass"));
        student.setRole("STUDENT");
        student.setGender("FEMALE");
        student.setCollege("Test College");
        userRepository.save(student);
        String studentToken = getAdminToken("student01", "studentpass");

        mockMvc.perform(get("/api/admin/dorm-buildings").header("Authorization", studentToken))
                .andExpect(status().isForbidden());
    }
}