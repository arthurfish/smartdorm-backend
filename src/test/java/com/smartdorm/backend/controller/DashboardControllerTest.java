package com.smartdorm.backend.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("TDD for DashboardController")
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Admin用户访问/view/admin/dashboard应成功并返回管理员面板")
    @WithMockUser(username = "test-admin", roles = "ADMIN")
    void whenAdminAccessesDashboard_thenReturnsAdminDashboardView() throws Exception {
        mockMvc.perform(get("/view/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"))
                .andExpect(content().string(containsString("管理员主面板")));
    }

    @Test
    @DisplayName("Student用户访问/view/student/dashboard应成功并返回学生面板")
    @WithMockUser(username = "test-student", roles = "STUDENT")
    void whenStudentAccessesDashboard_thenReturnsStudentDashboardView() throws Exception {
        mockMvc.perform(get("/view/student/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("student/dashboard"))
                .andExpect(content().string(containsString("学生主面板")));
    }

    @Test
    @DisplayName("Student用户访问管理员面板应返回403 Forbidden")
    @WithMockUser(username = "test-student", roles = "STUDENT")
    void whenStudentAccessesAdminDashboard_thenIsForbidden() throws Exception {
        mockMvc.perform(get("/view/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("未认证用户访问任何面板都应重定向到登录页")
    void whenUnauthenticatedAccessDashboard_thenRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/view/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }
}