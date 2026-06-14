package com.openisle.service;

import com.openisle.event.TreeholeReviewRequestedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TreeholeReviewService {

  @EventListener
  public void handleTreeholeReviewRequested(TreeholeReviewRequestedEvent event) {
    log.debug(
      "Treehole review requested for post {}, expectedVisibility={}, reviewStatus={}, crisis={}",
      event.postId(),
      event.expectedVisibility(),
      event.initialReviewStatus(),
      event.crisisFlagged()
    );
  }
}
