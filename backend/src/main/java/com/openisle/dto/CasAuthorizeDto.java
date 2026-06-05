package com.openisle.dto;

import lombok.Data;

@Data
public class CasAuthorizeDto {

  private String authorizationUrl;
  private String state;
  private boolean mock;
}
