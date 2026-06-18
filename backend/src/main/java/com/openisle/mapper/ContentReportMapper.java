package com.openisle.mapper;

import com.openisle.dto.ContentReportDto;
import com.openisle.model.ContentReport;
import org.springframework.stereotype.Component;

@Component
public class ContentReportMapper {

  private static final int EXCERPT_LIMIT = 160;

  public ContentReportDto toDto(ContentReport report) {
    ContentReportDto dto = new ContentReportDto();
    dto.setId(report.getId());
    dto.setTargetType(report.getTargetType());
    dto.setTargetId(report.getTargetId());
    dto.setReason(report.getReason());
    dto.setDetail(report.getDetail());
    dto.setStatus(report.getStatus());
    dto.setResolution(report.getResolution());
    dto.setCreatedAt(report.getCreatedAt());
    dto.setUpdatedAt(report.getUpdatedAt());
    dto.setHandledAt(report.getHandledAt());
    if (report.getReporter() != null) {
      dto.setReporterId(report.getReporter().getId());
      dto.setReporterUsername(report.getReporter().getUsername());
    }
    if (report.getHandler() != null) {
      dto.setHandlerUsername(report.getHandler().getUsername());
    }
    if (report.getPost() != null) {
      dto.setPostId(report.getPost().getId());
      dto.setTargetTitle(report.getPost().getTitle());
      dto.setTargetExcerpt(excerpt(report.getPost().getContent()));
    }
    if (report.getComment() != null) {
      dto.setCommentId(report.getComment().getId());
      dto.setPostId(report.getComment().getPost().getId());
      dto.setTargetTitle(report.getComment().getPost().getTitle());
      dto.setTargetExcerpt(excerpt(report.getComment().getContent()));
    }
    if (report.getMessage() != null) {
      dto.setMessageId(report.getMessage().getId());
      dto.setTargetTitle("Private message #" + report.getMessage().getId());
      dto.setTargetExcerpt(excerpt(report.getMessage().getContent()));
    }
    return dto;
  }

  private String excerpt(String content) {
    if (content == null) {
      return "";
    }
    String normalized = content.replaceAll("\\s+", " ").trim();
    if (normalized.length() <= EXCERPT_LIMIT) {
      return normalized;
    }
    return normalized.substring(0, EXCERPT_LIMIT);
  }
}
