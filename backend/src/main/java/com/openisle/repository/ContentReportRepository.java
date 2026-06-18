package com.openisle.repository;

import com.openisle.model.ContentReport;
import com.openisle.model.ContentReportStatus;
import com.openisle.model.ContentReportTargetType;
import com.openisle.model.User;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentReportRepository extends JpaRepository<ContentReport, Long> {
  boolean existsByReporterAndTargetTypeAndTargetIdAndStatusIn(
    User reporter,
    ContentReportTargetType targetType,
    Long targetId,
    Collection<ContentReportStatus> statuses
  );

  List<ContentReport> findByStatusOrderByCreatedAtDesc(
    ContentReportStatus status,
    Pageable pageable
  );

  List<ContentReport> findAllByOrderByCreatedAtDesc(Pageable pageable);

  long countByStatus(ContentReportStatus status);
}
