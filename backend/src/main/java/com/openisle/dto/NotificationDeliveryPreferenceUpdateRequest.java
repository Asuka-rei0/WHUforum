package com.openisle.dto;

import lombok.Data;

@Data
public class NotificationDeliveryPreferenceUpdateRequest {

  private Boolean siteEnabled;
  private Boolean emailEnabled;
  private Boolean pushEnabled;
  private String digestFrequency;
}
