package com.openisle.dto;

import com.openisle.model.PostType;
import com.openisle.model.PostVisibleScopeType;
import com.openisle.model.TreeholeExpectedVisibility;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * Request body for creating or updating a post.
 */
@Data
public class PostRequest {

  private Long categoryId;
  private String title;
  private String content;
  private List<Long> tagIds;
  private String captcha;

  // optional for lottery posts
  private PostType type;
  private PostVisibleScopeType postVisibleScopeType;
  private TreeholeExpectedVisibility treeholeExpectedVisibility;
  private String prizeDescription;
  private String prizeIcon;
  private Integer prizeCount;
  private Integer pointCost;
  private LocalDateTime startTime;
  private LocalDateTime endTime;
  // fields for poll posts
  private List<String> options;
  private Boolean multiple;

  // fields for category proposal posts
  private String proposedName;
  private String proposalDescription;

  // campus forum fields
  private Boolean anonymous;
  private Boolean fleaMarket;
  private BigDecimal fleaPrice;
  private String fleaTradeLocation;
  private String fleaContact;
}
