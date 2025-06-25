// src/main/java/com/smartdorm/backend/controller/AdminSupportViewController.java
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
@RequestMapping("/view/admin/support")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSupportViewController {

    private final SupportService supportService;
    private final UserRepository userRepository;

    public AdminSupportViewController(SupportService supportService, UserRepository userRepository) {
        this.supportService = supportService;
        this.userRepository = userRepository;
    }

    private User getCurrentUser(UserDetails userDetails) {
        return userRepository.findByStudentId(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found in database."));
    }

    // --- Swap Request Views ---
    @GetMapping("/swap-requests")
    public String listSwapRequests(Model model) {
        model.addAttribute("requests", supportService.getAllSwapRequests());
        return "admin/support/swap-requests-list";
    }

    @PostMapping("/swap-requests/{id}/process")
    public String processSwapRequest(@PathVariable UUID id,
                                     @RequestParam String status,
                                     @RequestParam(required = false) String adminComment,
                                     RedirectAttributes redirectAttributes) {
        try {
            SupportDtos.SwapRequestUpdateDto updateDto = new SupportDtos.SwapRequestUpdateDto(status, adminComment);
            supportService.processSwapRequest(id, updateDto);
            redirectAttributes.addFlashAttribute("successMessage", "申请已成功处理！");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "处理失败: " + e.getMessage());
        }
        return "redirect:/view/admin/support/swap-requests";
    }

    // --- Article Views ---
    @GetMapping("/articles")
    public String listArticles(Model model) {
        model.addAttribute("articles", supportService.getArticles(null));
        return "admin/support/articles-list";
    }

    @GetMapping("/articles/new")
    public String showNewArticleForm(Model model) {
        if (!model.containsAttribute("articleDto")) {
            model.addAttribute("articleDto", new SupportDtos.ArticleCreateDto("", "", ""));
        }
        model.addAttribute("pageTitle", "发布新文章");
        return "admin/support/article-form";
    }

    @PostMapping("/articles/create")
    public String createArticle(@Valid @ModelAttribute("articleDto") SupportDtos.ArticleCreateDto dto,
                                BindingResult result,
                                RedirectAttributes redirectAttributes,
                                @AuthenticationPrincipal UserDetails userDetails) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.articleDto", result);
            redirectAttributes.addFlashAttribute("articleDto", dto);
            return "redirect:/view/admin/support/articles/new";
        }
        supportService.createArticle(dto, getCurrentUser(userDetails));
        redirectAttributes.addFlashAttribute("successMessage", "文章 '" + dto.title() + "' 已成功发布！");
        return "redirect:/view/admin/support/articles";
    }

    @GetMapping("/articles/{id}/edit")
    public String showEditArticleForm(@PathVariable UUID id, Model model) {
        if (!model.containsAttribute("articleDto")) {
            SupportDtos.ArticleDto existingArticle = supportService.getArticleById(id);
            SupportDtos.ArticleUpdateDto updateDto = new SupportDtos.ArticleUpdateDto(existingArticle.title(), existingArticle.content(), existingArticle.category());
            model.addAttribute("articleDto", updateDto);
        }
        model.addAttribute("articleId", id);
        model.addAttribute("pageTitle", "编辑文章");
        return "admin/support/article-form";
    }

    @PostMapping("/articles/{id}/update")
    public String updateArticle(@PathVariable UUID id,
                                @Valid @ModelAttribute("articleDto") SupportDtos.ArticleUpdateDto dto,
                                BindingResult result,
                                RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.articleDto", result);
            redirectAttributes.addFlashAttribute("articleDto", dto);
            return "redirect:/view/admin/support/articles/" + id + "/edit";
        }
        supportService.updateArticle(id, dto);
        redirectAttributes.addFlashAttribute("successMessage", "文章更新成功！");
        return "redirect:/view/admin/support/articles";
    }

    @PostMapping("/articles/{id}/delete")
    public String deleteArticle(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            supportService.deleteArticle(id);
            redirectAttributes.addFlashAttribute("successMessage", "文章已成功删除。");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "删除失败: " + e.getMessage());
        }
        return "redirect:/view/admin/support/articles";
    }
}