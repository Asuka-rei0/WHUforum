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
(1,b'1','2025-09-02 10:51:56.000000','课程经验与选课讨论',NULL,'课程经验',NULL,NULL),
(2,b'1','2025-09-02 10:51:56.000000','校园生活信息',NULL,'校园生活',NULL,NULL),
(3,b'1','2025-09-02 10:51:56.000000','二手闲置与交易',NULL,'二手闲置',NULL,NULL);

SET FOREIGN_KEY_CHECKS = 1;
