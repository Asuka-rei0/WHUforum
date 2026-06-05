package com.openisle.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class AnonymousAuditDto {

  private Long id;
  private Long postId;
  private Long commentId;
  private String alias;
  private String realUsername;
  private String campusIdHash;
  private String campusPersonType;
  private String department;
  private String reason;
  private LocalDateTime createdAt;
}
