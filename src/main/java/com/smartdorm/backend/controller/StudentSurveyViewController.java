// src/main/java/com/smartdorm/backend/controller/StudentSurveyViewController.java
package com.smartdorm.backend.controller;

import com.smartdorm.backend.dto.StudentDtos;
import com.smartdorm.backend.entity.User;
import com.smartdorm.backend.exception.ResourceNotFoundException;
import com.smartdorm.backend.repository.UserRepository;
import com.smartdorm.backend.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/view/student")
@PreAuthorize("hasRole('STUDENT')")
public class StudentSurveyViewController {

    private final StudentService studentService;
    private final UserRepository userRepository;

    public StudentSurveyViewController(StudentService studentService, UserRepository userRepository) {
        this.studentService = studentService;
        this.userRepository = userRepository;
    }

    /**
     * 问卷表单 DTO (View Layer)
     * 为了方便Thymeleaf表单绑定，我们创建一个专门的视图层DTO。
     * Controller将在接收到它之后，再转换成Service层需要的DTO。
     */
    public static class SurveySubmissionDto {
        private Map<UUID, Double> answers;

        // Getters and Setters
        public Map<UUID, Double> getAnswers() { return answers; }
        public void setAnswers(Map<UUID, Double> answers) { this.answers = answers; }
    }

    @GetMapping("/survey")
    public String showSurveyForm(Model model) {
        try {
            StudentDtos.SurveyForStudentDto surveyDto = studentService.getSurveyForStudent();
            model.addAttribute("surveyDto", surveyDto);
            model.addAttribute("submissionDto", new SurveySubmissionDto());
            return "student/survey-form";
        } catch (ResourceNotFoundException e) {
            model.addAttribute("errorMessage", "当前没有开放的问卷，请稍后再试。");
            return "student/dashboard"; // 或者一个专门的错误/提示页面
        }
    }

    @PostMapping("/survey/submit")
    public String submitSurvey(@ModelAttribute SurveySubmissionDto submissionDto,
                               @AuthenticationPrincipal UserDetails userDetails,
                               RedirectAttributes redirectAttributes) {

        // 将视图层的 Map<UUID, Double> 转换为 Service 层需要的 List<ResponseItem>
        List<StudentDtos.ResponseItem> responses = submissionDto.getAnswers().entrySet().stream()
                .map(entry -> new StudentDtos.ResponseItem(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        StudentDtos.UserResponseSubmitDto serviceDto = new StudentDtos.UserResponseSubmitDto(responses);

        User currentUser = userRepository.findByStudentId(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        studentService.submitResponses(serviceDto, currentUser);

        redirectAttributes.addFlashAttribute("successMessage", "问卷提交成功，感谢您的参与！");
        return "redirect:/view/student/dashboard";
    }

    @GetMapping("/result")
    public String showResult(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User currentUser = userRepository.findByStudentId(userDetails.getUsername())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            StudentDtos.AssignmentResultStudentDto resultDto = studentService.getStudentResult(currentUser);
            model.addAttribute("resultDto", resultDto);
            return "student/result-view";
        } catch (ResourceNotFoundException e) {
            model.addAttribute("errorMessage", "您的分配结果尚未公布，请耐心等待。");
            return "student/dashboard";
        }
    }
}