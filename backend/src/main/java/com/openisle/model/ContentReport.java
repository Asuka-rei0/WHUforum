package com.openisle.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(
  name = "content_reports",
  indexes = {
    @Index(name = "idx_content_reports_status", columnList = "status"),
    @Index(name = "idx_content_reports_target", columnList = "target_type,target_id"),
    @Index(name = "idx_content_reports_reporter", columnList = "reporter_id"),
  }
)
public class ContentReport {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "target_type", nullable = false, length = 32)
  private ContentReportTargetType targetType;

  @Column(name = "target_id", nullable = false)
  private Long targetId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "post_id")
  private Post post;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "comment_id")
  private Comment comment;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "message_id")
  private Message message;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "reporter_id", nullable = false)
  private User reporter;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private ContentReportReason reason = ContentReportReason.OTHER;

  @Column(length = 1000)
  private String detail;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private ContentReportStatus status = ContentReportStatus.OPEN;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "handler_id")
  private User handler;

  @Column(length = 1000)
  private String resolution;

  @Column(name = "handled_at")
  private LocalDateTime handledAt;

  @CreationTimestamp
  @Column(
    nullable = false,
    updatable = false,
    columnDefinition = "DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)"
  )
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(nullable = false, columnDefinition = "DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)")
  private LocalDateTime updatedAt;
}
