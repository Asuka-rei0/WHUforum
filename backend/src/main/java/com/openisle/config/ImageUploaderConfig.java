package com.openisle.config;

import com.openisle.repository.ImageRepository;
import com.openisle.service.CosImageUploader;
import com.openisle.service.ImageUploader;
import com.openisle.service.LocalImageUploader;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class ImageUploaderConfig {

  private static final Logger logger = LoggerFactory.getLogger(ImageUploaderConfig.class);

  @Bean
  @ConditionalOnMissingBean(ImageUploader.class)
  public ImageUploader imageUploader(ImageRepository imageRepository, Environment env) {
    String provider = env
      .getProperty("app.upload.provider", "auto")
      .trim()
      .toLowerCase(Locale.ROOT);

    if ("local".equals(provider)) {
      return localUploader(imageRepository, env);
    }
    if ("cos".equals(provider)) {
      assertCosConfigured(env);
      return cosUploader(imageRepository, env);
    }
    if (!"auto".equals(provider)) {
      throw new IllegalArgumentException("Unsupported upload provider: " + provider);
    }
    if (isCosConfigured(env)) {
      return cosUploader(imageRepository, env);
    }

    logger.warn("COS upload is not fully configured; using local filesystem image uploader.");
    return localUploader(imageRepository, env);
  }

  private ImageUploader cosUploader(ImageRepository imageRepository, Environment env) {
    return new CosImageUploader(
      imageRepository,
      env.getProperty("cos.secret-id", ""),
      env.getProperty("cos.secret-key", ""),
      env.getProperty("cos.region", "ap-guangzhou"),
      env.getProperty("cos.bucket-name", ""),
      env.getProperty("cos.base-url", "https://example.com")
    );
  }

  private ImageUploader localUploader(ImageRepository imageRepository, Environment env) {
    String serverPort = env.getProperty("server.port", "8080");
    return new LocalImageUploader(
      imageRepository,
      env.getProperty("app.upload.local-dir", "uploads"),
      env.getProperty("app.upload.local-base-url", "http://localhost:" + serverPort + "/uploads")
    );
  }

  private void assertCosConfigured(Environment env) {
    if (!isCosConfigured(env)) {
      throw new IllegalStateException(
        "COS upload provider requires COS_SECRET_ID, COS_SECRET_KEY, COS_BUCKET_NAME and COS_BASE_URL"
      );
    }
  }

  private boolean isCosConfigured(Environment env) {
    return (
      hasText(env.getProperty("cos.secret-id")) &&
      hasText(env.getProperty("cos.secret-key")) &&
      hasText(env.getProperty("cos.bucket-name")) &&
      hasText(env.getProperty("cos.base-url"))
    );
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank() && !"https://example.com".equals(value);
  }
}
