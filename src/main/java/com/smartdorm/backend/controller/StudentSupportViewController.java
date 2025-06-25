// src/main/java/com/smartdorm/backend/controller/StudentSupportViewController.java
package com.smartdorm.backend.controller;

import com.smartdorm.backend.dto.SupportDtos;
import com.smartdorm.backend.entity.User;
import com.smartdorm.backend.exception.ResourceNotFoundException;
import com.smartdorm.backend.repository.UserRepository;
import com.smartdorm.backend.service.SupportService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("/view/student/support")
@PreAuthorize("hasRole('STUDENT')")
public class StudentSupportViewController {

    private final SupportService supportService;
    private final UserRepository userRepository;

    public StudentSupportViewController(SupportService supportService, UserRepository userRepository) {
        this.supportService = supportService;
        this.userRepository = userRepository;
    }

    private User getCurrentUser(UserDetails userDetails) {
        return userRepository.findByStudentId(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found in database."));
    }

    // --- Swap Request ---
    @GetMapping("/request-swap")
    public String showSwapRequestForm(Model model) {
        model.addAttribute("swapRequestDto", new SupportDtos.SwapRequestCreateDto(""));
        return "student/support/swap-request-form";
    }

    @PostMapping("/request-swap")
    public String submitSwapRequest(@Valid @ModelAttribute("swapRequestDto") SupportDtos.SwapRequestCreateDto dto,
                                    BindingResult result,
                                    @AuthenticationPrincipal UserDetails userDetails,
                                    RedirectAttributes redirectAttributes, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("swapRequestDto", dto);
            return "student/support/swap-request-form";
        }
        try {
            supportService.createSwapRequest(dto, getCurrentUser(userDetails));
            redirectAttributes.addFlashAttribute("successMessage", "您的调宿申请已提交，请耐心等待管理员审批。");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "提交失败: " + e.getMessage());
        }
        return "redirect:/view/student/dashboard";
    }

    // --- Article Viewing ---
    @GetMapping("/articles")
    public String listArticlesForStudent(@RequestParam(required = false) String category, Model model) {
        model.addAttribute("articles", supportService.getArticles(category));
        model.addAttribute("categories", supportService.getArticleCategories()); // Assuming you add this method
        return "student/support/articles-list";
    }

    @GetMapping("/articles/{id}")
    public String viewArticle(@PathVariable UUID id, Model model) {
        try {
            model.addAttribute("article", supportService.getArticleById(id));
            return "student/support/article-details";
        } catch (ResourceNotFoundException e) {
            return "redirect:/view/student/support/articles";
        }
    }
}