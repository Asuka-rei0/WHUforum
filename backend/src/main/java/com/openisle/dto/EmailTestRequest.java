package com.openisle.dto;

import lombok.Data;

/** Request to send an admin email delivery test. */
@Data
public class EmailTestRequest {

  private String to;
}
