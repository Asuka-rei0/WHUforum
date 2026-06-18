package com.openisle.dto;

import lombok.Data;

@Data
public class AdminDashboardStatsDto {

  private long users;
  private long posts;
  private long comments;
  private long openReports;
  private long reviewingReports;
  private long pendingPosts;
  private long openTreeholeCases;
}
