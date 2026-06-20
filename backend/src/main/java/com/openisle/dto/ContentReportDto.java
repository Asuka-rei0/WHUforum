package com.openisle.dto;

import com.openisle.model.ContentReportReason;
import com.openisle.model.ContentReportStatus;
import com.openisle.model.ContentReportTargetType;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ContentReportDto {

  private Long id;
  private ContentReportTargetType targetType;
  private Long targetId;
  private ContentReportReason reason;
  private String detail;
  private ContentReportStatus status;
  private String reporterUsername;
  private Long reporterId;
  private String handlerUsername;
  private String resolution;
  private String targetTitle;
  private String targetExcerpt;
  private Long postId;
  private Long commentId;
  private Long messageId;
  private Long conversationId;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private LocalDateTime handledAt;
}
