package com.openisle.dto;

import com.openisle.model.PostStatus;
import com.openisle.model.PostVisibleScopeType;
import com.openisle.model.TreeholeInterventionAction;
import com.openisle.model.TreeholeInterventionStatus;
import com.openisle.model.TreeholeReviewStatus;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class TreeholeInterventionRecordDto {

  private Long id;
  private Long caseId;
  private Long postId;
  private String adminUsername;
  private TreeholeInterventionAction action;
  private String note;
  private TreeholeInterventionStatus fromStatus;
  private TreeholeInterventionStatus toStatus;
  private TreeholeReviewStatus fromReviewStatus;
  private TreeholeReviewStatus toReviewStatus;
  private PostStatus fromPostStatus;
  private PostStatus toPostStatus;
  private PostVisibleScopeType fromVisibleScope;
  private PostVisibleScopeType toVisibleScope;
  private LocalDateTime createdAt;
}
