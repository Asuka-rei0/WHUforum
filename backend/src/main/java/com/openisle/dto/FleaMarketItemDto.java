package com.openisle.dto;

import com.openisle.model.FleaMarketStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class FleaMarketItemDto {

  private Long id;
  private BigDecimal price;
  private String tradeLocation;
  private String contact;
  private FleaMarketStatus status;
  private AuthorDto buyer;
  private LocalDateTime updatedAt;
}
