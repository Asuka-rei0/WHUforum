package com.openisle.service;

import com.openisle.dto.SensitiveWordDto;
import com.openisle.dto.SensitiveWordRequest;
import com.openisle.model.SensitiveWord;
import com.openisle.repository.SensitiveWordRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SensitiveWordService {

  private final SensitiveWordRepository sensitiveWordRepository;

  public List<SensitiveWordDto> list() {
    return sensitiveWordRepository.findAll().stream().map(this::toDto).toList();
  }

  public SensitiveWordDto upsert(SensitiveWordRequest request) {
    if (request.getWord() == null || request.getWord().isBlank()) {
      throw new IllegalArgumentException("Sensitive word is required");
    }
    SensitiveWord word = sensitiveWordRepository
      .findByWordIgnoreCase(request.getWord().trim())
      .orElseGet(SensitiveWord::new);
    word.setWord(request.getWord().trim());
    word.setCrisis(request.isCrisis());
    word.setEnabled(request.isEnabled());
    return toDto(sensitiveWordRepository.save(word));
  }

  public void delete(Long id) {
    sensitiveWordRepository.deleteById(id);
  }

  private SensitiveWordDto toDto(SensitiveWord word) {
    SensitiveWordDto dto = new SensitiveWordDto();
    dto.setId(word.getId());
    dto.setWord(word.getWord());
    dto.setCrisis(word.isCrisis());
    dto.setEnabled(word.isEnabled());
    dto.setCreatedAt(word.getCreatedAt());
    return dto;
  }
}
