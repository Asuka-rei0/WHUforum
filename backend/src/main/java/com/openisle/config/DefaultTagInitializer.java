package com.openisle.config;

import com.openisle.repository.TagRepository;
import com.openisle.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DefaultTagInitializer implements ApplicationRunner {

  private static final DefaultTag[] DEFAULT_TAGS = {
    new DefaultTag("选课", "课程选择、培养方案与选课建议"),
    new DefaultTag("考试", "考试安排、复习资料与考后交流"),
    new DefaultTag("课程经验", "课堂体验、作业难度与课程评价"),
    new DefaultTag("教务通知", "教务安排、学籍与办事提醒"),
    new DefaultTag("奖助学金", "奖学金、助学金与评奖评优信息"),
    new DefaultTag("讲座", "讲座预告、沙龙与学术报告"),
    new DefaultTag("科研", "科研项目、课题组与实验室交流"),
    new DefaultTag("论文", "论文写作、投稿与阅读讨论"),
    new DefaultTag("竞赛", "学科竞赛、创新创业与组队信息"),
    new DefaultTag("升学", "保研、考研、申博与留学交流"),
    new DefaultTag("出闲置", "发布二手闲置与转让信息"),
    new DefaultTag("求购", "求购教材、用品与设备"),
    new DefaultTag("教材", "教材、资料、讲义与书籍交易"),
    new DefaultTag("数码", "电脑、手机、配件与电子设备"),
    new DefaultTag("生活用品", "宿舍用品、家具家电与日用品"),
    new DefaultTag("树洞", "匿名倾诉、情绪记录与日常碎碎念"),
    new DefaultTag("求助", "学习、生活与校园事务求助"),
    new DefaultTag("寻物", "失物招领、寻物启事与线索征集"),
    new DefaultTag("互助", "互相帮忙、经验答疑与资源共享"),
    new DefaultTag("校园安全", "安全提醒、风险反馈与应急互助"),
    new DefaultTag("食堂", "食堂菜品、窗口推荐与用餐体验"),
    new DefaultTag("宿舍", "寝室生活、维修、水电与住宿经验"),
    new DefaultTag("社团", "社团活动、招新与兴趣小组"),
    new DefaultTag("运动", "运动打卡、场馆、约球与赛事"),
    new DefaultTag("校园活动", "校内活动、展演、志愿与打卡分享"),
  };

  private final TagRepository tagRepository;
  private final TagService tagService;

  @Value("${app.default-tags.enabled:true}")
  private boolean enabled;

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (!enabled) {
      return;
    }
    for (DefaultTag tag : DEFAULT_TAGS) {
      if (tagRepository.findByName(tag.name()).isEmpty()) {
        tagService.createTag(tag.name(), tag.description(), null, null, true, null);
      }
    }
  }

  private record DefaultTag(String name, String description) {}
}
