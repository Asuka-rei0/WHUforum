package com.openisle.dto;

import com.openisle.model.TreeholeInterventionAction;
import lombok.Data;

@Data
public class TreeholeInterventionActionRequest {

  private TreeholeInterventionAction action;
  private String note;
}
