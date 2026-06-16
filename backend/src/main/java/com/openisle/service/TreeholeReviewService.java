package com.openisle.service;

import com.openisle.event.TreeholeReviewRequestedEvent;
import com.openisle.model.NotificationType;
import com.openisle.model.Post;
import com.openisle.model.PostStatus;
import com.openisle.model.PostType;
import com.openisle.model.PostVisibleScopeType;
import com.openisle.model.Role;
import com.openisle.model.TreeholeExpectedVisibility;
import com.openisle.model.TreeholeReviewStatus;
import com.openisle.repository.PostRepository;
import com.openisle.repository.UserRepository;
import com.openisle.search.SearchIndexEventPublisher;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Service
@RequiredArgsConstructor
public class TreeholeReviewService {

  private final PostRepository postRepository;
  private final UserRepository userRepository;
  private final AiReviewClient aiReviewClient;
  private final NotificationService notificationService;
  private final SearchIndexEventPublisher searchIndexEventPublisher;
  private final TreeholeInterventionService treeholeInterventionService;

  @Value("${app.treehole.ai-review.enabled:true}")
  private boolean aiReviewEnabled;

  @Async("treeholeReviewExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Transactional
  public void handleTreeholeReviewRequested(TreeholeReviewRequestedEvent event) {
    if (!aiReviewEnabled) {
      log.debug("Treehole AI review disabled for post {}", event.postId());
      return;
    }
    postRepository.findById(event.postId()).ifPresent(this::reviewPost);
  }

  @Async("treeholeReviewExecutor")
  @EventListener(ApplicationReadyEvent.class)
  @Transactional
  public void recoverPendingTreeholeReviews() {
    if (!aiReviewEnabled) {
      return;
    }
    List<Post> pendingReviews =
      postRepository.findByTypeAndStatusAndTreeholeReviewStatusInAndTreeholeReviewedAtIsNull(
        PostType.TREEHOLE,
        PostStatus.PENDING,
        List.of(TreeholeReviewStatus.AI_REVIEWING, TreeholeReviewStatus.PRIVATE)
      );
    if (pendingReviews.isEmpty()) {
      return;
    }
    log.info("Recovering {} pending treehole AI review(s)", pendingReviews.size());
    pendingReviews.forEach(this::reviewPost);
  }

  private void reviewPost(Post post) {
    if (!isReviewableTreehole(post)) {
      return;
    }
    try {
      AiReviewResult result = aiReviewClient.assessTreehole(
        new AiReviewRequest(post.getId(), post.getTitle(), post.getContent())
      );
      applyReviewResult(post, result);
    } catch (Exception e) {
      log.warn("Treehole AI review failed for post {}: {}", post.getId(), e.getMessage());
      applyReviewFailure(post, e);
    }
  }

  private boolean isReviewableTreehole(Post post) {
    if (post.getType() != PostType.TREEHOLE || post.getStatus() != PostStatus.PENDING) {
      return false;
    }
    if (post.getTreeholeReviewedAt() != null) {
      return false;
    }
    return (
      post.getTreeholeReviewStatus() == TreeholeReviewStatus.AI_REVIEWING ||
      post.getTreeholeReviewStatus() == TreeholeReviewStatus.PRIVATE
    );
  }

  private void applyReviewResult(Post post, AiReviewResult result) {
    post.setTreeholeRiskLevel(result.riskLevel());
    post.setTreeholeRiskReason(result.reason());
    post.setTreeholeRecommendedAction(result.recommendedAction());
    post.setTreeholeReviewedAt(LocalDateTime.now());

    switch (result.riskLevel()) {
      case L0, L1 -> applyLowRiskResult(post);
      case L2 -> applyAdminReviewResult(post, result);
      case L3 -> applyRestrictedResult(post, TreeholeReviewStatus.PUBLIC_RESTRICTED, result);
      case L4 -> applyRestrictedResult(post, TreeholeReviewStatus.REPORTED, result);
    }
  }

  private void applyLowRiskResult(Post post) {
    if (post.getTreeholeExpectedVisibility() == TreeholeExpectedVisibility.PUBLIC) {
      post.setTreeholeReviewStatus(TreeholeReviewStatus.PUBLIC);
      post.setStatus(PostStatus.PUBLISHED);
      post.setVisibleScope(PostVisibleScopeType.ALL);
      Post saved = postRepository.save(post);
      searchIndexEventPublisher.publishPostSaved(saved);
      return;
    }
    post.setTreeholeReviewStatus(TreeholeReviewStatus.PRIVATE);
    post.setStatus(PostStatus.PENDING);
    post.setVisibleScope(PostVisibleScopeType.ONLY_ME);
    postRepository.save(post);
  }

  private void applyAdminReviewResult(Post post, AiReviewResult result) {
    post.setTreeholeReviewStatus(TreeholeReviewStatus.ADMIN_REVIEWING);
    post.setStatus(PostStatus.PENDING);
    post.setVisibleScope(PostVisibleScopeType.ONLY_ME);
    Post saved = postRepository.save(post);
    treeholeInterventionService.ensureCase(
      saved,
      buildAdminMessage("Treehole requires admin review", result)
    );
    notifyAdmins(
      saved,
      NotificationType.POST_REVIEW_REQUEST,
      buildAdminMessage("Treehole requires admin review", result)
    );
  }

  private void applyRestrictedResult(
    Post post,
    TreeholeReviewStatus reviewStatus,
    AiReviewResult result
  ) {
    post.setTreeholeReviewStatus(reviewStatus);
    post.setStatus(PostStatus.PENDING);
    post.setVisibleScope(PostVisibleScopeType.ONLY_ME);
    Post saved = postRepository.save(post);
    searchIndexEventPublisher.publishPostDeleted(saved.getId());
    treeholeInterventionService.ensureCase(
      saved,
      buildAdminMessage("Treehole restricted and reported", result)
    );
    notifyAdmins(
      saved,
      NotificationType.MODERATION_ALERT,
      buildAdminMessage("Treehole restricted and reported", result)
    );
  }

  private void applyReviewFailure(Post post, Exception exception) {
    post.setTreeholeReviewStatus(TreeholeReviewStatus.ADMIN_REVIEWING);
    post.setStatus(PostStatus.PENDING);
    post.setVisibleScope(PostVisibleScopeType.ONLY_ME);
    post.setTreeholeRiskReason(truncate("AI review failed: " + exception.getMessage(), 1000));
    post.setTreeholeRecommendedAction("Admin manual review required");
    post.setTreeholeReviewedAt(LocalDateTime.now());
    Post saved = postRepository.save(post);
    treeholeInterventionService.ensureCase(saved, "Treehole AI review failed");
    notifyAdmins(saved, NotificationType.POST_REVIEW_REQUEST, "Treehole AI review failed");
  }

  private void notifyAdmins(Post post, NotificationType type, String content) {
    userRepository
      .findByRole(Role.ADMIN)
      .forEach(admin ->
        notificationService.createNotification(
          admin,
          type,
          post,
          null,
          null,
          post.getAuthor(),
          null,
          content
        )
      );
  }

  private String buildAdminMessage(String prefix, AiReviewResult result) {
    return truncate(
      prefix +
        ": risk=" +
        result.riskLevel() +
        ", reason=" +
        nullToEmpty(result.reason()) +
        ", action=" +
        nullToEmpty(result.recommendedAction()),
      1000
    );
  }

  private String truncate(String value, int maxLength) {
    if (value == null) {
      return null;
    }
    return value.length() <= maxLength ? value : value.substring(0, maxLength);
  }

  private String nullToEmpty(String value) {
    return value == null ? "" : value;
  }
}
