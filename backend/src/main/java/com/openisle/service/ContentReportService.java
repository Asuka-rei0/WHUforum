package com.openisle.service;

import com.openisle.dto.ContentReportActionRequest;
import com.openisle.dto.ContentReportRequest;
import com.openisle.exception.NotFoundException;
import com.openisle.mapper.ContentReportMapper;
import com.openisle.model.*;
import com.openisle.repository.*;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ContentReportService {

  private static final int MAX_DETAIL_LENGTH = 1000;
  private static final int MAX_RESOLUTION_LENGTH = 1000;
  private static final EnumSet<ContentReportStatus> ACTIVE_STATUSES = EnumSet.of(
    ContentReportStatus.OPEN,
    ContentReportStatus.REVIEWING
  );

  private final ContentReportRepository contentReportRepository;
  private final UserRepository userRepository;
  private final PostRepository postRepository;
  private final CommentRepository commentRepository;
  private final MessageRepository messageRepository;
  private final MessageParticipantRepository messageParticipantRepository;
  private final NotificationService notificationService;
  private final ContentReportMapper contentReportMapper;

  @Transactional
  public ContentReport create(String username, ContentReportRequest request) {
    if (request == null || request.getTargetType() == null || request.getTargetId() == null) {
      throw new IllegalArgumentException("Report target is required");
    }
    User reporter = userRepository
      .findByUsername(username)
      .orElseThrow(() -> new NotFoundException("User not found"));
    if (
      contentReportRepository.existsByReporterAndTargetTypeAndTargetIdAndStatusIn(
        reporter,
        request.getTargetType(),
        request.getTargetId(),
        ACTIVE_STATUSES
      )
    ) {
      throw new IllegalArgumentException("You have already reported this content");
    }

    ContentReport report = new ContentReport();
    report.setReporter(reporter);
    report.setTargetType(request.getTargetType());
    report.setTargetId(request.getTargetId());
    report.setReason(
      request.getReason() != null ? request.getReason() : ContentReportReason.OTHER
    );
    report.setDetail(trimToLimit(request.getDetail(), MAX_DETAIL_LENGTH));
    attachTarget(report, reporter);
    ContentReport saved = contentReportRepository.save(report);
    notifyAdmins(saved);
    return saved;
  }

  public List<ContentReport> list(ContentReportStatus status, int page, int pageSize) {
    PageRequest pageable = PageRequest.of(Math.max(page, 0), clampPageSize(pageSize));
    if (status == null) {
      return contentReportRepository.findAllByOrderByCreatedAtDesc(pageable);
    }
    return contentReportRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
  }

  @Transactional
  public ContentReport updateStatus(Long id, String adminUsername, ContentReportActionRequest req) {
    ContentReport report = contentReportRepository
      .findById(id)
      .orElseThrow(() -> new NotFoundException("Report not found"));
    User admin = userRepository
      .findByUsername(adminUsername)
      .orElseThrow(() -> new NotFoundException("Admin not found"));
    ContentReportStatus previousStatus = report.getStatus();
    ContentReportStatus status = req != null && req.getStatus() != null
      ? req.getStatus()
      : ContentReportStatus.REVIEWING;
    report.setStatus(status);
    report.setHandler(admin);
    if (req != null && req.getResolution() != null) {
      report.setResolution(trimToLimit(req.getResolution(), MAX_RESOLUTION_LENGTH));
    }
    if (status == ContentReportStatus.RESOLVED || status == ContentReportStatus.DISMISSED) {
      report.setHandledAt(LocalDateTime.now());
    }
    ContentReport saved = contentReportRepository.save(report);
    if (isTerminalReviewStatus(status) && !isTerminalReviewStatus(previousStatus)) {
      notifyReporter(saved, status, admin);
    }
    return saved;
  }

  public long countOpen() {
    return contentReportRepository.countByStatus(ContentReportStatus.OPEN);
  }

  public long countReviewing() {
    return contentReportRepository.countByStatus(ContentReportStatus.REVIEWING);
  }

  public com.openisle.dto.ContentReportDto toDto(ContentReport report) {
    return contentReportMapper.toDto(report);
  }

  private void attachTarget(ContentReport report, User reporter) {
    switch (report.getTargetType()) {
      case POST -> {
        Post post = postRepository
          .findById(report.getTargetId())
          .orElseThrow(() -> new NotFoundException("Post not found"));
        report.setPost(post);
      }
      case TREEHOLE -> {
        Post post = postRepository
          .findById(report.getTargetId())
          .orElseThrow(() -> new NotFoundException("Treehole not found"));
        if (post.getType() != PostType.TREEHOLE) {
          throw new IllegalArgumentException("Target is not a treehole");
        }
        report.setPost(post);
      }
      case COMMENT -> {
        Comment comment = commentRepository
          .findById(report.getTargetId())
          .orElseThrow(() -> new NotFoundException("Comment not found"));
        report.setComment(comment);
        report.setPost(comment.getPost());
      }
      case MESSAGE -> {
        Message message = messageRepository
          .findById(report.getTargetId())
          .orElseThrow(() -> new NotFoundException("Message not found"));
        boolean canReport = messageParticipantRepository
          .findByConversationIdAndUserId(message.getConversation().getId(), reporter.getId())
          .isPresent();
        if (!canReport) {
          throw new IllegalArgumentException("You cannot report this message");
        }
        report.setMessage(message);
      }
    }
  }

  private void notifyAdmins(ContentReport report) {
    String content =
      "Content report #" +
      report.getId() +
      " " +
      report.getTargetType() +
      "/" +
      report.getTargetId() +
      " reason=" +
      report.getReason();
    for (User admin : userRepository.findByRole(Role.ADMIN)) {
      notificationService.createNotification(
        admin,
        NotificationType.CONTENT_REPORT,
        report.getPost(),
        report.getComment(),
        null,
        report.getReporter(),
        null,
        content
      );
    }
  }

  private void notifyReporter(ContentReport report, ContentReportStatus status, User admin) {
    User reporter = report.getReporter();
    if (reporter == null) {
      return;
    }
    Boolean approved = status == ContentReportStatus.RESOLVED ? Boolean.TRUE : Boolean.FALSE;
    notificationService.createNotification(
      reporter,
      NotificationType.CONTENT_REPORT_REVIEWED,
      report.getPost(),
      report.getComment(),
      approved,
      admin,
      null,
      buildReporterNotificationContent(report)
    );
  }

  private String buildReporterNotificationContent(ContentReport report) {
    StringBuilder content = new StringBuilder();
    content
      .append("举报 #")
      .append(report.getId())
      .append(" ")
      .append(report.getTargetType())
      .append("/")
      .append(report.getTargetId());
    if (StringUtils.hasText(report.getResolution())) {
      content.append("，处理说明：").append(report.getResolution().trim());
    }
    return content.toString();
  }

  private boolean isTerminalReviewStatus(ContentReportStatus status) {
    return status == ContentReportStatus.RESOLVED || status == ContentReportStatus.DISMISSED;
  }

  private String trimToLimit(String value, int limit) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.length() > limit ? trimmed.substring(0, limit) : trimmed;
  }

  private int clampPageSize(int pageSize) {
    if (pageSize < 1) {
      return 20;
    }
    return Math.min(pageSize, 100);
  }
}
