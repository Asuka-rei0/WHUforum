package com.openisle.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.openisle.event.TreeholeReviewRequestedEvent;
import com.openisle.model.NotificationType;
import com.openisle.model.Post;
import com.openisle.model.PostStatus;
import com.openisle.model.PostType;
import com.openisle.model.PostVisibleScopeType;
import com.openisle.model.Role;
import com.openisle.model.TreeholeExpectedVisibility;
import com.openisle.model.TreeholeRiskLevel;
import com.openisle.model.TreeholeReviewStatus;
import com.openisle.model.User;
import com.openisle.repository.PostRepository;
import com.openisle.repository.UserRepository;
import com.openisle.search.SearchIndexEventPublisher;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class TreeholeReviewServiceTest {

  private PostRepository postRepository;
  private UserRepository userRepository;
  private AiReviewClient aiReviewClient;
  private NotificationService notificationService;
  private SearchIndexEventPublisher searchIndexEventPublisher;
  private TreeholeReviewService service;

  @BeforeEach
  void setUp() {
    postRepository = mock(PostRepository.class);
    userRepository = mock(UserRepository.class);
    aiReviewClient = mock(AiReviewClient.class);
    notificationService = mock(NotificationService.class);
    searchIndexEventPublisher = mock(SearchIndexEventPublisher.class);
    service = new TreeholeReviewService(
      postRepository,
      userRepository,
      aiReviewClient,
      notificationService,
      searchIndexEventPublisher
    );
    ReflectionTestUtils.setField(service, "aiReviewEnabled", true);
    when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  void publicTreeholeLowRiskPublishesPost() {
    Post post = treehole(TreeholeExpectedVisibility.PUBLIC, TreeholeReviewStatus.AI_REVIEWING);
    when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));
    when(aiReviewClient.assessTreehole(any()))
      .thenReturn(new AiReviewResult(TreeholeRiskLevel.L0, "normal", "publish"));

    service.handleTreeholeReviewRequested(eventFor(post));

    assertEquals(PostStatus.PUBLISHED, post.getStatus());
    assertEquals(PostVisibleScopeType.ALL, post.getVisibleScope());
    assertEquals(TreeholeReviewStatus.PUBLIC, post.getTreeholeReviewStatus());
    assertEquals(TreeholeRiskLevel.L0, post.getTreeholeRiskLevel());
    assertEquals("normal", post.getTreeholeRiskReason());
    assertEquals("publish", post.getTreeholeRecommendedAction());
    assertNotNull(post.getTreeholeReviewedAt());
    verify(searchIndexEventPublisher).publishPostSaved(post);
    verifyNoInteractions(notificationService);
  }

  @Test
  void privateTreeholeLowRiskStaysPrivate() {
    Post post = treehole(TreeholeExpectedVisibility.ONLY_ME, TreeholeReviewStatus.PRIVATE);
    when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));
    when(aiReviewClient.assessTreehole(any()))
      .thenReturn(new AiReviewResult(TreeholeRiskLevel.L1, "low", "keep private"));

    service.handleTreeholeReviewRequested(eventFor(post));

    assertEquals(PostStatus.PENDING, post.getStatus());
    assertEquals(PostVisibleScopeType.ONLY_ME, post.getVisibleScope());
    assertEquals(TreeholeReviewStatus.PRIVATE, post.getTreeholeReviewStatus());
    assertEquals(TreeholeRiskLevel.L1, post.getTreeholeRiskLevel());
    verify(searchIndexEventPublisher, never()).publishPostSaved(any());
    verifyNoInteractions(notificationService);
  }

  @Test
  void mediumRiskGoesToAdminReview() {
    Post post = treehole(TreeholeExpectedVisibility.PUBLIC, TreeholeReviewStatus.AI_REVIEWING);
    User admin = admin();
    when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));
    when(userRepository.findByRole(Role.ADMIN)).thenReturn(List.of(admin));
    when(aiReviewClient.assessTreehole(any()))
      .thenReturn(new AiReviewResult(TreeholeRiskLevel.L2, "distress", "admin review"));

    service.handleTreeholeReviewRequested(eventFor(post));

    assertEquals(PostStatus.PENDING, post.getStatus());
    assertEquals(PostVisibleScopeType.ONLY_ME, post.getVisibleScope());
    assertEquals(TreeholeReviewStatus.ADMIN_REVIEWING, post.getTreeholeReviewStatus());
    verify(notificationService).createNotification(
      eq(admin),
      eq(NotificationType.POST_REVIEW_REQUEST),
      eq(post),
      isNull(),
      isNull(),
      eq(post.getAuthor()),
      isNull(),
      contains("risk=L2")
    );
  }

  @Test
  void highRiskIsRestrictedAndReported() {
    Post post = treehole(TreeholeExpectedVisibility.PUBLIC, TreeholeReviewStatus.AI_REVIEWING);
    User admin = admin();
    when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));
    when(userRepository.findByRole(Role.ADMIN)).thenReturn(List.of(admin));
    when(aiReviewClient.assessTreehole(any()))
      .thenReturn(new AiReviewResult(TreeholeRiskLevel.L3, "high", "restrict"));

    service.handleTreeholeReviewRequested(eventFor(post));

    assertEquals(PostStatus.PENDING, post.getStatus());
    assertEquals(PostVisibleScopeType.ONLY_ME, post.getVisibleScope());
    assertEquals(TreeholeReviewStatus.PUBLIC_RESTRICTED, post.getTreeholeReviewStatus());
    verify(searchIndexEventPublisher).publishPostDeleted(post.getId());
    verify(notificationService).createNotification(
      eq(admin),
      eq(NotificationType.MODERATION_ALERT),
      eq(post),
      isNull(),
      isNull(),
      eq(post.getAuthor()),
      isNull(),
      contains("risk=L3")
    );
  }

  @Test
  void urgentRiskIsReported() {
    Post post = treehole(TreeholeExpectedVisibility.PUBLIC, TreeholeReviewStatus.AI_REVIEWING);
    User admin = admin();
    when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));
    when(userRepository.findByRole(Role.ADMIN)).thenReturn(List.of(admin));
    when(aiReviewClient.assessTreehole(any()))
      .thenReturn(new AiReviewResult(TreeholeRiskLevel.L4, "urgent", "report"));

    service.handleTreeholeReviewRequested(eventFor(post));

    assertEquals(TreeholeReviewStatus.REPORTED, post.getTreeholeReviewStatus());
    assertEquals(PostStatus.PENDING, post.getStatus());
    verify(notificationService).createNotification(
      eq(admin),
      eq(NotificationType.MODERATION_ALERT),
      eq(post),
      isNull(),
      isNull(),
      eq(post.getAuthor()),
      isNull(),
      contains("risk=L4")
    );
  }

  @Test
  void aiFailureFallsBackToAdminReview() {
    Post post = treehole(TreeholeExpectedVisibility.PUBLIC, TreeholeReviewStatus.AI_REVIEWING);
    User admin = admin();
    when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));
    when(userRepository.findByRole(Role.ADMIN)).thenReturn(List.of(admin));
    when(aiReviewClient.assessTreehole(any())).thenThrow(new IllegalStateException("timeout"));

    service.handleTreeholeReviewRequested(eventFor(post));

    assertEquals(PostStatus.PENDING, post.getStatus());
    assertEquals(PostVisibleScopeType.ONLY_ME, post.getVisibleScope());
    assertEquals(TreeholeReviewStatus.ADMIN_REVIEWING, post.getTreeholeReviewStatus());
    assertTrue(post.getTreeholeRiskReason().contains("timeout"));
    assertEquals("Admin manual review required", post.getTreeholeRecommendedAction());
    verify(searchIndexEventPublisher, never()).publishPostSaved(any());
    verify(notificationService).createNotification(
      eq(admin),
      eq(NotificationType.POST_REVIEW_REQUEST),
      eq(post),
      isNull(),
      isNull(),
      eq(post.getAuthor()),
      isNull(),
      eq("Treehole AI review failed")
    );
  }

  @Test
  void disabledReviewDoesNotTouchPost() {
    ReflectionTestUtils.setField(service, "aiReviewEnabled", false);
    Post post = treehole(TreeholeExpectedVisibility.PUBLIC, TreeholeReviewStatus.AI_REVIEWING);

    service.handleTreeholeReviewRequested(eventFor(post));

    verifyNoInteractions(postRepository, aiReviewClient, notificationService);
  }

  private Post treehole(
    TreeholeExpectedVisibility visibility,
    TreeholeReviewStatus reviewStatus
  ) {
    User author = new User();
    author.setId(10L);
    author.setUsername("author");
    author.setRole(Role.USER);

    Post post = new Post();
    post.setId(100L);
    post.setTitle("Treehole");
    post.setContent("Content");
    post.setAuthor(author);
    post.setType(PostType.TREEHOLE);
    post.setStatus(PostStatus.PENDING);
    post.setVisibleScope(PostVisibleScopeType.ONLY_ME);
    post.setTreeholeExpectedVisibility(visibility);
    post.setTreeholeReviewStatus(reviewStatus);
    return post;
  }

  private TreeholeReviewRequestedEvent eventFor(Post post) {
    return new TreeholeReviewRequestedEvent(
      post.getId(),
      post.getAuthor().getId(),
      post.getTreeholeExpectedVisibility(),
      post.getTreeholeReviewStatus(),
      false,
      false,
      null
    );
  }

  private User admin() {
    User admin = new User();
    admin.setId(1L);
    admin.setUsername("admin");
    admin.setRole(Role.ADMIN);
    return admin;
  }
}
