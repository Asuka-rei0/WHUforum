package com.openisle.repository;

import com.openisle.model.AnonymousAudit;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnonymousAuditRepository extends JpaRepository<AnonymousAudit, Long> {
  Optional<AnonymousAudit> findByPost_IdAndCommentIsNull(Long postId);
  Optional<AnonymousAudit> findByComment_Id(Long commentId);
  List<AnonymousAudit> findByPost_IdOrderByCreatedAtAsc(Long postId);
}
