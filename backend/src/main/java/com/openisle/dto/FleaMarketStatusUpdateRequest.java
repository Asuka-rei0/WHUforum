package com.openisle.dto;

import com.openisle.model.FleaMarketStatus;
import lombok.Data;

@Data
public class FleaMarketStatusUpdateRequest {

  private FleaMarketStatus status;
}
