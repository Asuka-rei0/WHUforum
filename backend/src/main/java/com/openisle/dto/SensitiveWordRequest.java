package com.openisle.dto;

import lombok.Data;

@Data
public class SensitiveWordRequest {

  private String word;
  private boolean crisis;
  private boolean enabled = true;
}
