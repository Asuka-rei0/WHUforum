package com.openisle.service;

import com.openisle.model.SensitiveWord;
import com.openisle.repository.SensitiveWordRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ModerationService {

  private final SensitiveWordRepository sensitiveWordRepository;

  @Value("${app.moderation.blocked-words:}")
  private String blockedWords;

  @Value("${app.moderation.crisis-words:}")
  private String crisisWords;

  public ModerationResult inspect(String text) {
    String normalized = text == null ? "" : text.toLowerCase(Locale.ROOT);
    for (SensitiveWord word : sensitiveWordRepository.findByEnabledTrue()) {
      if (matches(normalized, word.getWord())) {
        return new ModerationResult(true, word.isCrisis(), word.getWord());
      }
    }
    for (String word : split(blockedWords)) {
      if (matches(normalized, word)) {
        return new ModerationResult(true, false, word);
      }
    }
    for (String word : split(crisisWords)) {
      if (matches(normalized, word)) {
        return new ModerationResult(true, true, word);
      }
    }
    return ModerationResult.clean();
  }

  private boolean matches(String normalized, String word) {
    return (
      word != null && !word.isBlank() && normalized.contains(word.trim().toLowerCase(Locale.ROOT))
    );
  }

  private List<String> split(String words) {
    if (words == null || words.isBlank()) {
      return List.of();
    }
    return Arrays.stream(words.split(","))
      .map(String::trim)
      .filter(s -> !s.isBlank())
      .toList();
  }

  public record ModerationResult(boolean flagged, boolean crisis, String matchedWord) {
    public static ModerationResult clean() {
      return new ModerationResult(false, false, null);
    }
  }
}
