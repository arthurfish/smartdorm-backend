// src/test/java/com/smartdorm/backend/controller/StudentSurveyViewControllerTest.java
package com.smartdorm.backend.controller;

import com.smartdorm.backend.dto.StudentDtos;
import com.smartdorm.backend.entity.User;
import com.smartdorm.backend.repository.UserRepository;
import com.smartdorm.backend.service.StudentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("TDD for StudentSurveyViewController")
@WithMockUser(username = "student", roles = "STUDENT") // 模拟已登录的学生用户
public class StudentSurveyViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StudentService studentService;

    @MockBean
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        // Mock the user repository to return a user for the authenticated principal
        User mockUser = new User();
        mockUser.setStudentId("student");
        when(userRepository.findByStudentId("student")).thenReturn(Optional.of(mockUser));
    }

    @Test
    @DisplayName("GET /view/student/survey - Should display survey form")
    void whenGetSurvey_thenReturnsSurveyFormView() throws Exception {
        // Mock service layer
        StudentDtos.SurveyForStudentDto surveyDto = new StudentDtos.SurveyForStudentDto(UUID.randomUUID(), Collections.emptyList());
        when(studentService.getSurveyForStudent()).thenReturn(surveyDto);

        mockMvc.perform(get("/view/student/survey"))
                .andExpect(status().isOk())
                .andExpect(view().name("student/survey-form"))
                .andExpect(model().attributeExists("surveyDto"))
                .andExpect(model().attributeExists("submissionDto"));
    }

    @Test
    @DisplayName("POST /view/student/survey/submit - Should process form and redirect to dashboard")
    void whenPostSurvey_thenRedirectsToDashboard() throws Exception {
        mockMvc.perform(post("/view/student/survey/submit")
                        .param("answers['" + UUID.randomUUID() + "']", "1.0") // 模拟表单提交
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/view/student/dashboard"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @DisplayName("GET /view/student/result - Should display assignment result")
    void whenGetResult_thenReturnsResultView() throws Exception {
        // Mock service layer
        StudentDtos.AssignmentDetails details = new StudentDtos.AssignmentDetails("紫荆公寓", "101", 1);
        List<StudentDtos.RoommateDto> roommates = List.of(new StudentDtos.RoommateDto("李四", "20240002"));
        StudentDtos.AssignmentResultStudentDto resultDto = new StudentDtos.AssignmentResultStudentDto(details, roommates);

        when(studentService.getStudentResult(any(User.class))).thenReturn(resultDto);

        mockMvc.perform(get("/view/student/result"))
                .andExpect(status().isOk())
                .andExpect(view().name("student/result-view"))
                .andExpect(model().attributeExists("resultDto"));
    }
}