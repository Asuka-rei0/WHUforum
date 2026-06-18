package com.openisle.dto;

import com.openisle.model.ContentReportReason;
import com.openisle.model.ContentReportTargetType;
import lombok.Data;

@Data
public class ContentReportRequest {

  private ContentReportTargetType targetType;
  private Long targetId;
  private ContentReportReason reason;
  private String detail;
}
