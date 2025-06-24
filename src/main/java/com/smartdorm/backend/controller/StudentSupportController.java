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
@RequestMapping("/api/student")
@PreAuthorize("hasRole('STUDENT')")
public class StudentSupportController {

    private final SupportService supportService;
    private final UserRepository userRepository;

    public StudentSupportController(SupportService supportService, UserRepository userRepository) {
        this.supportService = supportService;
        this.userRepository = userRepository;
    }

    private User getCurrentUser(UserDetails userDetails) {
        return userRepository.findByStudentId(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found in database."));
    }

    // --- Feedback ---
    @PostMapping("/feedback")
    public ResponseEntity<Void> submitFeedback(@Valid @RequestBody FeedbackCreateDto dto, @AuthenticationPrincipal UserDetails userDetails) {
        supportService.createFeedback(dto, getCurrentUser(userDetails));
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    // --- Swap Request ---
    @PostMapping("/swap-requests")
    public ResponseEntity<Void> submitSwapRequest(@Valid @RequestBody SwapRequestCreateDto dto, @AuthenticationPrincipal UserDetails userDetails) {
        supportService.createSwapRequest(dto, getCurrentUser(userDetails));
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    // --- Articles ---
    @GetMapping("/articles")
    public ResponseEntity<List<ArticleDto>> getArticles(@RequestParam(required = false) String category) {
        return ResponseEntity.ok(supportService.getArticles(category));
    }

    @GetMapping("/articles/{articleId}")
    public ResponseEntity<ArticleDto> getArticleById(@PathVariable UUID articleId) {
        return ResponseEntity.ok(supportService.getArticleById(articleId));
    }

    // --- Notifications ---
    @GetMapping("/notifications")
    public ResponseEntity<List<NotificationDto>> getNotifications(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(supportService.getNotificationsForUser(getCurrentUser(userDetails)));
    }

    @PostMapping("/notifications/{notificationId}/read")
    public ResponseEntity<Void> markNotificationAsRead(@PathVariable UUID notificationId, @AuthenticationPrincipal UserDetails userDetails) {
        supportService.markNotificationAsRead(notificationId, getCurrentUser(userDetails));
        return ResponseEntity.noContent().build();
    }
}