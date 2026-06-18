package com.openisle.dto;

import lombok.Data;

@Data
public class NotificationDeliveryPreferenceDto {

  private boolean siteEnabled;
  private boolean emailEnabled;
  private boolean pushEnabled;
  private String digestFrequency;
}
