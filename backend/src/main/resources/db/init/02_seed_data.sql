USE `openisle`;
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM `tags`;
DELETE FROM `categories`;
DELETE FROM `users`;

-- 插入用户，两个普通用户，一个管理员
-- username:admin/user1/user2 password:123456
INSERT INTO `users` (
  `id`,
  `approved`,
  `avatar`,
  `created_at`,
  `display_medal`,
  `email`,
  `experience`,
  `introduction`,
  `password`,
  `password_reset_code`,
  `point`,
  `register_reason`,
  `role`,
  `username`,
  `verification_code`,
  `verified`,
  `is_bot`
) VALUES
(1, b'1', '/whu-emblem.webp', '2025-09-01 16:08:17.426430', 'PIONEER', 'admin@whu.edu.cn', 70, NULL, '$2a$10$x7HXjUyJTmrvqjnBlBQZH.vmfsC56NzTSWqQ6WqZqRjUO859EhviS', NULL, 110, '珞珈论坛初始化管理员', 'ADMIN', 'admin', NULL, b'1', b'0'),
(2, b'1', '/whu-emblem.webp', '2025-09-03 16:08:17.426430', 'PIONEER', 'student1@whu.edu.cn', 70, NULL, '$2a$10$x7HXjUyJTmrvqjnBlBQZH.vmfsC56NzTSWqQ6WqZqRjUO859EhviS', NULL, 110, '武汉大学校园论坛用户', 'USER', 'user1', NULL, b'1', b'0'),
(3, b'1', '/whu-emblem.webp', '2025-09-02 17:21:21.617666', 'PIONEER', 'student2@whu.edu.cn', 40, NULL, '$2a$10$x7HXjUyJTmrvqjnBlBQZH.vmfsC56NzTSWqQ6WqZqRjUO859EhviS', NULL, 40, '武汉大学校园论坛用户', 'USER', 'user2', NULL, b'1', b'0');

INSERT INTO `categories` (`id`,`description`,`icon`,`name`,`small_icon`) VALUES
(1,'教务信息、课程与考试通知','book-open','教务公告',NULL),
(2,'讲座、科研、学术讨论','experiment','学术交流',NULL),
(3,'二手交易与校园闲置','shopping-bag','跳蚤市场',NULL),
(4,'匿名倾诉、求助与互助','message-privacy','树洞互助',NULL),
(5,'日常生活、活动与经验分享','school','校园生活',NULL);

INSERT INTO `tags` (`id`,`approved`,`created_at`,`description`,`icon`,`name`,`small_icon`,`creator_id`) VALUES
(1,b'1','2025-09-02 10:51:56.000000','课程选择、培养方案与选课建议',NULL,'选课',NULL,NULL),
(2,b'1','2025-09-02 10:51:56.000000','考试安排、复习资料与考后交流',NULL,'考试',NULL,NULL),
(3,b'1','2025-09-02 10:51:56.000000','课堂体验、作业难度与课程评价',NULL,'课程经验',NULL,NULL),
(4,b'1','2025-09-02 10:51:56.000000','教务安排、学籍与办事提醒',NULL,'教务通知',NULL,NULL),
(5,b'1','2025-09-02 10:51:56.000000','奖学金、助学金与评奖评优信息',NULL,'奖助学金',NULL,NULL),
(6,b'1','2025-09-02 10:51:56.000000','讲座预告、沙龙与学术报告',NULL,'讲座',NULL,NULL),
(7,b'1','2025-09-02 10:51:56.000000','科研项目、课题组与实验室交流',NULL,'科研',NULL,NULL),
(8,b'1','2025-09-02 10:51:56.000000','论文写作、投稿与阅读讨论',NULL,'论文',NULL,NULL),
(9,b'1','2025-09-02 10:51:56.000000','学科竞赛、创新创业与组队信息',NULL,'竞赛',NULL,NULL),
(10,b'1','2025-09-02 10:51:56.000000','保研、考研、申博与留学交流',NULL,'升学',NULL,NULL),
(11,b'1','2025-09-02 10:51:56.000000','发布二手闲置与转让信息',NULL,'出闲置',NULL,NULL),
(12,b'1','2025-09-02 10:51:56.000000','求购教材、用品与设备',NULL,'求购',NULL,NULL),
(13,b'1','2025-09-02 10:51:56.000000','教材、资料、讲义与书籍交易',NULL,'教材',NULL,NULL),
(14,b'1','2025-09-02 10:51:56.000000','电脑、手机、配件与电子设备',NULL,'数码',NULL,NULL),
(15,b'1','2025-09-02 10:51:56.000000','宿舍用品、家具家电与日用品',NULL,'生活用品',NULL,NULL),
(16,b'1','2025-09-02 10:51:56.000000','匿名倾诉、情绪记录与日常碎碎念',NULL,'树洞',NULL,NULL),
(17,b'1','2025-09-02 10:51:56.000000','学习、生活与校园事务求助',NULL,'求助',NULL,NULL),
(18,b'1','2025-09-02 10:51:56.000000','失物招领、寻物启事与线索征集',NULL,'寻物',NULL,NULL),
(19,b'1','2025-09-02 10:51:56.000000','互相帮忙、经验答疑与资源共享',NULL,'互助',NULL,NULL),
(20,b'1','2025-09-02 10:51:56.000000','安全提醒、风险反馈与应急互助',NULL,'校园安全',NULL,NULL),
(21,b'1','2025-09-02 10:51:56.000000','食堂菜品、窗口推荐与用餐体验',NULL,'食堂',NULL,NULL),
(22,b'1','2025-09-02 10:51:56.000000','寝室生活、维修、水电与住宿经验',NULL,'宿舍',NULL,NULL),
(23,b'1','2025-09-02 10:51:56.000000','社团活动、招新与兴趣小组',NULL,'社团',NULL,NULL),
(24,b'1','2025-09-02 10:51:56.000000','运动打卡、场馆、约球与赛事',NULL,'运动',NULL,NULL),
(25,b'1','2025-09-02 10:51:56.000000','校内活动、展演、志愿与打卡分享',NULL,'校园活动',NULL,NULL);

SET FOREIGN_KEY_CHECKS = 1;
