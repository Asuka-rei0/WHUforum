package com.openisle.repository;

import com.openisle.model.PollVote;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PollVoteRepository extends JpaRepository<PollVote, Long> {
  List<PollVote> findByPostId(Long postId);

  @Modifying
  @Query(value = "DELETE FROM poll_votes WHERE post_id = :postId", nativeQuery = true)
  void deleteByPost_Id(@Param("postId") Long postId);
}
