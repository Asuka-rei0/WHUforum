package com.openisle.dto;

import com.openisle.model.PostStatus;
import com.openisle.model.PostType;
import com.openisle.model.PostVisibleScopeType;
import com.openisle.model.TreeholeExpectedVisibility;
import com.openisle.model.TreeholeRiskLevel;
import com.openisle.model.TreeholeReviewStatus;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * Lightweight DTO for listing posts without comments.
 */
@Data
public class PostSummaryDto {

  private Long id;
  private String title;
  private String content;
  private LocalDateTime createdAt;
  private AuthorDto author;
  private CategoryDto category;
  private List<TagDto> tags;
  private long views;
  private long commentCount;
  private PostStatus status;
  private LocalDateTime pinnedAt;
  private LocalDateTime lastReplyAt;
  private List<ReactionDto> reactions;
  private List<AuthorDto> participants;
  private boolean subscribed;
  private int reward;
  private int pointReward;
  private PostType type;
  private LotteryDto lottery;
  private PollDto poll;
  private boolean rssExcluded;
  private boolean closed;
  private PostVisibleScopeType visibleScope;
  private TreeholeExpectedVisibility treeholeExpectedVisibility;
  private TreeholeReviewStatus treeholeReviewStatus;
  private TreeholeRiskLevel treeholeRiskLevel;
  private String treeholeRiskReason;
  private String treeholeRecommendedAction;
  private LocalDateTime treeholeReviewedAt;
  private boolean anonymous;
  private String anonymousAlias;
  private boolean ownedByCurrentUser;
  private FleaMarketItemDto fleaMarketItem;
}
