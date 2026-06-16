package com.openisle.dto;

import com.openisle.model.TreeholeInterventionStatus;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class TreeholeInterventionCaseDto {

  private Long id;
  private Long postId;
  private TreeholeInterventionStatus status;
  private String note;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private LocalDateTime closedAt;
  private PostSummaryDto post;
}
