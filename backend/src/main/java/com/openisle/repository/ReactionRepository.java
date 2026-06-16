package com.openisle.repository;

import com.openisle.model.Comment;
import com.openisle.model.Message;
import com.openisle.model.Post;
import com.openisle.model.Reaction;
import com.openisle.model.User;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReactionRepository extends JpaRepository<Reaction, Long> {
  Optional<Reaction> findByUserAndPostAndType(
    User user,
    Post post,
    com.openisle.model.ReactionType type
  );
  Optional<Reaction> findByUserAndCommentAndType(
    User user,
    Comment comment,
    com.openisle.model.ReactionType type
  );
  Optional<Reaction> findByUserAndMessageAndType(
    User user,
    Message message,
    com.openisle.model.ReactionType type
  );
  List<Reaction> findByPost(Post post);
  List<Reaction> findByComment(Comment comment);
  List<Reaction> findByMessage(Message message);

  @Modifying
  @Query(value = "DELETE FROM reactions WHERE comment_id IN (:commentIds)", nativeQuery = true)
  void deleteByComment_IdIn(@Param("commentIds") Collection<Long> commentIds);

  @Query(
    "SELECT r.post.id FROM Reaction r WHERE r.post IS NOT NULL " +
    "AND r.post.author.username = :username " +
    "AND r.post.anonymous = false " +
    "AND r.post.status = com.openisle.model.PostStatus.PUBLISHED " +
    "AND r.post.visibleScope = com.openisle.model.PostVisibleScopeType.ALL " +
    "AND r.type = com.openisle.model.ReactionType.LIKE " +
    "GROUP BY r.post.id ORDER BY COUNT(r.id) DESC"
  )
  List<Long> findTopPostIds(@Param("username") String username, Pageable pageable);

  @Query(
    "SELECT r.comment.id FROM Reaction r WHERE r.comment IS NOT NULL " +
    "AND r.comment.author.username = :username " +
    "AND r.comment.anonymous = false " +
    "AND r.comment.post.anonymous = false " +
    "AND r.comment.post.status = com.openisle.model.PostStatus.PUBLISHED " +
    "AND r.comment.post.visibleScope = com.openisle.model.PostVisibleScopeType.ALL " +
    "AND r.type = com.openisle.model.ReactionType.LIKE " +
    "GROUP BY r.comment.id ORDER BY COUNT(r.id) DESC"
  )
  List<Long> findTopCommentIds(@Param("username") String username, Pageable pageable);

  @Query(
    "SELECT COUNT(r) FROM Reaction r WHERE r.user.username = :username AND r.type = com.openisle.model.ReactionType.LIKE"
  )
  long countLikesSent(@Param("username") String username);

  @Query(
    "SELECT COUNT(r) FROM Reaction r WHERE r.user.username = :username AND r.createdAt >= :start"
  )
  long countByUserAfter(
    @Param("username") String username,
    @Param("start") java.time.LocalDateTime start
  );

  @Query(
    """
    SELECT COUNT(DISTINCT r.id)
    FROM Reaction r
    LEFT JOIN r.post    p
    LEFT JOIN p.author  pa
    LEFT JOIN r.comment c
    LEFT JOIN c.author  ca
    WHERE r.type = com.openisle.model.ReactionType.LIKE
      AND (
           (r.post IS NOT NULL
             AND pa.username = :username
             AND p.anonymous = false
             AND p.status = com.openisle.model.PostStatus.PUBLISHED
             AND p.visibleScope = com.openisle.model.PostVisibleScopeType.ALL)
        OR (r.comment IS NOT NULL
             AND ca.username = :username
             AND c.anonymous = false
             AND c.post.anonymous = false
             AND c.post.status = com.openisle.model.PostStatus.PUBLISHED
             AND c.post.visibleScope = com.openisle.model.PostVisibleScopeType.ALL)
      )
    """
  )
  long countLikesReceived(@Param("username") String username);

  @Query(
    """
    SELECT COUNT(r) FROM Reaction r
    LEFT JOIN r.post p
    LEFT JOIN r.comment c
    WHERE (p IS NOT NULL
             AND p.author.username = :username
             AND p.anonymous = false
             AND p.status = com.openisle.model.PostStatus.PUBLISHED
             AND p.visibleScope = com.openisle.model.PostVisibleScopeType.ALL)
       OR (c IS NOT NULL
             AND c.author.username = :username
             AND c.anonymous = false
             AND c.post.anonymous = false
             AND c.post.status = com.openisle.model.PostStatus.PUBLISHED
             AND c.post.visibleScope = com.openisle.model.PostVisibleScopeType.ALL)
    """
  )
  long countReceived(@Param("username") String username);
}
