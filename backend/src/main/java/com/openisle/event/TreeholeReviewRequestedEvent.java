package com.openisle.event;

import com.openisle.model.TreeholeExpectedVisibility;
import com.openisle.model.TreeholeReviewStatus;

public record TreeholeReviewRequestedEvent(
  Long postId,
  Long authorId,
  TreeholeExpectedVisibility expectedVisibility,
  TreeholeReviewStatus initialReviewStatus,
  boolean moderationFlagged,
  boolean crisisFlagged,
  String matchedWord
) {}
