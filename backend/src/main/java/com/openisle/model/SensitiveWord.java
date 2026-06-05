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
@Table(name = "sensitive_words")
public class SensitiveWord {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 120)
  private String word;

  @Column(nullable = false)
  private boolean crisis = false;

  @Column(nullable = false)
  private boolean enabled = true;

  @CreationTimestamp
  @Column(
    nullable = false,
    updatable = false,
    columnDefinition = "DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)"
  )
  private LocalDateTime createdAt;
}
