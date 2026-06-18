package com.openisle.repository;

import com.openisle.model.ContentReport;
import com.openisle.model.ContentReportStatus;
import com.openisle.model.ContentReportTargetType;
import com.openisle.model.User;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

  @Modifying
  @Query(
    value = """
      UPDATE content_reports
      SET post_id = NULL, comment_id = NULL
      WHERE post_id = :postId
         OR comment_id IN (SELECT id FROM comments WHERE post_id = :postId)
      """,
    nativeQuery = true
  )
  void detachPostReferences(@Param("postId") Long postId);
}
