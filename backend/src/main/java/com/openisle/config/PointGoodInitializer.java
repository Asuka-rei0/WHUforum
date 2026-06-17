package com.openisle.config;

import com.openisle.model.PointGood;
import com.openisle.repository.PointGoodRepository;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/** Initialize default point mall goods. */
@Component
@RequiredArgsConstructor
public class PointGoodInitializer implements CommandLineRunner {

  private final PointGoodRepository pointGoodRepository;

  private record Seed(String legacyName, String name, int cost, String image) {}

  private static final List<Seed> TARGET_GOODS = List.of(
    new Seed(
      "GPT Plus 1 个月",
      "校园网网费 1 个月",
      3000,
      "https://img.icons8.com/color/240/wifi--v1.png"
    ),
    new Seed("奶茶", "食堂餐券 20 元", 2000, "https://img.icons8.com/color/240/meal.png"),
    new Seed(null, "图书馆打印券 50 页", 1200, "https://img.icons8.com/color/240/print.png")
  );

  @Override
  public void run(String... args) {
    Map<String, PointGood> goodsByName = pointGoodRepository
      .findAll()
      .stream()
      .collect(java.util.stream.Collectors.toMap(PointGood::getName, good -> good, (a, b) -> a));

    for (Seed seed : TARGET_GOODS) {
      PointGood good = goodsByName.get(seed.name());
      PointGood legacyGood = seed.legacyName() == null ? null : goodsByName.get(seed.legacyName());
      if (good == null && seed.legacyName() != null) {
        good = legacyGood;
      } else if (legacyGood != null && !java.util.Objects.equals(good.getId(), legacyGood.getId())) {
        pointGoodRepository.delete(legacyGood);
      }
      if (good == null) {
        good = new PointGood();
      }
      good.setName(seed.name());
      good.setCost(seed.cost());
      good.setImage(seed.image());
      pointGoodRepository.save(good);
    }
  }
}
