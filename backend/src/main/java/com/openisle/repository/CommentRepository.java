package com.openisle.repository;

import com.openisle.model.Comment;
import com.openisle.model.Post;
import com.openisle.model.User;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, Long> {
  List<Comment> findByPostAndParentIsNullOrderByCreatedAtAsc(Post post);
  List<Comment> findByParentOrderByCreatedAtAsc(Comment parent);
  List<Comment> findByPostAndCreatedAtLessThanOrderByCreatedAtAsc(
    Post post,
    LocalDateTime createdAt
  );
  List<Comment> findByAuthorOrderByCreatedAtDesc(User author, Pageable pageable);
  List<Comment> findByContentContainingIgnoreCase(String keyword);

  @Query(
    "SELECT c FROM Comment c WHERE c.author = :author " +
    "AND c.anonymous = false AND c.post.anonymous = false " +
    "AND c.post.status = com.openisle.model.PostStatus.PUBLISHED " +
    "AND c.post.visibleScope = com.openisle.model.PostVisibleScopeType.ALL " +
    "ORDER BY c.createdAt DESC"
  )
  List<Comment> findPublicNonAnonymousByAuthorOrderByCreatedAtDesc(
    @Param("author") User author,
    Pageable pageable
  );

  @Query(
    "SELECT c FROM Comment c WHERE LOWER(c.content) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
    "AND c.anonymous = false AND c.post.anonymous = false " +
    "AND c.post.status = com.openisle.model.PostStatus.PUBLISHED " +
    "AND c.post.visibleScope = com.openisle.model.PostVisibleScopeType.ALL"
  )
  List<Comment> findPublicNonAnonymousByContentContainingIgnoreCase(
    @Param("keyword") String keyword
  );

  @Query("SELECT DISTINCT c.author FROM Comment c WHERE c.post = :post")
  java.util.List<User> findDistinctAuthorsByPost(@Param("post") Post post);

  @Query("SELECT DISTINCT c.post.id, c.author FROM Comment c WHERE c.post.id IN :postIds")
  java.util.List<Object[]> findDistinctAuthorsByPostIds(
    @Param("postIds") java.util.List<Long> postIds
  );

  @Query("SELECT MAX(c.createdAt) FROM Comment c WHERE c.post = :post")
  java.time.LocalDateTime findLastCommentTime(@Param("post") Post post);

  @Query(
    "SELECT COUNT(c) FROM Comment c WHERE c.author.username = :username AND c.createdAt >= :start"
  )
  long countByAuthorAfter(
    @Param("username") String username,
    @Param("start") java.time.LocalDateTime start
  );

  @Query(
    "SELECT MAX(c.createdAt) FROM Comment c WHERE c.author.id = :userId " +
    "AND c.anonymous = false AND c.post.anonymous = false " +
    "AND c.post.status = com.openisle.model.PostStatus.PUBLISHED " +
    "AND c.post.visibleScope = com.openisle.model.PostVisibleScopeType.ALL"
  )
  java.time.LocalDateTime findLastCommentTimeOfUserByUserId(@Param("userId") Long userId);

  @Query("SELECT COUNT(c) FROM Comment c WHERE c.post.id = :postId")
  long countByPostId(@Param("postId") Long postId);

  long countByAuthor_Id(Long userId);

  @Query(
    "SELECT FUNCTION('date', c.createdAt) AS d, COUNT(c) AS c FROM Comment c " +
      "WHERE c.createdAt >= :start AND c.createdAt < :end GROUP BY d ORDER BY d"
  )
  java.util.List<Object[]> countDailyRange(
    @Param("start") java.time.LocalDateTime start,
    @Param("end") java.time.LocalDateTime end
  );

  @Query(value = "SELECT id FROM comments WHERE post_id = :postId", nativeQuery = true)
  List<Long> findAllIdsByPostIdIncludingDeleted(@Param("postId") Long postId);

  @Query(value = "SELECT content FROM comments WHERE post_id = :postId", nativeQuery = true)
  List<String> findAllContentsByPostIdIncludingDeleted(@Param("postId") Long postId);

  @Modifying
  @Query(
    value = "UPDATE comments SET parent_id = NULL WHERE parent_id IN (:ids)",
    nativeQuery = true
  )
  void clearParentReferences(@Param("ids") Collection<Long> ids);

  @Modifying
  @Query(value = "DELETE FROM comments WHERE id IN (:ids)", nativeQuery = true)
  void hardDeleteByIds(@Param("ids") Collection<Long> ids);
}
