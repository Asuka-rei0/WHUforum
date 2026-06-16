package com.openisle.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(
  name = "treehole_intervention_cases",
  indexes = {
    @Index(name = "idx_treehole_cases_post", columnList = "post_id"),
    @Index(name = "idx_treehole_cases_status", columnList = "status"),
  }
)
public class TreeholeInterventionCase {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "post_id", nullable = false, unique = true)
  private Post post;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private TreeholeInterventionStatus status = TreeholeInterventionStatus.OPEN;

  @Column(length = 1000)
  private String note;

  @CreationTimestamp
  @Column(
    nullable = false,
    updatable = false,
    columnDefinition = "DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)"
  )
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(
    nullable = false,
    columnDefinition = "DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)"
  )
  private LocalDateTime updatedAt;

  @Column(name = "closed_at")
  private LocalDateTime closedAt;
}
