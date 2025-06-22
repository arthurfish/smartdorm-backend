package com.smartdorm.backend.controller;

import com.smartdorm.backend.dto.SupportDtos.*;
import com.smartdorm.backend.entity.User;
import com.smartdorm.backend.exception.ResourceNotFoundException;
import com.smartdorm.backend.repository.UserRepository;
import com.smartdorm.backend.service.SupportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSupportController {

    private final SupportService supportService;
    private final UserRepository userRepository;

    public AdminSupportController(SupportService supportService, UserRepository userRepository) {
        this.supportService = supportService;
        this.userRepository = userRepository;
    }

    private User getCurrentUser(UserDetails userDetails) {
        return userRepository.findByStudentId(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found in database."));
    }

    // --- Swap Requests ---
    @GetMapping("/swap-requests")
    public ResponseEntity<List<SwapRequestDto>> getAllSwapRequests() {
        return ResponseEntity.ok(supportService.getAllSwapRequests());
    }

    @PutMapping("/swap-requests/{requestId}/process")
    public ResponseEntity<SwapRequestDto> processSwapRequest(
            @PathVariable UUID requestId,
            @Valid @RequestBody SwapRequestUpdateDto dto) {
        return ResponseEntity.ok(supportService.processSwapRequest(requestId, dto));
    }

    // --- Articles ---
    @PostMapping("/articles")
    public ResponseEntity<ArticleDto> createArticle(@Valid @RequestBody ArticleCreateDto dto, @AuthenticationPrincipal UserDetails userDetails) {
        ArticleDto createdArticle = supportService.createArticle(dto, getCurrentUser(userDetails));
        return new ResponseEntity<>(createdArticle, HttpStatus.CREATED);
    }

    @GetMapping("/articles")
    public ResponseEntity<List<ArticleDto>> getAllArticles() {
        return ResponseEntity.ok(supportService.getArticles(null));
    }

    @GetMapping("/articles/{articleId}")
    public ResponseEntity<ArticleDto> getArticleById(@PathVariable UUID articleId) {
        return ResponseEntity.ok(supportService.getArticleById(articleId));
    }

    @PutMapping("/articles/{articleId}")
    public ResponseEntity<ArticleDto> updateArticle(
            @PathVariable UUID articleId,
            @Valid @RequestBody ArticleUpdateDto dto) {
        return ResponseEntity.ok(supportService.updateArticle(articleId, dto));
    }

    @DeleteMapping("/articles/{articleId}")
    public ResponseEntity<Void> deleteArticle(@PathVariable UUID articleId) {
        supportService.deleteArticle(articleId);
        return ResponseEntity.noContent().build();
    }
}