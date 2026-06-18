package com.openisle.dto;

import com.openisle.model.ContentReportStatus;
import lombok.Data;

@Data
public class ContentReportActionRequest {

  private ContentReportStatus status;
  private String resolution;
}
