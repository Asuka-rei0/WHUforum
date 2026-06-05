package com.openisle.dto;

import lombok.Data;

@Data
public class CasCallbackRequest {

  private String state;
  private String ticket;
  private String inviteToken;
}
