package com.openisle.service;

import com.openisle.repository.ImageRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

/**
 * ImageUploader implementation backed by a local filesystem directory.
 */
public class LocalImageUploader extends ImageUploader {

  private static final String UPLOAD_DIR = "dynamic_assert/";
  private static final Logger logger = LoggerFactory.getLogger(LocalImageUploader.class);

  private final Path rootDir;
  private final String baseUrl;
  private final ExecutorService executor = Executors.newFixedThreadPool(
    2,
    new CustomizableThreadFactory("local-upload-")
  );

  public LocalImageUploader(ImageRepository imageRepository, String localDir, String baseUrl) {
    super(imageRepository, baseUrl);
    this.rootDir = Paths.get(localDir).toAbsolutePath().normalize();
    this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
  }

  @Override
  protected CompletableFuture<String> doUpload(byte[] data, String filename) {
    return CompletableFuture.supplyAsync(
      () -> {
        String objectKey = UPLOAD_DIR + randomFilename(filename);
        Path target = rootDir.resolve(objectKey).normalize();
        if (!target.startsWith(rootDir)) {
          throw new IllegalArgumentException("Invalid upload path");
        }
        try {
          Files.createDirectories(target.getParent());
          Files.write(target, data);
        } catch (IOException e) {
          throw new IllegalStateException("Failed to write uploaded file", e);
        }
        String url = baseUrl + "/" + objectKey.replace('\\', '/');
        logger.debug("Local upload successful, accessible at {}", url);
        return url;
      },
      executor
    );
  }

  @Override
  protected void deleteFromStore(String key) {
    try {
      Path target = rootDir.resolve(key).normalize();
      if (!target.startsWith(rootDir)) {
        throw new IllegalArgumentException("Invalid upload path");
      }
      Files.deleteIfExists(target);
    } catch (Exception e) {
      logger.warn("Failed to delete local image {}", key, e);
    }
  }

  private String randomFilename(String filename) {
    String ext = "";
    if (filename != null) {
      int dot = filename.lastIndexOf('.');
      if (dot != -1) {
        ext = filename.substring(dot);
      }
    }
    return UUID.randomUUID().toString().replace("-", "") + ext;
  }
}
