package com.openisle.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class SensitiveWordDto {

  private Long id;
  private String word;
  private boolean crisis;
  private boolean enabled;
  private LocalDateTime createdAt;
}
