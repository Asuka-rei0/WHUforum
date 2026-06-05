package com.openisle.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.openisle.repository.ImageRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalImageUploaderTest {

  @TempDir
  Path tempDir;

  @Test
  void uploadWritesFileAndReturnsUrl() {
    ImageRepository repo = mock(ImageRepository.class);
    LocalImageUploader uploader = new LocalImageUploader(
      repo,
      tempDir.toString(),
      "http://localhost:8080/uploads"
    );

    String url = uploader.upload("data".getBytes(), "img.png").join();

    assertTrue(url.matches("http://localhost:8080/uploads/dynamic_assert/[a-f0-9]{32}\\.png"));
    String key = url.substring("http://localhost:8080/uploads/".length());
    assertTrue(Files.exists(tempDir.resolve(key)));
  }
}
