// src/test/java/com/smartdorm/backend/controller/SupportFeaturesViewControllerTest.java
package com.smartdorm.backend.controller;

import com.smartdorm.backend.dto.SupportDtos;
import com.smartdorm.backend.entity.User;
import com.smartdorm.backend.repository.UserRepository;
import com.smartdorm.backend.service.SupportService;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.Optional;
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
@DisplayName("TDD for Support and Content View Controllers")
public class SupportFeaturesViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SupportService supportService;

    @MockBean
    private UserRepository userRepository;

    private final User mockAdmin = new User();
    private final User mockStudent = new User();

    @BeforeEach
    void setUp() {
        mockAdmin.setId(UUID.randomUUID());
        mockAdmin.setStudentId("admin");

        mockStudent.setId(UUID.randomUUID());
        mockStudent.setStudentId("student");

        when(userRepository.findByStudentId("admin")).thenReturn(Optional.of(mockAdmin));
        when(userRepository.findByStudentId("student")).thenReturn(Optional.of(mockStudent));
    }

    // --- Admin-Side Tests ---

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("[Admin] GET /swap-requests - Should display list of swap requests")
    void admin_listSwapRequests_shouldReturnListView() throws Exception {
        when(supportService.getAllSwapRequests()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/view/admin/support/swap-requests"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/support/swap-requests-list"))
                .andExpect(model().attributeExists("requests"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("[Admin] POST /swap-requests/{id}/process - Should process request and redirect")
    void admin_processSwapRequest_shouldRedirect() throws Exception {
        UUID requestId = UUID.randomUUID();
        mockMvc.perform(post("/view/admin/support/swap-requests/" + requestId + "/process")
                        .param("status", "APPROVED")
                        .param("adminComment", "OK")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/view/admin/support/swap-requests"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(supportService, times(1)).processSwapRequest(eq(requestId), any(SupportDtos.SwapRequestUpdateDto.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("[Admin] GET /articles/new - Should show the article creation form")
    void admin_showNewArticleForm_shouldReturnForm() throws Exception {
        mockMvc.perform(get("/view/admin/support/articles/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/support/article-form"))
                .andExpect(model().attributeExists("articleDto"));
    }

    @Test
    @WithMockUser(username="admin", roles = "ADMIN")
    @DisplayName("[Admin] POST /articles/create - Should create article and redirect")
    void admin_createArticle_shouldRedirect() throws Exception {
        mockMvc.perform(post("/view/admin/support/articles/create")
                        .param("title", "Test Title")
                        .param("category", "Test Category")
                        .param("content", "Test Content")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/view/admin/support/articles"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(supportService, times(1)).createArticle(any(SupportDtos.ArticleCreateDto.class), any(User.class));
    }


    // --- Student-Side Tests ---

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("[Student] GET /request-swap - Should show the swap request form")
    void student_showSwapRequestForm_shouldReturnForm() throws Exception {
        mockMvc.perform(get("/view/student/support/request-swap"))
                .andExpect(status().isOk())
                .andExpect(view().name("student/support/swap-request-form"))
                .andExpect(model().attributeExists("swapRequestDto"));
    }

    @Test
    @WithMockUser(username="student", roles = "STUDENT")
    @DisplayName("[Student] POST /request-swap - Should submit request and redirect")
    void student_submitSwapRequest_shouldRedirectToDashboard() throws Exception {
        mockMvc.perform(post("/view/student/support/request-swap")
                        .param("reason", "I need to change my room.")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/view/student/dashboard"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(supportService, times(1)).createSwapRequest(any(SupportDtos.SwapRequestCreateDto.class), any(User.class));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("[Student] GET /articles/{id} - Should display article details")
    void student_viewArticle_shouldReturnDetailsView() throws Exception {
        UUID articleId = UUID.randomUUID();
        SupportDtos.ArticleDto mockArticle = new SupportDtos.ArticleDto(articleId, "Title", "Content", "Cat", null, "Admin", Instant.now());
        when(supportService.getArticleById(articleId)).thenReturn(mockArticle);

        mockMvc.perform(get("/view/student/support/articles/" + articleId))
                .andExpect(status().isOk())
                .andExpect(view().name("student/support/article-details"))
                .andExpect(model().attribute("article", mockArticle));
    }

    // --- Security Test ---
    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("[Security] Student accessing admin support page should be forbidden")
    void student_accessingAdminPage_shouldBeForbidden() throws Exception {
        mockMvc.perform(get("/view/admin/support/swap-requests"))
                .andExpect(status().isForbidden());
    }
}