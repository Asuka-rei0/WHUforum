package com.openisle.repository;

import com.openisle.model.FleaMarketItem;
import com.openisle.model.Post;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FleaMarketItemRepository extends JpaRepository<FleaMarketItem, Long> {
  Optional<FleaMarketItem> findByPost(Post post);
  Optional<FleaMarketItem> findByPost_Id(Long postId);
  void deleteByPost(Post post);
}
