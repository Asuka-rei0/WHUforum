package com.openisle.dto;

import java.util.List;
import lombok.Data;

@Data
public class TreeholeInterventionDetailDto {

  private TreeholeInterventionCaseDto caseInfo;
  private PostSummaryDto post;
  private List<TreeholeInterventionRecordDto> records;
}
