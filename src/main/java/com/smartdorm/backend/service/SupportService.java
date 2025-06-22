package com.smartdorm.backend.service;

import com.smartdorm.backend.dto.SupportDtos.*;
import com.smartdorm.backend.entity.*;
import com.smartdorm.backend.exception.DataConflictException;
import com.smartdorm.backend.exception.ResourceNotFoundException;
import com.smartdorm.backend.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class SupportService {

    private final FeedbackRepository feedbackRepository;
    private final SwapRequestRepository swapRequestRepository;
    private final ContentArticleRepository articleRepository;
    private final NotificationRepository notificationRepository;
    private final MatchingCycleRepository cycleRepository;

    public SupportService(FeedbackRepository feedbackRepository, SwapRequestRepository swapRequestRepository, ContentArticleRepository articleRepository, NotificationRepository notificationRepository, MatchingCycleRepository cycleRepository) {
        this.feedbackRepository = feedbackRepository;
        this.swapRequestRepository = swapRequestRepository;
        this.articleRepository = articleRepository;
        this.notificationRepository = notificationRepository;
        this.cycleRepository = cycleRepository;
    }

    // --- Helper to find the latest completed or processing cycle ---
    private MatchingCycle findLatestActiveCycle() {
        return cycleRepository.findAll().stream()
                .filter(c -> "COMPLETED".equals(c.getStatus()) || "PROCESSING".equals(c.getStatus()))
                .max((c1, c2) -> c1.getCreatedAt().compareTo(c2.getCreatedAt()))
                .orElseThrow(() -> new ResourceNotFoundException("No active or completed cycle found."));
    }

    // --- Feedback Logic ---
    public void createFeedback(FeedbackCreateDto dto, User currentUser) {
        MatchingCycle cycle = findLatestActiveCycle();
        Feedback feedback = new Feedback();
        feedback.setCycle(cycle);
        feedback.setUser(currentUser);
        feedback.setAnonymous(dto.isAnonymous());
        feedback.setRating(dto.rating());
        feedback.setComment(dto.comment());
        feedbackRepository.save(feedback);
    }

    // --- Swap Request Logic ---
    public void createSwapRequest(SwapRequestCreateDto dto, User currentUser) {
        MatchingCycle cycle = findLatestActiveCycle();
        SwapRequest request = new SwapRequest();
        request.setUser(currentUser);
        request.setCycle(cycle);
        request.setReason(dto.reason());
        swapRequestRepository.save(request);
    }

    public List<SwapRequestDto> getAllSwapRequests() {
        return swapRequestRepository.findAll().stream()
                .map(this::mapToSwapRequestDto)
                .collect(Collectors.toList());
    }

    public SwapRequestDto processSwapRequest(UUID requestId, SwapRequestUpdateDto dto) {
        SwapRequest request = swapRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Swap request not found with id: " + requestId));
        request.setStatus(dto.status());
        request.setAdminComment(dto.adminComment());
        return mapToSwapRequestDto(swapRequestRepository.save(request));
    }

    // --- Article Logic ---
    public ArticleDto createArticle(ArticleCreateDto dto, User adminUser) {
        ContentArticle article = new ContentArticle();
        article.setTitle(dto.title());
        article.setContent(dto.content());
        article.setCategory(dto.category());
        article.setAuthor(adminUser);
        return mapToArticleDto(articleRepository.save(article));
    }

    public ArticleDto updateArticle(UUID articleId, ArticleUpdateDto dto) {
        ContentArticle article = articleRepository.findById(articleId)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found with id: " + articleId));
        if (StringUtils.hasText(dto.title())) article.setTitle(dto.title());
        if (StringUtils.hasText(dto.content())) article.setContent(dto.content());
        if (StringUtils.hasText(dto.category())) article.setCategory(dto.category());
        return mapToArticleDto(articleRepository.save(article));
    }

    public void deleteArticle(UUID articleId) {
        if (!articleRepository.existsById(articleId)) {
            throw new ResourceNotFoundException("Article not found with id: " + articleId);
        }
        articleRepository.deleteById(articleId);
    }

    public List<ArticleDto> getArticles(String category) {
        List<ContentArticle> articles;
        if (StringUtils.hasText(category)) {
            articles = articleRepository.findByCategory(category);
        } else {
            articles = articleRepository.findAll();
        }
        return articles.stream().map(this::mapToArticleDto).collect(Collectors.toList());
    }



    public ArticleDto getArticleById(UUID articleId) {
        return articleRepository.findById(articleId)
                .map(this::mapToArticleDto)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found with id: " + articleId));
    }

    // --- Notification Logic ---
    public List<NotificationDto> getNotificationsForUser(User currentUser) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId()).stream()
                .map(this::mapToNotificationDto)
                .collect(Collectors.toList());
    }

    public void markNotificationAsRead(UUID notificationId, User currentUser) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));
        if (!notification.getUser().getId().equals(currentUser.getId())) {
            throw new DataConflictException("User does not have permission to read this notification.");
        }
        notification.setRead(true);
        notificationRepository.save(notification);
    }


    // --- Mappers ---
    private SwapRequestDto mapToSwapRequestDto(SwapRequest req) {
        return new SwapRequestDto(req.getId(), req.getUser().getId(), req.getUser().getName(), req.getCycle().getId(), req.getReason(), req.getStatus(), req.getAdminComment(), req.getCreatedAt());
    }

    private ArticleDto mapToArticleDto(ContentArticle article) {
        User author = article.getAuthor();
        return new ArticleDto(article.getId(), article.getTitle(), article.getContent(), article.getCategory(),
                author != null ? author.getId() : null,
                author != null ? author.getName() : "System",
                article.getCreatedAt());
    }

    private NotificationDto mapToNotificationDto(Notification notif) {
        return new NotificationDto(notif.getId(), notif.getMessage(), notif.getLinkUrl(), notif.isRead(), notif.getCreatedAt());
    }
}