package com.openisle.dto;

import lombok.Data;

@Data
public class TreeholeAuthorIdentityDto {

  private Long id;
  private String username;
  private String email;
  private String campusIdHash;
  private String campusPersonType;
  private String department;
  private boolean campusVerified;
}
