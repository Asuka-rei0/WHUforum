package com.openisle.repository;

import com.openisle.model.SensitiveWord;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SensitiveWordRepository extends JpaRepository<SensitiveWord, Long> {
  List<SensitiveWord> findByEnabledTrue();
  Optional<SensitiveWord> findByWordIgnoreCase(String word);
}
