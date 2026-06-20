package com.openisle.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.openisle.dto.ContentReportActionRequest;
import com.openisle.exception.NotFoundException;
import com.openisle.mapper.ContentReportMapper;
import com.openisle.model.*;
import com.openisle.repository.*;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ContentReportServiceTest {

  private ContentReportRepository contentReportRepository;
  private UserRepository userRepository;
  private PostRepository postRepository;
  private CommentRepository commentRepository;
  private MessageRepository messageRepository;
  private MessageParticipantRepository messageParticipantRepository;
  private NotificationService notificationService;
  private ContentReportMapper contentReportMapper;
  private ContentReportService service;

  @BeforeEach
  void setUp() {
    contentReportRepository = mock(ContentReportRepository.class);
    userRepository = mock(UserRepository.class);
    postRepository = mock(PostRepository.class);
    commentRepository = mock(CommentRepository.class);
    messageRepository = mock(MessageRepository.class);
    messageParticipantRepository = mock(MessageParticipantRepository.class);
    notificationService = mock(NotificationService.class);
    contentReportMapper = mock(ContentReportMapper.class);
    service = new ContentReportService(
      contentReportRepository,
      userRepository,
      postRepository,
      commentRepository,
      messageRepository,
      messageParticipantRepository,
      notificationService,
      contentReportMapper
    );
  }

  @Test
  void updateStatusToResolvedNotifiesReporter() {
    User reporter = user("alice");
    User admin = user("admin");
    Post post = post(9L);
    ContentReport report = report(5L, reporter, post, ContentReportStatus.OPEN);

    when(contentReportRepository.findById(5L)).thenReturn(Optional.of(report));
    when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
    when(contentReportRepository.save(any(ContentReport.class))).thenAnswer(
      invocation -> invocation.getArgument(0)
    );

    ContentReportActionRequest request = new ContentReportActionRequest();
    request.setStatus(ContentReportStatus.RESOLVED);
    request.setResolution("已删除违规内容");

    ContentReport updated = service.updateStatus(5L, "admin", request);

    assertEquals(ContentReportStatus.RESOLVED, updated.getStatus());
    assertNotNull(updated.getHandledAt());

    ArgumentCaptor<Boolean> approvedCaptor = ArgumentCaptor.forClass(Boolean.class);
    verify(notificationService)
      .createNotification(
        eq(reporter),
        eq(NotificationType.CONTENT_REPORT_REVIEWED),
        eq(post),
        isNull(),
        approvedCaptor.capture(),
        eq(admin),
        isNull(),
        eq("举报 #5 POST/9，处理说明：已删除违规内容")
      );
    assertTrue(approvedCaptor.getValue());
  }

  @Test
  void updateStatusToDismissedNotifiesReporterAsRejected() {
    User reporter = user("alice");
    User admin = user("admin");
    Post post = post(9L);
    ContentReport report = report(6L, reporter, post, ContentReportStatus.REVIEWING);

    when(contentReportRepository.findById(6L)).thenReturn(Optional.of(report));
    when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
    when(contentReportRepository.save(any(ContentReport.class))).thenAnswer(
      invocation -> invocation.getArgument(0)
    );

    ContentReportActionRequest request = new ContentReportActionRequest();
    request.setStatus(ContentReportStatus.DISMISSED);

    service.updateStatus(6L, "admin", request);

    verify(notificationService)
      .createNotification(
        eq(reporter),
        eq(NotificationType.CONTENT_REPORT_REVIEWED),
        eq(post),
        isNull(),
        eq(false),
        eq(admin),
        isNull(),
        eq("举报 #6 POST/9")
      );
  }

  @Test
  void updateStatusToReviewingDoesNotNotifyReporter() {
    User reporter = user("alice");
    User admin = user("admin");
    ContentReport report = report(7L, reporter, post(1L), ContentReportStatus.OPEN);

    when(contentReportRepository.findById(7L)).thenReturn(Optional.of(report));
    when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
    when(contentReportRepository.save(any(ContentReport.class))).thenAnswer(
      invocation -> invocation.getArgument(0)
    );

    ContentReportActionRequest request = new ContentReportActionRequest();
    request.setStatus(ContentReportStatus.REVIEWING);

    service.updateStatus(7L, "admin", request);

    verify(notificationService, never())
      .createNotification(
        eq(reporter),
        eq(NotificationType.CONTENT_REPORT_REVIEWED),
        any(),
        any(),
        any(),
        any(),
        any(),
        any()
      );
  }

  @Test
  void updateStatusFromResolvedToDismissedDoesNotNotifyAgain() {
    User reporter = user("alice");
    User admin = user("admin");
    ContentReport report = report(8L, reporter, post(1L), ContentReportStatus.RESOLVED);

    when(contentReportRepository.findById(8L)).thenReturn(Optional.of(report));
    when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
    when(contentReportRepository.save(any(ContentReport.class))).thenAnswer(
      invocation -> invocation.getArgument(0)
    );

    ContentReportActionRequest request = new ContentReportActionRequest();
    request.setStatus(ContentReportStatus.DISMISSED);

    service.updateStatus(8L, "admin", request);

    verifyNoInteractions(notificationService);
  }

  @Test
  void updateStatusThrowsWhenReportMissing() {
    when(contentReportRepository.findById(99L)).thenReturn(Optional.empty());

    assertThrows(
      NotFoundException.class,
      () -> service.updateStatus(99L, "admin", new ContentReportActionRequest())
    );
  }

  private User user(String username) {
    User user = new User();
    user.setId((long) username.hashCode());
    user.setUsername(username);
    return user;
  }

  private Post post(Long id) {
    Post post = new Post();
    post.setId(id);
    post.setTitle("test post");
    return post;
  }

  private ContentReport report(
    Long id,
    User reporter,
    Post post,
    ContentReportStatus status
  ) {
    ContentReport report = new ContentReport();
    report.setId(id);
    report.setReporter(reporter);
    report.setPost(post);
    report.setTargetType(ContentReportTargetType.POST);
    report.setTargetId(post.getId());
    report.setStatus(status);
    return report;
  }
}
