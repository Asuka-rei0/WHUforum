package com.openisle.service;

import com.openisle.model.TreeholeRiskLevel;

public record AiReviewResult(
  TreeholeRiskLevel riskLevel,
  String reason,
  String recommendedAction
) {
  public AiReviewResult {
    if (riskLevel == null) {
      throw new IllegalArgumentException("riskLevel is required");
    }
  }
}
