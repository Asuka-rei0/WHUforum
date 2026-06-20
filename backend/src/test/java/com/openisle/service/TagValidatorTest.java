package com.openisle.service;

import static org.junit.jupiter.api.Assertions.*;

import com.openisle.exception.FieldException;
import org.junit.jupiter.api.Test;

class TagValidatorTest {

  private final TagValidator validator = new TagValidator();

  @Test
  void rejectsEmptyName() {
    assertThrows(FieldException.class, () -> validator.validate(""));
    assertThrows(FieldException.class, () -> validator.validate(null));
    assertThrows(FieldException.class, () -> validator.validate("   "));
  }

  @Test
  void allowsLettersNumbersAndChinese() {
    assertDoesNotThrow(() -> validator.validate("tag1"));
    assertDoesNotThrow(() -> validator.validate("校园标签"));
    assertDoesNotThrow(() -> validator.validate("WHU2026"));
  }

  @Test
  void rejectsSpecialCharacters() {
    FieldException ex = assertThrows(FieldException.class, () -> validator.validate("tag-name"));
    assertEquals("name", ex.getField());
    assertEquals(
      "Tag name must be letters, numbers, or Chinese characters",
      ex.getMessage()
    );
  }
}
