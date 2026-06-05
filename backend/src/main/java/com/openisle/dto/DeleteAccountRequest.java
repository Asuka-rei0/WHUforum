package com.openisle.dto;

import lombok.Data;

/** Request to delete the current user account. */
@Data
public class DeleteAccountRequest {

  private String password;
}
