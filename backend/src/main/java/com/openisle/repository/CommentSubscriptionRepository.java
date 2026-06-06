package com.openisle.repository;

import com.openisle.model.Comment;
import com.openisle.model.CommentSubscription;
import com.openisle.model.User;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentSubscriptionRepository extends JpaRepository<CommentSubscription, Long> {
  List<CommentSubscription> findByComment(Comment comment);
  List<CommentSubscription> findByUser(User user);
  Optional<CommentSubscription> findByUserAndComment(User user, Comment comment);

  @Modifying
  @Query(
    value = "DELETE FROM comment_subscriptions WHERE comment_id IN (:commentIds)",
    nativeQuery = true
  )
  void deleteByComment_IdIn(@Param("commentIds") Collection<Long> commentIds);
}
