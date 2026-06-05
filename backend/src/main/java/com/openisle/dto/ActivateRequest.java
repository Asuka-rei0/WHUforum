package com.openisle.dto;

import lombok.Data;

/** Request to activate a user registration by email link. */
@Data
public class ActivateRequest {

  private String token;
}
