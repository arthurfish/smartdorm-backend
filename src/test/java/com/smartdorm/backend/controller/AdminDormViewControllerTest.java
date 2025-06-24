package com.smartdorm.backend.controller;

import com.smartdorm.backend.dto.DormDtos;
import com.smartdorm.backend.entity.User;
import com.smartdorm.backend.repository.UserRepository;
import com.smartdorm.backend.service.DormResourceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// 关键变更 1: 使用 @SpringBootTest 加载完整上下文
@SpringBootTest
// 关键变更 2: 自动配置 MockMvc
@AutoConfigureMockMvc
@DisplayName("TDD for AdminDormViewController (using @SpringBootTest)")
public class AdminDormViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // 我们仍然 mock service 层，以隔离 controller 的逻辑
    @MockBean
    private DormResourceService dormResourceService;

    // 注意：在@SpringBootTest模式下，我们不再需要mock SecurityConfig的依赖了，
    // 因为它们会从完整的上下文中自动加载。我们也不需要@Import(SecurityConfig.class)。
    // 但是，我们仍然需要一个已认证的用户来通过权限检查。

    @Test
    @DisplayName("管理员获取楼栋列表页面应成功")
    // 关键变更 3: 使用 @WithMockUser 来模拟一个已登录的ADMIN用户
    // 这比手动获取token要简单得多，非常适合视图层测试。
    @WithMockUser(username = "admin", roles = "ADMIN")
    void whenListBuildings_thenReturnsListView() throws Exception {
        when(dormResourceService.getAllBuildings()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/view/admin/dorms/buildings"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dorm/buildings-list"))
                .andExpect(model().attributeExists("buildings"));
    }

    @Test
    @DisplayName("管理员提交新建楼栋表单应成功并重定向")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void whenCreateBuilding_thenRedirects() throws Exception {
        when(dormResourceService.createBuilding(any(DormDtos.BuildingCreateUpdateDto.class)))
                .thenReturn(new DormDtos.DormBuildingDto(UUID.randomUUID(), "紫荆1号楼"));

        mockMvc.perform(post("/view/admin/dorms/buildings/create")
                        .contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED)
                        .param("name", "紫荆1号楼")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/view/admin/dorms/buildings"));

        verify(dormResourceService, times(1)).createBuilding(any(DormDtos.BuildingCreateUpdateDto.class));
    }

    @Test
    @DisplayName("管理员提交删除楼栋请求应成功并重定向")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void whenDeleteBuilding_thenRedirects() throws Exception {
        UUID buildingId = UUID.randomUUID();
        doNothing().when(dormResourceService).deleteBuilding(buildingId);

        mockMvc.perform(post("/view/admin/dorms/buildings/" + buildingId + "/delete")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/view/admin/dorms/buildings"));

        verify(dormResourceService, times(1)).deleteBuilding(buildingId);
    }

    // --- Room Tests ---

    @Test
    @DisplayName("管理员获取房间详情与床位列表页面应成功")
    @WithMockUser(roles = "ADMIN")
    void whenShowRoomDetails_thenReturnsDetailsView() throws Exception {
        UUID roomId = UUID.randomUUID();
        DormResourceService.RoomDetailDto mockRoomDetails = new DormResourceService.RoomDetailDto(roomId, "101", 4, "MALE", UUID.randomUUID(), "Test Building");

        // 关键修正: 确保我们 mock 的是 Controller 实际调用的 getRoomDetailById 方法
        when(dormResourceService.getRoomDetailById(roomId)).thenReturn(mockRoomDetails);

        // 这个 mock 保持不变，是正确的
        when(dormResourceService.getBedsForRoom(roomId)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/view/admin/dorms/rooms/" + roomId + "/details"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dorm/room-details"))
                .andExpect(model().attributeExists("room"))
                .andExpect(model().attribute("room", mockRoomDetails)) // 我们可以更进一步，验证 model 里的对象就是我们 mock 的那个
                .andExpect(model().attributeExists("beds"));
    }

    @Test
    @DisplayName("管理员提交新建房间表单应成功并重定向")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void whenCreateRoom_thenRedirects() throws Exception {
        UUID buildingId = UUID.randomUUID();
        when(dormResourceService.createRoom(any(DormDtos.RoomCreateUpdateDto.class)))
                .thenReturn(new DormDtos.DormRoomDto(UUID.randomUUID(), buildingId, "101", 4, "MALE"));

        mockMvc.perform(post("/view/admin/dorms/rooms/create")
                        .contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED)
                        .param("buildingId", buildingId.toString())
                        .param("roomNumber", "101")
                        .param("capacity", "4")
                        .param("genderType", "MALE")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/view/admin/dorms/rooms"));

        verify(dormResourceService, times(1)).createRoom(any(DormDtos.RoomCreateUpdateDto.class));
    }


    @Test
    @DisplayName("管理员批量创建床位应成功并重定向回详情页")
    @WithMockUser(roles = "ADMIN")
    void whenCreateBedsBatch_thenRedirectsToDetails() throws Exception {
        UUID roomId = UUID.randomUUID();

        mockMvc.perform(post("/view/admin/dorms/rooms/" + roomId + "/beds/create-batch")
                        .contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED)
                        .param("bedCount", "4")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/view/admin/dorms/rooms/" + roomId + "/details"));

        verify(dormResourceService, times(1)).createBedsForRoom(eq(roomId), any(DormDtos.BedCreateRequestDto.class));
    }
}