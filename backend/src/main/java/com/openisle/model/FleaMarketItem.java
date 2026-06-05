package com.openisle.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
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
@Table(name = "campus_flea_market_items")
public class FleaMarketItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "post_id", nullable = false, unique = true)
  private Post post;

  @Column(precision = 10, scale = 2)
  private BigDecimal price;

  @Column(length = 200)
  private String tradeLocation;

  @Column(length = 200)
  private String contact;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private FleaMarketStatus status = FleaMarketStatus.AVAILABLE;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "buyer_id")
  private User buyer;

  @Version
  private Long version;

  @CreationTimestamp
  @Column(
    nullable = false,
    updatable = false,
    columnDefinition = "DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)"
  )
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(columnDefinition = "DATETIME(6)")
  private LocalDateTime updatedAt;
}
