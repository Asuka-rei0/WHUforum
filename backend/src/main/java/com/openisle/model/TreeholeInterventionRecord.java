package com.openisle.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(
  name = "treehole_intervention_records",
  indexes = {
    @Index(name = "idx_treehole_records_case", columnList = "case_id"),
    @Index(name = "idx_treehole_records_post", columnList = "post_id"),
    @Index(name = "idx_treehole_records_action", columnList = "action"),
    @Index(name = "idx_treehole_records_created_at", columnList = "created_at"),
  }
)
public class TreeholeInterventionRecord {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "case_id", nullable = false)
  private TreeholeInterventionCase interventionCase;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "post_id", nullable = false)
  private Post post;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "admin_id")
  private User admin;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private TreeholeInterventionAction action;

  @Column(length = 1000)
  private String note;

  @Enumerated(EnumType.STRING)
  @Column(name = "from_status", length = 32)
  private TreeholeInterventionStatus fromStatus;

  @Enumerated(EnumType.STRING)
  @Column(name = "to_status", length = 32)
  private TreeholeInterventionStatus toStatus;

  @Enumerated(EnumType.STRING)
  @Column(name = "from_review_status", length = 32)
  private TreeholeReviewStatus fromReviewStatus;

  @Enumerated(EnumType.STRING)
  @Column(name = "to_review_status", length = 32)
  private TreeholeReviewStatus toReviewStatus;

  @Enumerated(EnumType.STRING)
  @Column(name = "from_post_status", length = 32)
  private PostStatus fromPostStatus;

  @Enumerated(EnumType.STRING)
  @Column(name = "to_post_status", length = 32)
  private PostStatus toPostStatus;

  @Enumerated(EnumType.STRING)
  @Column(name = "from_visible_scope", length = 32)
  private PostVisibleScopeType fromVisibleScope;

  @Enumerated(EnumType.STRING)
  @Column(name = "to_visible_scope", length = 32)
  private PostVisibleScopeType toVisibleScope;

  @CreationTimestamp
  @Column(
    name = "created_at",
    nullable = false,
    updatable = false,
    columnDefinition = "DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)"
  )
  private LocalDateTime createdAt;
}
