package com.openisle.config;

import com.openisle.model.Category;
import com.openisle.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CampusCategoryInitializer implements CommandLineRunner {

  private final CategoryRepository categoryRepository;

  @Value("${app.whu.init-categories:true}")
  private boolean initCategories;

  @Override
  public void run(String... args) {
    if (!initCategories) {
      return;
    }
    ensureCategory(
      "\u6559\u52a1\u516c\u544a",
      "\u6559\u52a1\u4fe1\u606f\u3001\u8bfe\u7a0b\u4e0e\u8003\u8bd5\u901a\u77e5",
      "book-open"
    );
    ensureCategory(
      "\u5b66\u672f\u4ea4\u6d41",
      "\u8bb2\u5ea7\u3001\u79d1\u7814\u3001\u5b66\u672f\u8ba8\u8bba",
      "experiment"
    );
    ensureCategory(
      "\u8df3\u86a4\u5e02\u573a",
      "\u4e8c\u624b\u4ea4\u6613\u4e0e\u6821\u56ed\u95f2\u7f6e",
      "shopping-bag"
    );
    ensureCategory(
      "\u6811\u6d1e\u4e92\u52a9",
      "\u533f\u540d\u503e\u8bc9\u3001\u6c42\u52a9\u4e0e\u4e92\u52a9",
      "message-privacy"
    );
    ensureCategory(
      "\u6821\u56ed\u751f\u6d3b",
      "\u65e5\u5e38\u751f\u6d3b\u3001\u6d3b\u52a8\u4e0e\u7ecf\u9a8c\u5206\u4eab",
      "school"
    );
  }

  private void ensureCategory(String name, String description, String icon) {
    if (categoryRepository.findByName(name).isPresent()) {
      return;
    }
    Category category = new Category();
    category.setName(name);
    category.setDescription(description);
    category.setIcon(icon);
    categoryRepository.save(category);
  }
}
