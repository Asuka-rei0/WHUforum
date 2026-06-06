package com.openisle.repository;

import com.openisle.model.AnonymousAudit;
import com.openisle.model.Comment;
import com.openisle.model.Post;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnonymousAuditRepository extends JpaRepository<AnonymousAudit, Long> {
  Optional<AnonymousAudit> findByPost_IdAndCommentIsNull(Long postId);
  Optional<AnonymousAudit> findByComment_Id(Long commentId);
  List<AnonymousAudit> findByPost_IdOrderByCreatedAtAsc(Long postId);
  void deleteByPost(Post post);
  void deleteByComment(Comment comment);

  @Modifying
  @Query(
    value = "DELETE FROM anonymous_audits WHERE comment_id IN (:commentIds)",
    nativeQuery = true
  )
  void deleteByComment_IdIn(@Param("commentIds") Collection<Long> commentIds);
}
