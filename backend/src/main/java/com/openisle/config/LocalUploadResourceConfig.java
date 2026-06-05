package com.openisle.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class LocalUploadResourceConfig implements WebMvcConfigurer {

  @Value("${app.upload.local-dir:uploads}")
  private String localDir;

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    Path uploadPath = Paths.get(localDir).toAbsolutePath().normalize();
    String location = uploadPath.toUri().toString();
    if (!location.endsWith("/")) {
      location += "/";
    }
    registry.addResourceHandler("/uploads/**").addResourceLocations(location);
  }
}
