/*
 Navicat Premium Data Transfer

 Source Server         : localhost
 Source Server Type    : MySQL
 Source Server Version : 80045
 Source Host           : localhost:3306
 Source Schema         : novaleap

 Target Server Type    : MySQL
 Target Server Version : 80045
 File Encoding         : 65001

 Date: 09/07/2026 16:24:20
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for custom_question_banks
-- ----------------------------
DROP TABLE IF EXISTS `custom_question_banks`;
CREATE TABLE `custom_question_banks`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '上传用户 ID',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '题库名',
  `original_file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '原始文件名',
  `file_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'txt/doc/docx',
  `raw_content` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '解析后的原始文本',
  `category` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'java' COMMENT '默认分类',
  `difficulty` tinyint NOT NULL DEFAULT 2 COMMENT '默认难度',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '0: 待审核 1: 已通过 2: 已驳回',
  `question_count` int NOT NULL DEFAULT 0 COMMENT '解析题目数',
  `imported_question_count` int NOT NULL DEFAULT 0 COMMENT '正式入库题目数',
  `reject_reason` varchar(240) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '驳回原因',
  `audited_at` datetime NULL DEFAULT NULL COMMENT '审核时间',
  `imported_at` datetime NULL DEFAULT NULL COMMENT '正式入库时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_custom_question_banks_user_status`(`user_id` ASC, `status` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_custom_question_banks_status_created`(`status` ASC, `created_at` ASC) USING BTREE,
  CONSTRAINT `fk_custom_question_banks_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户自定义题库审核表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of custom_question_banks
-- ----------------------------
INSERT INTO `custom_question_banks` VALUES (1, 1, '题库导入模板', '题库导入模板.txt', 'txt', '题目：请解释数据库索引失效的常见原因哈哈哈。\n答案：函数计算、隐式类型转换、前导模糊匹配等会导致索引失效。\n\n题目：请说明缓存穿透和缓存击穿的区别啦啦啦。\n答案：缓存穿透是查询不存在数据，缓存击穿是热点 key 失效瞬间并发打到数据库。', 'java', 2, 1, 2, 2, NULL, '2026-03-26 13:17:01', '2026-03-26 13:17:01', '2026-03-26 13:16:41', '2026-03-26 13:17:01');
INSERT INTO `custom_question_banks` VALUES (2, 1, 'Linux', '题库导入模板.txt', 'txt', '题目：请解释数据库索引失效的常见原因哈哈哈。\n答案：函数计算、隐式类型转换、前导模糊匹配等会导致索引失效。\n\n题目：请说明缓存穿透和缓存击穿的区别啦啦啦。\n答案：缓存穿透是查询不存在数据，缓存击穿是热点 key 失效瞬间并发打到数据库。', 'java', 2, 1, 2, 2, NULL, '2026-03-29 01:59:55', '2026-03-29 01:59:55', '2026-03-26 22:19:47', '2026-03-29 01:59:55');
INSERT INTO `custom_question_banks` VALUES (3, 49, '题库导入模板', '题库导入模板.txt', 'txt', '题目：请解释数据库索引失效的常见原因。\n答案：函数计算、隐式类型转换、前导模糊匹配等会导致索引失效。\n\n题目：请说明缓存穿透和缓存击穿的区别。\n答案：缓存穿透是查询不存在数据，缓存击穿是热点 key 失效瞬间并发打到数据库。', 'ai', 2, 1, 2, 2, NULL, '2026-04-21 10:20:46', '2026-04-21 10:20:46', '2026-04-21 10:19:31', '2026-04-21 10:20:46');

-- ----------------------------
-- Table structure for game_score_logs
-- ----------------------------
DROP TABLE IF EXISTS `game_score_logs`;
CREATE TABLE `game_score_logs`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT 'users.id',
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'username snapshot',
  `round_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'one game round/session id',
  `score` int NOT NULL COMMENT 'reported score at this time',
  `is_final` tinyint NOT NULL DEFAULT 0 COMMENT '1 final score for round',
  `recorded_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'record time',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_game_score_logs_user_time`(`user_id` ASC, `recorded_at` ASC) USING BTREE,
  INDEX `idx_game_score_logs_round_time`(`round_id` ASC, `recorded_at` ASC) USING BTREE,
  INDEX `idx_game_score_logs_score`(`score` ASC) USING BTREE,
  CONSTRAINT `fk_game_score_logs_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 137 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'cuihua runner live score logs' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of game_score_logs
-- ----------------------------
INSERT INTO `game_score_logs` VALUES (110, 40, 'seed_joy_offline', 'seed-round-seed_joy_offline', 840, 0, '2026-04-01 20:40:13');
INSERT INTO `game_score_logs` VALUES (111, 40, 'seed_joy_offline', 'seed-round-seed_joy_offline', 1090, 0, '2026-04-01 20:40:21');
INSERT INTO `game_score_logs` VALUES (112, 40, 'seed_joy_offline', 'seed-round-seed_joy_offline', 1260, 1, '2026-04-01 20:40:29');
INSERT INTO `game_score_logs` VALUES (113, 41, 'seed_cloudy_today', 'seed-round-seed_cloudy_today', 765, 0, '2026-04-02 20:40:13');
INSERT INTO `game_score_logs` VALUES (114, 41, 'seed_cloudy_today', 'seed-round-seed_cloudy_today', 1015, 0, '2026-04-02 20:40:21');
INSERT INTO `game_score_logs` VALUES (115, 41, 'seed_cloudy_today', 'seed-round-seed_cloudy_today', 1185, 1, '2026-04-02 20:40:29');
INSERT INTO `game_score_logs` VALUES (116, 42, 'seed_too_quiet', 'seed-round-seed_too_quiet', 678, 0, '2026-04-03 20:40:13');
INSERT INTO `game_score_logs` VALUES (117, 42, 'seed_too_quiet', 'seed-round-seed_too_quiet', 928, 0, '2026-04-03 20:40:21');
INSERT INTO `game_score_logs` VALUES (118, 42, 'seed_too_quiet', 'seed-round-seed_too_quiet', 1098, 1, '2026-04-03 20:40:29');
INSERT INTO `game_score_logs` VALUES (119, 43, 'seed_low_pressure', 'seed-round-seed_low_pressure', 568, 0, '2026-04-04 20:40:13');
INSERT INTO `game_score_logs` VALUES (120, 43, 'seed_low_pressure', 'seed-round-seed_low_pressure', 818, 0, '2026-04-04 20:40:21');
INSERT INTO `game_score_logs` VALUES (121, 43, 'seed_low_pressure', 'seed-round-seed_low_pressure', 988, 1, '2026-04-04 20:40:29');
INSERT INTO `game_score_logs` VALUES (122, 44, 'seed_mood_overcast', 'seed-round-seed_mood_overcast', 512, 0, '2026-04-05 20:40:13');
INSERT INTO `game_score_logs` VALUES (123, 44, 'seed_mood_overcast', 'seed-round-seed_mood_overcast', 762, 0, '2026-04-05 20:40:21');
INSERT INTO `game_score_logs` VALUES (124, 44, 'seed_mood_overcast', 'seed-round-seed_mood_overcast', 932, 1, '2026-04-05 20:40:29');
INSERT INTO `game_score_logs` VALUES (125, 45, 'seed_late_happy', 'seed-round-seed_late_happy', 466, 0, '2026-04-06 20:40:13');
INSERT INTO `game_score_logs` VALUES (126, 45, 'seed_late_happy', 'seed-round-seed_late_happy', 716, 0, '2026-04-06 20:40:21');
INSERT INTO `game_score_logs` VALUES (127, 45, 'seed_late_happy', 'seed-round-seed_late_happy', 886, 1, '2026-04-06 20:40:29');
INSERT INTO `game_score_logs` VALUES (128, 46, 'seed_happy_loading', 'seed-round-seed_happy_loading', 421, 0, '2026-04-07 20:40:13');
INSERT INTO `game_score_logs` VALUES (129, 46, 'seed_happy_loading', 'seed-round-seed_happy_loading', 671, 0, '2026-04-07 20:40:21');
INSERT INTO `game_score_logs` VALUES (130, 46, 'seed_happy_loading', 'seed-round-seed_happy_loading', 841, 1, '2026-04-07 20:40:29');
INSERT INTO `game_score_logs` VALUES (131, 47, 'seed_human_observer', 'seed-round-seed_human_observer', 348, 0, '2026-04-08 20:40:13');
INSERT INTO `game_score_logs` VALUES (132, 47, 'seed_human_observer', 'seed-round-seed_human_observer', 598, 0, '2026-04-08 20:40:21');
INSERT INTO `game_score_logs` VALUES (133, 47, 'seed_human_observer', 'seed-round-seed_human_observer', 768, 1, '2026-04-08 20:40:29');
INSERT INTO `game_score_logs` VALUES (134, 48, 'seed_silent_mode', 'seed-round-seed_silent_mode', 282, 0, '2026-04-09 20:40:13');
INSERT INTO `game_score_logs` VALUES (135, 48, 'seed_silent_mode', 'seed-round-seed_silent_mode', 532, 0, '2026-04-09 20:40:21');
INSERT INTO `game_score_logs` VALUES (136, 48, 'seed_silent_mode', 'seed-round-seed_silent_mode', 702, 1, '2026-04-09 20:40:29');

-- ----------------------------
-- Table structure for note_comments
-- ----------------------------
DROP TABLE IF EXISTS `note_comments`;
CREATE TABLE `note_comments`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `note_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `nickname` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `content` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_note_comments_note_id_created`(`note_id` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_note_comments_user_id`(`user_id` ASC) USING BTREE,
  CONSTRAINT `fk_note_comments_note` FOREIGN KEY (`note_id`) REFERENCES `notes` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_note_comments_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Notes comments' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of note_comments
-- ----------------------------
INSERT INTO `note_comments` VALUES (3, 29, 48, 'seed_silent_mode', '间歇性沉默', '我也是挂过一次项目深挦才反应过来，面试官真的就爱问“线上真实情况”。', '2026-04-02 13:11:12', 0);
INSERT INTO `note_comments` VALUES (4, 30, 40, 'seed_joy_offline', '快乐不在服务区', '索引这块我也是 explain 看得少，每次一被问 SQL 就虚。', '2026-04-03 13:11:12', 0);
INSERT INTO `note_comments` VALUES (5, 31, 41, 'seed_cloudy_today', '天气转阴不转晴', '“知识点 + 项目场景”这种记法我准备今晚就试一下。', '2026-04-04 13:11:12', 0);
INSERT INTO `note_comments` VALUES (6, 32, 42, 'seed_too_quiet', '最近常常太安静', '我之前也是乱投，后来发现 JD 不对着改简历真的白投。', '2026-04-05 13:11:13', 0);
INSERT INTO `note_comments` VALUES (7, 33, 43, 'seed_low_pressure', '低气压区', '简历里有数字和前后对比之后，面试官追问都会更顺一点。', '2026-04-06 13:11:13', 0);
INSERT INTO `note_comments` VALUES (8, 34, 44, 'seed_mood_overcast', '情绪阴天', 'HR 面这块我也是后来才发现，死背模板真的不如说人话。', '2026-04-07 13:11:13', 0);
INSERT INTO `note_comments` VALUES (9, 35, 45, 'seed_late_happy', '晚点开心', '用故障场景记 Redis 这个感觉挺好，我老是把 RDB 和 AOF 背串了。', '2026-04-08 13:11:13', 0);
INSERT INTO `note_comments` VALUES (10, 36, 46, 'seed_happy_loading', '开心加载中', '牛客面经真的要看追问，不然看完一堆题目还是不会答。', '2026-04-09 13:11:13', 0);
INSERT INTO `note_comments` VALUES (11, 37, 47, 'seed_human_observer', '人间观察员', '补文档这个我太有共鸣了，写完才发现自己原来有那么多细节没想清楚。', '2026-04-10 13:11:13', 0);

-- ----------------------------
-- Table structure for note_likes
-- ----------------------------
DROP TABLE IF EXISTS `note_likes`;
CREATE TABLE `note_likes`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `note_id` bigint NOT NULL,
  `actor_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'user / guest',
  `actor_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_note_like_actor`(`note_id` ASC, `actor_type` ASC, `actor_id` ASC) USING BTREE,
  INDEX `idx_note_likes_note_id`(`note_id` ASC) USING BTREE,
  INDEX `idx_note_likes_actor`(`actor_type` ASC, `actor_id` ASC) USING BTREE,
  CONSTRAINT `fk_note_likes_note` FOREIGN KEY (`note_id`) REFERENCES `notes` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Notes likes' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of note_likes
-- ----------------------------
INSERT INTO `note_likes` VALUES (2, 29, 'user', 'seed_cloudy_today', '2026-04-02 11:11:12');
INSERT INTO `note_likes` VALUES (3, 30, 'user', 'seed_too_quiet', '2026-04-03 11:11:12');
INSERT INTO `note_likes` VALUES (4, 31, 'user', 'seed_low_pressure', '2026-04-04 11:11:12');
INSERT INTO `note_likes` VALUES (5, 32, 'user', 'seed_mood_overcast', '2026-04-05 11:11:13');
INSERT INTO `note_likes` VALUES (6, 33, 'user', 'seed_late_happy', '2026-04-06 11:11:13');
INSERT INTO `note_likes` VALUES (7, 34, 'user', 'seed_happy_loading', '2026-04-07 11:11:13');
INSERT INTO `note_likes` VALUES (8, 35, 'user', 'seed_human_observer', '2026-04-08 11:11:13');
INSERT INTO `note_likes` VALUES (9, 36, 'user', 'seed_silent_mode', '2026-04-09 11:11:13');
INSERT INTO `note_likes` VALUES (10, 37, 'user', 'seed_joy_offline', '2026-04-10 11:11:13');

-- ----------------------------
-- Table structure for notes
-- ----------------------------
DROP TABLE IF EXISTS `notes`;
CREATE TABLE `notes`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '手记标题',
  `content` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '手记正文',
  `summary` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '手记摘要',
  `category` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '技术手记' COMMENT '手记分类',
  `emoji` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '?' COMMENT '封面表情',
  `author` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'Nova 学员' COMMENT '作者名',
  `user_id` bigint NULL DEFAULT NULL COMMENT '作者用户ID，关联 users.id',
  `view_count` int NOT NULL DEFAULT 0 COMMENT '浏览次数',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '0: 隐藏 1: 上线',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标记: 1是, 0否',
  `reject_reason` varchar(240) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '瀹℃牳澶辫触鍘熷洜',
  `audit_source` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '瀹℃牳鏉ユ簮锛欰I/MANUAL/RULES',
  `audited_at` datetime NULL DEFAULT NULL COMMENT '瀹℃牳鏃堕棿',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_notes_status_created`(`status` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_notes_category`(`category` ASC) USING BTREE,
  INDEX `idx_notes_user_id`(`user_id` ASC) USING BTREE,
  CONSTRAINT `fk_notes_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 38 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '灵感手记表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of notes
-- ----------------------------
INSERT INTO `notes` VALUES (1, 'Spring 事务为什么会失效：排查时先看代理，再看异常', '很多事务失效问题并不是数据库出了故障，而是方法没有经过 Spring 代理，或者异常在业务层被提前吞掉了。\n\n排查时建议先确认调用入口是不是容器管理的 Bean，再确认目标方法是否满足事务增强条件，最后检查异常传播链路是否真的触发回滚规则。\n\n真正稳定的排查方式不是猜测配置，而是按“代理、方法、异常、提交结果”这条链路逐层定位。', '事务失效最常见的问题不在数据库，而在代理链路和异常传播链路。', '后端实践', '?', 'Nova 编辑部', NULL, 331, 1, '2026-04-10 09:30:00', '2026-04-10 10:00:00', 0, NULL, 'MANUAL', '2026-04-10 10:00:00');
INSERT INTO `notes` VALUES (2, 'Redis 缓存设计的三条底线：别让缓存比数据库更难维护', '缓存不是加上就一定快，很多系统真正的问题是失效策略、回源策略和降级方案没有提前想清楚。\n\n设计缓存时，至少要回答三个问题：什么时候失效，失效后如何回源，缓存不可用时系统如何降级。只有这三个问题清楚了，缓存才是性能工具，而不是复杂度放大器。\n\n缓存命中率很重要，但一致性边界和异常处理能力更重要。', '缓存设计不能只盯命中率，更要先想清楚失效、回源和降级。', '后端实践', '?', 'Nova 编辑部', NULL, 287, 1, '2026-04-10 14:00:00', '2026-04-10 14:20:00', 0, NULL, 'MANUAL', '2026-04-10 14:20:00');
INSERT INTO `notes` VALUES (3, 'MySQL 索引优化不是多建索引，而是减少无效访问', '索引优化的重点从来不是“索引数量够不够多”，而是核心查询是否走在正确的访问路径上。\n\n排查时要重点看 SQL 是否命中预期索引、是否发生回表、排序和分页是否抵消了索引收益，以及联合索引是否真的符合最左匹配规则。\n\n真正有效的索引优化，本质上是在减少不必要的数据扫描和回表成本。', '索引优化的核心不是堆索引，而是让核心查询减少无效访问。', '后端实践', '?', 'Nova 编辑部', NULL, 250, 1, '2026-04-11 09:10:00', '2026-04-11 09:40:00', 0, NULL, 'MANUAL', '2026-04-11 09:40:00');
INSERT INTO `notes` VALUES (4, '接口幂等的四种常见方案，我更推荐先从业务唯一键开始', '支付回调、重复提交、消息重复消费，本质上都在考验系统是否具备幂等能力。\n\n常见做法包括前端防重复提交、服务端幂等令牌、业务唯一键约束和状态机控制。相比单纯在代码里堆判断，业务唯一键更贴近真实业务事实，也更稳定。\n\n如果模型天然具备“这件事只能成功一次”的表达能力，很多幂等问题会简单很多。', '幂等治理最稳的落点通常不是多写 if，而是给业务事实加上唯一约束。', '工程实践', '?', 'Nova 编辑部', NULL, 223, 1, '2026-04-11 11:00:00', '2026-04-11 11:30:00', 0, NULL, 'MANUAL', '2026-04-11 11:30:00');
INSERT INTO `notes` VALUES (5, '从一次慢 SQL 排查里学到的事：先缩小范围，再谈优化', '慢 SQL 排查最怕一上来就改索引、改语句、改参数，却没有先确认问题属于哪一类。\n\n更高效的方式是先拿到真实 SQL 和发生时段，再判断是偶发还是稳定复现，然后通过执行计划确认问题落在扫描、排序、回表还是锁等待，最后再决定优化动作。\n\n先定位，再优化，才不会在错误方向上越改越乱。', '慢 SQL 优化前先判断问题落点，否则方向错了，动作越多越容易失真。', '工程实践', '?', 'Nova 编辑部', NULL, 196, 1, '2026-04-11 15:30:00', '2026-04-11 16:10:00', 0, NULL, 'MANUAL', '2026-04-11 16:10:00');
INSERT INTO `notes` VALUES (6, '简历改版时如何避免技术债扩散：先统一表达，再统一格式', '很多简历问题看起来像“内容不够”，其实更常见的是表达结构不统一，导致亮点无法稳定呈现。\n\n改简历时，建议先统一每段项目经历的表达框架，例如背景、目标、动作、结果，再去处理标题、时间、技术栈和格式细节。\n\n先有统一结构，再做局部润色，才能避免越改越乱。', '简历优化的第一步不是堆内容，而是先统一表达结构。', '职业成长', '?', 'Nova 编辑部', NULL, 168, 1, '2026-04-12 08:50:00', '2026-04-12 09:20:00', 0, NULL, 'MANUAL', '2026-04-12 09:20:00');
INSERT INTO `notes` VALUES (7, '面试中回答项目经历的结构化模板：背景、动作、权衡、结果', '很多项目经历不是没有内容，而是回答时没有稳定结构，导致重点被细节打散。\n\n更适合面试的表达方式是先交代业务背景和目标，再说明自己负责的动作，然后补充关键设计取舍，最后给出结果和复盘。\n\n有结构的表达，不只会让内容更清晰，也能帮助面试官快速判断你的边界和深度。', '项目经历回答最怕散，结构化模板能显著提升表达稳定性。', '面试复盘', '?', 'Nova 编辑部', NULL, 313, 1, '2026-04-12 10:00:00', '2026-04-12 10:40:00', 0, NULL, 'MANUAL', '2026-04-12 10:40:00');
INSERT INTO `notes` VALUES (8, '如何把八股文变成自己的表达：先理解场景，再压缩语言', '八股文最大的问题不是知识点本身，而是很多人只记住了答案，没有建立“它在什么场景里有用”的连接。\n\n更好的训练方式是先为知识点找到真实场景，再把答案压缩成两三句话，最后补一个可以展开的细节。这样在面试现场更容易自然表达，而不是机械背诵。\n\n能把知识点讲成自己的语言，才算真正完成吸收。', '八股文真正要练的不是记忆，而是把场景和表达连接起来。', '学习方法', '?', 'Nova 编辑部', NULL, 278, 1, '2026-04-12 12:40:00', '2026-04-12 13:10:00', 0, NULL, 'MANUAL', '2026-04-12 13:10:00');
INSERT INTO `notes` VALUES (9, '学习新技术时我的三层拆解法：概念、链路、边界', '很多技术资料看了不少，却仍然记不住，往往是因为只停留在概念层，没有建立执行链路和边界条件。\n\n更稳的学习方式是把新知识拆成三层：它是什么，它怎么运行，它在什么情况下失效或不适合。这样既能帮助记忆，也更适合后续复盘和面试表达。\n\n真正可迁移的理解，一定包含边界感。', '学技术不只记概念，更要建立链路和边界，才有可迁移性。', '学习方法', '?', 'Nova 编辑部', NULL, 201, 1, '2026-04-12 14:50:00', '2026-04-12 15:20:00', 0, NULL, 'MANUAL', '2026-04-12 15:20:00');
INSERT INTO `notes` VALUES (10, '后端接口设计里最容易忽略的边界：权限、幂等、可观测性', '接口能跑通，不等于设计已经完整。很多线上问题并不是出在 happy path，而是权限边界、重复提交和排障能力没有提前设计。\n\n设计接口时，至少要同步确认谁能调用、重复调用会发生什么、出了问题能否快速定位。这些信息和参数定义一样重要。\n\n成熟的接口设计，从来不是只有请求和响应。', '接口设计真正的成熟度，在于是否补齐权限、幂等和可观测性边界。', '工程实践', '?', 'Nova 编辑部', NULL, 183, 1, '2026-04-12 17:20:00', '2026-04-12 18:00:00', 0, NULL, 'MANUAL', '2026-04-12 18:00:00');
INSERT INTO `notes` VALUES (11, '从日志、指标、链路三层做故障排查，比盯单个报错更有效', '看到一条报错就立刻修改，是最常见也最容易误判的排障方式。\n\n更可靠的做法是同时看三层信息：日志告诉你哪里异常，指标告诉你影响范围，链路告诉你卡在上下游的哪个位置。三层结合之后，定位速度和准确率都会明显提升。\n\n如果一次故障只能靠经验记忆解决，往往说明可观测性建设还不够。', '排障不能只盯一条报错，把日志、指标和链路结合起来才更稳。', '工程实践', '?', 'Nova 编辑部', NULL, 240, 1, '2026-04-13 08:20:00', '2026-04-13 09:00:00', 0, NULL, 'MANUAL', '2026-04-13 09:00:00');
INSERT INTO `notes` VALUES (12, '为什么团队需要代码评审清单：稳定质量靠流程，不靠临场发挥', '好的代码评审不是谁挑谁的问题，而是团队一起降低线上风险的过程。\n\n如果没有评审清单，关注点会很随机。今天看命名，明天看空指针，后天看权限，质量就很难稳定。至少应该覆盖业务逻辑、权限风险、异常路径、测试补充和兼容性影响。\n\n把个人经验沉淀成团队流程，比一次评审多挑几个问题更有价值。', '代码评审真正的价值，是把经验沉淀成稳定可复用的质量流程。', '工程实践', '✅', 'Nova 编辑部', NULL, 157, 1, '2026-04-13 09:50:00', '2026-04-13 10:20:00', 0, NULL, 'MANUAL', '2026-04-13 10:20:00');
INSERT INTO `notes` VALUES (13, '做题之后为什么一定要写复盘：知识只有被整理过才算自己的', '很多人把“做完一道题”当成“掌握了一类题”，但如果没有复盘，几天后往往只剩下模糊印象。\n\n复盘至少要回答四个问题：这道题考什么，自己卡在哪，正确解法的关键转折点是什么，下次再遇到相似题如何更快判断。\n\n题库负责输入，复盘负责沉淀。没有沉淀的输入，很难形成长期优势。', '做题后的复盘不是附加动作，而是把输入沉淀成判断框架的关键步骤。', '学习方法', '✍️', 'Nova 编辑部', NULL, 268, 1, '2026-04-13 11:10:00', '2026-04-13 11:40:00', 0, NULL, 'MANUAL', '2026-04-13 11:40:00');
INSERT INTO `notes` VALUES (14, '系统设计题如何在十分钟内搭骨架：入口、存储、异步、风控', '系统设计题最容易越讲越散，根本原因通常不是不会，而是没有先搭结构。\n\n一个更稳的骨架是先讲入口层，包括鉴权、限流和接入，再讲存储模型和一致性策略，然后补充异步削峰和通知链路，最后补上审计、监控和降级方案。\n\n先搭骨架，再展开细节，表达会稳定很多。', '系统设计题先讲骨架，再补细节，表达和思路都会清晰很多。', '面试复盘', '?', 'Nova 编辑部', NULL, 344, 1, '2026-04-13 12:40:00', '2026-04-13 13:20:00', 0, NULL, 'MANUAL', '2026-04-13 13:20:00');
INSERT INTO `notes` VALUES (15, 'JVM 调优之前先回答五个问题，不要一上来就改参数', '很多 JVM 调优没有效果，不是因为参数不够多，而是问题定义不清楚。\n\n在动参数之前，至少要先回答：当前症状是什么，触发场景是什么，目标是吞吐优先还是停顿优先，GC 日志和监控是否完整，是否存在明显的对象生命周期问题。\n\n先定义问题，再定义目标，最后才是参数调整。', 'JVM 调优真正的起点不是参数表，而是问题定义和目标澄清。', '后端实践', '☕', 'Nova 编辑部', NULL, 231, 1, '2026-04-13 14:20:00', '2026-04-13 15:00:00', 0, NULL, 'MANUAL', '2026-04-13 15:00:00');
INSERT INTO `notes` VALUES (16, '如何给自己做一次可落地的周复盘：只盯目标，不要堆待办', '很多周复盘最后写成流水账，记录了做过什么，却没回答哪些动作真正有效。\n\n更有用的复盘方式是只保留四块内容：本周最重要的结果、带来结果的动作、投入高但收益低的动作、下周只保留的关键动作。\n\n复盘的价值不是制造更多待办，而是帮助自己收敛节奏。', '高质量周复盘的重点是识别有效动作，而不是继续堆积待办。', '职业成长', '?', 'Nova 编辑部', NULL, 145, 1, '2026-04-13 15:50:00', '2026-04-13 16:20:00', 0, NULL, 'MANUAL', '2026-04-13 16:20:00');
INSERT INTO `notes` VALUES (17, '从需求评审开始减少返工：先确认边界，再确认异常流', '很多返工并不是开发阶段才产生的，而是在需求评审时就埋下了隐患。\n\n评审时如果只谈主流程，不谈边界和异常，开发、测试和产品对最终结果的理解就很容易分叉。更稳的做法是把权限不足、重复提交、回滚策略、审核失败和兼容旧数据这些问题提前问清楚。\n\n很多返工，最后追根到底都是前期边界没有谈明白。', '减少返工最有效的阶段往往不是开发中，而是需求评审时先把边界和异常流问清楚。', '工程实践', '?', 'Nova 编辑部', NULL, 181, 1, '2026-04-13 17:30:00', '2026-04-13 18:10:00', 0, NULL, 'MANUAL', '2026-04-13 18:10:00');
INSERT INTO `notes` VALUES (18, '线上事故复盘不要只写原因，更要写识别信号和预防动作', '事故复盘如果只写“发生了什么”和“根因是什么”，往往还不够，因为经验很难真正复用。\n\n一份更完整的复盘，应该补上最早可识别的信号、当时为什么没有提前发现、后续如何建立告警或阻断动作，以及责任归属和完成时限。\n\n复盘的目标不是追责，而是让同类问题更早被发现、更容易被挡住。', '事故复盘要补齐识别信号和预防动作，经验才能真正复用。', '工程实践', '?', 'Nova 编辑部', NULL, 192, 1, '2026-04-14 08:30:00', '2026-04-14 09:10:00', 0, NULL, 'MANUAL', '2026-04-14 09:10:00');
INSERT INTO `notes` VALUES (19, '个人知识库为什么要坚持统一格式：检索效率比记录热情更重要', '知识库最怕的不是记得少，而是记得很多却找不到、用不上。\n\n统一标题结构、标签规则、摘要写法和复盘模板，能显著降低后续检索成本。长期来看，检索效率远比一时的记录热情更重要。\n\n能被快速找到并再次使用的内容，才算真正进入了你的知识系统。', '知识库长期可用的关键，是统一格式和检索效率，而不是短期记录冲动。', '学习方法', '?', 'Nova 编辑部', NULL, 179, 1, '2026-04-14 09:20:00', '2026-04-14 09:50:00', 0, NULL, 'MANUAL', '2026-04-14 09:50:00');
INSERT INTO `notes` VALUES (20, '写给准备秋招的自己：节奏比刷量更重要', '秋招准备最容易陷入的误区，是把每天做了多少题、看了多少内容，误当成了真正进步。\n\n更重要的是建立稳定节奏：固定输入、固定复盘、固定查漏补缺。只要节奏稳定，积累会自然产生；如果节奏失控，再大的刷量也很难沉淀。\n\n准备期最怕的不是慢，而是乱。', '秋招准备不是比谁刷得多，而是比谁能维持稳定而可持续的成长节奏。', '职业成长', '?', 'Nova 编辑部', NULL, 230, 1, '2026-04-14 10:10:00', '2026-04-14 10:50:00', 0, NULL, 'MANUAL', '2026-04-14 10:50:00');
INSERT INTO `notes` VALUES (29, '一面挂在项目深挦，我才发现自己一直在背“假项目”', '## 今天挂的点\n\n一面面试官没有先问八股，直接让我讲简历上那个“接口性能优化”的项目。我一开口就在说用了 Redis，用了 MQ，用了异步，但对面一直追问：“为什么一定要加 Redis？”“热点是怎么出来的？”“如果缓存雪崩，你这个服务抗不抗得住？”\n\n我当时的问题不是不会，而是说得太像背答案。我会说“缓存击穿用布隆过滤器，雪崩加随机过期时间”，但说不出我这个项目里到底哪个 key 是热点，QPS 大概到什么量级，有没有真遇到过超时。\n\n## 我晚上补的东西\n\n我现在把每个项目都重新拆成 5 格：\n\n- 业务背景是什么\n- 数据量大概多少\n- 为什么选这个方案\n- 真实踩过什么坑\n- 最后指标改善了多少\n\n感觉之前那种写法更像“我参与过项目”，不像“我真的把这个项目做过”。下次再被问“如果再来一次你会怎么设计”，至少不会再直接卡住。', '今天复盘了一场中厂后端一面，本来以为会问 Redis 和 JVM，结果前 30 分钟都在追着项目问。', '面经复盘', '?', '快乐不在服务区', 40, 128, 1, '2026-06-29 21:08:57', '2026-06-29 21:18:57', 0, NULL, 'SYSTEM_SEED', '2026-06-29 21:13:57');
INSERT INTO `notes` VALUES (30, '二面问到 MySQL 索引失效，我终于知道这题该怎么答', '## 二面追问得特别细\n\n今天被问到一题：“联合索引(a, b, c)，为什么 where a = ? and c = ? 不一定走完整索引？”我一开始上来就背最左前缀，对面直接说：“这些我知道，你按 optimizer 的视角讲。”\n\n那一刻我才发现，我之前都是在背答案，不是在理解 SQL 怎么跑。后面我回去把常见的几种情况重新记了一遍：\n\n- 等值匹配是否连续\n- 范围查询在哪一列截断\n- like 以前缀开头还是通配开头\n- 有没有发生隐式类型转换\n- 最终还是要看 explain，不是看感觉\n\n## 我现在的记法\n\n我给自己定了个规矩：每遇到一个八股题，至少写一条真实 SQL 例子，再附一张 explain 截图思路。要不然到面试里一被追问“那你线上怎么定位的？”，还是只能默住。', '不是背出“最左前缀”就结束，面试官更想听你能不能把 SQL 和执行计划串起来。', '面经复盘', '?', '天气转阴不转晴', 41, 116, 1, '2026-06-30 21:08:57', '2026-06-30 21:18:57', 0, NULL, 'SYSTEM_SEED', '2026-06-30 21:13:57');
INSERT INTO `notes` VALUES (31, '八股背到第五轮，我开始用项目例子救自己', '## 之前的状态\n\n前阵子我一直在背 Java 基础、JVM、Redis、Spring 那些高频题，背到后面有点麻木了。例如“线程池拒绝策略有哪些”我能立马说出来，但你真问我项目里为什么核心线程数设 8，我又开始飘。\n\n## 后来的改法\n\n我开始用“知识点 -> 项目场景 -> 追问问题”这种方式记笔记。比如：\n\n- Redis 过期策略 -> 短信验证码和首页热门推荐怎么设置 TTL\n- 线程池 -> 异步发邮件为什么不直接 new Thread\n- 事务失效 -> 我们项目里为什么有一个方法明明加了 @Transactional 还是没回滚\n\n这样整理后，面试的时候至少不会只有“标准答案”。现在我一张卡片上只记一个核心知识点、一个项目场景、两个可能的追问，效率比之前高很多。', '单纯背答案很容易忘，但把每个知识点和自己做过的项目绑起来之后，回答反而顺了。', '学习方法', '?', '最近常常太安静', 42, 103, 1, '2026-07-01 21:08:57', '2026-07-01 21:18:57', 0, NULL, 'SYSTEM_SEED', '2026-07-01 21:13:57');
INSERT INTO `notes` VALUES (32, '投递别贪多，我把每天目标改成了2投 + 1 复盘', '最近把投递节奏改了一下，只给自己定 3 件事：\n\n- 精投 2 个岗位\n- 认真改 1 次简历\n- 复盘 1 场面试或 1 份笔记\n\n之前我一直觉得没 offer 就要暴力多投，但后来发现问题根本不在“投得不够多”，而是每个 JD 我都没有对着改。有些岗位要的是 Java 基础和 MySQL，有些更看项目里的中间件和工程化，结果我简历写得都一样。\n\n现在我每投一个岗，都会顺手把简历上的 1 条项目描述重写得更贴 JD。数量少了，但心里没那么乱了，约面率反而比之前好一点。', '真的不是投得越多就越安心，我前期一天乱投 20 多家，后面反而没精力跟进。', '求职记录', '?', '低气压区', 43, 94, 1, '2026-07-02 21:08:57', '2026-07-02 21:18:57', 0, NULL, 'SYSTEM_SEED', '2026-07-02 21:13:57');
INSERT INTO `notes` VALUES (33, '简历上那句“负责优化接口性能”，今天终于被我改具体了', '## 我原来的写法\n\n“负责接口性能优化，提升了系统吞吐能力。”\n\n这句话我自己看着都没感觉，更别说面试官了。\n\n## 我现在的写法\n\n我把它改成：“针对首页推荐接口的热点查询链路进行优化，通过 Redis 缓存 + 批量查询重构，将 P95 响应时间从 320ms 降到 140ms。”\n\n改完之后我自己都觉得实在多了，因为它起码说清楚了：\n\n- 优化的是什么接口\n- 为什么会慢\n- 用了什么方法\n- 最后指标改善了多少\n\n感觉简历真的就是要当成一份“可以被追问的答案”去写，不然很容易写成大白话。', '没有数字、没有场景、没有前后对比的简历描述，看起来就像没做过。', '简历工坊', '✍️', '情绪阴天', 44, 88, 1, '2026-07-03 21:08:57', '2026-07-03 21:18:57', 0, NULL, 'SYSTEM_SEED', '2026-07-03 21:13:57');
INSERT INTO `notes` VALUES (34, 'HR 面没有想象中那么玄，真诚反而比标准答案有用', '今天 HR 面问了我 3 个题，印象特别深：\n\n1. 你为什么想做后端？\n2. 你最近一次遭遇挫败是什么？\n3. 如果 offer 没下来，你会怎么调整？\n\n我本来还想按照模板答，但后来干脆就说实话了：我就是喜欢那种把一个问题拆开，从接口到库表到监控一层层换掉的感觉；前面有几场面试被问懵了，我有点着急，但这个月开始学会每场只抓 2 个真问题去补。\n\n答完之后对面反而跟我聊得挺宽松的。感觉 HR 面也不是非要“满分答案”，而是要看你是不是知道自己在干嘛，遇到不顺的时候会不会调整。', '我之前总觉得 HR 面是“背话术”，今天才发现说清楚自己的真实想法其实更重要。', '求职记录', '?', '晚点开心', 45, 79, 1, '2026-07-04 21:08:57', '2026-07-04 21:18:57', 0, NULL, 'SYSTEM_SEED', '2026-07-04 21:13:57');
INSERT INTO `notes` VALUES (35, 'Redis 持久化我总记混，今天终于用一个故障例子把它顺下来了', '## 我之前的困惑\n\n面试一问 Redis 持久化，我就会说 RDB 是快照，AOF 是命令追加，然后就说不下去了。但一旦问到“那你线上会怎么选？”，我就还是虚。\n\n## 后来我用一个故障场景去记\n\n我给自己写了一个非常笨的场景：如果今天 Redis 里放的是下单后 5 分钟内要用到的状态数据，机器突然挂了，你能接受丢多少？\n\n- 如果可以接受丢最近一小段，RDB 可能就够用\n- 如果写入不想丢，就要考虑 AOF everysec 甚至更严格的策略\n- 如果还有主从切换，就要再说清楚数据一致性预期\n\n感觉这种记法对我挺有用的，因为我一旦能把场景说顺，后面的优缺点就不是死背了。', '只背 RDB 和 AOF 的概念真的不够，把“如果突然挂了会丢多少数据”想明白之后，记忆点一下就稳了。', '八股整理', '?', '开心加载中', 46, 72, 1, '2026-07-05 21:08:57', '2026-07-05 21:18:57', 0, NULL, 'SYSTEM_SEED', '2026-07-05 21:13:57');
INSERT INTO `notes` VALUES (36, '看了10 多篇牛客面经后，我把高频追问整理成了一张表', '这两天刷了一堆牛客和博客里的 Java 后端面经，感觉最常出现的不是单独的一道题，而是一整串追问。\n\n我现在整理了一张表，左边是高频主题，右边是常见追问：\n\n- Redis -> 为什么用，key 怎么设计，缓存与数据库怎么保持一致\n- MySQL -> 索引为什么生效/失效，explain 怎么看，慢 SQL 怎么优化\n- Spring 事务 -> 为什么失效，自调用问题，异常回滚规则\n- 线程池 -> 参数怎么配，超过核心线程数后会发生什么\n- 项目 -> 最大流量，最难的 bug，如果再设计一遍会改哪里\n\n我发现自己以前总在补“点”，但面试官更爱听“链路”。现在看面经也没那么焦虑了，因为我会边看边问自己：如果这题问到我，我后面会被追问到哪一层？', '光看题目没用，我后来发现真正有用的是“这题问完之后还会接着问什么”。', '面经整理', '?', '人间观察员', 47, 65, 1, '2026-07-06 21:08:57', '2026-07-06 21:18:57', 0, NULL, 'SYSTEM_SEED', '2026-07-06 21:13:57');
INSERT INTO `notes` VALUES (37, '接口文档补完之后，我才敢说自己真的做过这个项目', '我这两天把做过的一个小项目重新补了一份文档，里面只写 4 件事：\n\n- 接口入参出参是什么\n- 库表映射和核心流转是怎么走的\n- 哪些地方有边界情况\n- 线上出问题时最可能查哪几个点\n\n补完之后我再回头看简历，突然觉得自己之前对项目的理解其实挺浮的。很多时候我以为自己会讲，只是因为我记得功能，但真问到异常分支、幂等、接口边界，还是得想半天。\n\n所以我现在的想法是，项目文档不只是为了工作留档，对求职也很有用。至少它能帮我分清楚：哪些是我真会的，哪些只是我以为我会。', '最近没有狂刷题，反而在补项目文档。结果发现这件事对面试的帮助比我想象中大得多。', '项目复盘', '?️', '间歇性沉默', 48, 59, 1, '2026-07-07 21:08:57', '2026-07-07 21:18:57', 0, NULL, 'SYSTEM_SEED', '2026-07-07 21:13:57');

-- ----------------------------
-- Table structure for question_categories
-- ----------------------------
DROP TABLE IF EXISTS `question_categories`;
CREATE TABLE `question_categories`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分类编码',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分类名称',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序',
  `enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否启用：1启用 0禁用',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `code`(`code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 767 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of question_categories
-- ----------------------------
INSERT INTO `question_categories` VALUES (1, 'java', 'Java 核心', 1, 1, '2026-03-26 20:59:35', '2026-04-20 15:52:25');
INSERT INTO `question_categories` VALUES (2, 'spring', 'Spring 生态', 2, 1, '2026-03-26 20:59:35', '2026-04-20 15:52:25');
INSERT INTO `question_categories` VALUES (3, 'db', '数据存储', 3, 1, '2026-03-26 20:59:35', '2026-04-20 15:52:25');
INSERT INTO `question_categories` VALUES (4, 'redis', 'Redis', 4, 1, '2026-03-26 20:59:35', '2026-04-14 09:31:35');
INSERT INTO `question_categories` VALUES (5, 'algo', '算法', 5, 1, '2026-03-26 20:59:35', '2026-04-20 15:52:25');
INSERT INTO `question_categories` VALUES (15, 'linux', 'Linux', 8, 1, '2026-03-26 21:20:36', '2026-04-15 09:35:52');
INSERT INTO `question_categories` VALUES (40, 'network', '计算机网络', 6, 1, '2026-03-26 23:47:06', '2026-04-20 15:52:25');
INSERT INTO `question_categories` VALUES (424, 'ai', 'AI', 8, 1, '2026-04-14 16:28:47', '2026-04-14 09:31:35');
INSERT INTO `question_categories` VALUES (427, '其他', '其他', 9, 1, '2026-04-14 17:09:39', '2026-04-14 17:09:39');
INSERT INTO `question_categories` VALUES (428, 'system-design', '系统设计', 7, 1, '2026-04-15 09:35:52', '2026-04-20 15:52:25');
INSERT INTO `question_categories` VALUES (429, 'arch', '架构设计', 9, 1, '2026-04-15 09:35:52', '2026-04-20 15:52:25');

-- ----------------------------
-- Table structure for questions
-- ----------------------------
DROP TABLE IF EXISTS `questions`;
CREATE TABLE `questions`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '题目标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '题目详细描述',
  `standard_answer` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '数据库标准答案（供AI回答）',
  `difficulty` tinyint NOT NULL DEFAULT 2 COMMENT '难度: 1简单 2中等 3困难',
  `category` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'java' COMMENT '分类: java, spring, db, redis, algo, network',
  `tags` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '标签 (逗号分隔)',
  `view_count` int NOT NULL DEFAULT 0 COMMENT '浏览次数',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '0: 下架 1: 上架',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `source_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'OFFICIAL' COMMENT '来源类型：OFFICIAL/CUSTOM',
  `custom_bank_id` bigint NULL DEFAULT NULL COMMENT '自定义题库 ID',
  `owner_user_id` bigint NULL DEFAULT NULL COMMENT '题目所属用户 ID',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_questions_source_created`(`source_type` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_questions_custom_bank`(`custom_bank_id` ASC) USING BTREE,
  INDEX `idx_questions_owner_source`(`owner_user_id` ASC, `source_type` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 139 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '题库表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of questions
-- ----------------------------
INSERT INTO `questions` VALUES (1, 'Java 为什么只有值传递？', '请解释 Java 为什么只有值传递，并分别说明基本类型参数和引用类型参数在方法调用中的表现。', 'Java 方法调用时传入的永远是实参的副本，因此本质上只有值传递。对于基本类型，传递的是具体数值的副本，修改互不影响；对于引用类型，传递的是对象在堆内存中的地址副本。因此，虽然可以通过这个地址引用修改对象内部的属性，但如果将形参重新指向一个新的对象，并不会改变外部实参原本的指向。', 2, 'java', '官方题库,高频,JavaGuide,java,基础', 0, 1, '2026-04-14 09:31:35', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (2, '== 和 equals() 有什么区别？', '请从基本类型、引用类型以及 String 等常见场景出发，说明 == 和 equals() 的区别。', '基本类型用 == 比较的是数值是否相等；引用类型用 == 比较的是两个引用是否指向堆内存中的同一个对象（即内存地址）。equals() 是 Object 类的方法，默认行为与 == 完全一致。但在实际开发中，String、Integer 等类都重写了 equals() 方法，用于比较对象内部的业务逻辑“值”是否相等，而不是比较内存地址。', 1, 'java', '官方题库,高频,JavaGuide,java,基础', 0, 1, '2026-04-14 09:31:35', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (3, 'hashCode() 和 equals() 为什么要一起重写？', '请说明 hashCode() 和 equals() 的约定，以及只重写其中一个可能带来的问题。', 'Java 规范要求：如果两个对象 equals 相等，那么它们的 hashCode 必须相等。HashSet、HashMap 等散列集合在存储时，会先根据 hashCode 定位桶位置，如果位置上有元素再用 equals 判断是否重复。如果只重写 equals 而不重写 hashCode，会导致逻辑上相等的两个对象产生不同的哈希值，从而被存入不同的桶中，造成集合中出现“重复”元素或无法正确查找。', 2, 'java', '官方题库,高频,JavaGuide,java,集合', 1, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (4, 'String、StringBuilder、StringBuffer 的区别是什么？', '请说明三者的可变性、线程安全性和适用场景。', 'String 的内部字符数组被 final 修饰（JDK 9 后为 byte[]），是不可变对象，每次拼接都会产生新对象；StringBuilder 是可变的，性能最好，但线程不安全，适合单线程下的字符串频繁拼接；StringBuffer 也是可变的，但其核心方法都加了 synchronized 同步锁，线程安全，多线程场景下可用，但性能通常低于 StringBuilder。', 1, 'java', '官方题库,高频,JavaGuide,java,字符串', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (5, 'final、finally、finalize 有什么区别？', '请说明 final、finally、finalize 三者各自的含义和使用场景。', 'final 是修饰符，修饰类表示不可继承，修饰方法表示不可重写，修饰变量表示常量不可二次赋值；finally 是异常处理的关键字，搭配 try/catch 使用，包含必定会执行的收尾代码（如释放连接、关闭流）；finalize 是 Object 类的方法，在对象被垃圾回收前由 JVM 调用，但由于执行时机不确定且可能导致对象复活，JDK 9 之后已被废弃，不推荐依赖它做资源清理。', 1, 'java', '官方题库,高频,JavaGuide,java,基础', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (6, '重载和重写有什么区别？', '请从发生阶段、参数列表、返回值、访问权限等角度说明重载与重写的差异。', '重载（Overload）发生在同一个类中，方法名相同但参数列表（个数、类型、顺序）不同，与返回值无关，是编译时的静态多态；重写（Override）发生在父子类之间，方法签名必须完全一致。重写时，子类的访问权限不能严于父类，抛出的异常不能大于父类，返回值类型必须是父类返回值的同类或子类，属于运行时的动态多态。', 1, 'java', '官方题库,高频,JavaGuide,java,面向对象', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (7, '接口和抽象类有什么区别？', '请说明接口与抽象类在设计目的、继承方式和使用场景上的差异。', '设计层面上，抽象类是对事物本质的抽象（is-a），包含通用模板代码和成员变量；接口是对行为规范的抽象（like-a），更强调具备某种能力。继承特性上，Java 单继承多实现，一个类只能继承一个抽象类，但可以实现多个接口。JDK 8 之后，接口也支持 default 和 static 方法，使得接口在一定程度上具备了代码复用能力，但在状态维护（成员变量）上仍由抽象类主导。', 1, 'java', '官方题库,高频,JavaGuide,java,面向对象', 1, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (8, '浅拷贝和深拷贝有什么区别？', '请说明浅拷贝和深拷贝在包含引用类型字段时的行为差异。', '浅拷贝仅仅复制对象的第一层属性。如果属性是基本类型，复制值；如果是引用类型，则仅复制内存地址，新旧对象的该字段仍指向同一个内部对象，修改会相互影响。深拷贝则会递归复制整个对象图，把引用链上的所有内部对象都复制一份，创建出完全独立的新对象，新旧对象的任何修改都互不干扰。常见的深拷贝方式有序列化/反序列化和重写 clone()。', 2, 'java', '官方题库,高频,JavaGuide,java,对象拷贝', 1, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (9, 'ArrayList 和 LinkedList 有什么区别？', '请从底层结构、随机访问、插入删除、内存开销等角度说明两者差异。', 'ArrayList 底层基于动态数组，支持 O(1) 的随机访问，但在中间插入/删除元素需要整体搬移数据，代价较高；LinkedList 底层基于双向链表，随机访问需遍历 O(n)，但在已知节点的任意位置插入/删除只需修改指针，耗时 O(1)。此外，LinkedList 每个节点都需要额外存储前后指针，内存开销更大。日常开发中，由于 CPU 缓存对数组结构更友好，绝大多数场景优先推荐使用 ArrayList。', 1, 'java', '官方题库,高频,JavaGuide,java,集合', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (10, 'HashMap 的底层数据结构是什么？', '请结合 JDK8 说明 HashMap 的底层结构以及树化条件。', 'JDK 8 中，HashMap 由“数组 + 链表 + 红黑树”组成。存入元素时先计算 hash 值并通过与运算定位到数组桶。当发生哈希冲突时，元素会被追加到链表末尾。为了避免链表过长导致查询退化为 O(n)，当链表长度大于 8 且数组总容量大于 64 时，链表会转化为红黑树，将查询复杂度优化至 O(log n)。如果链表长度退化到 6 以下，红黑树会重新转回链表。', 2, 'java', '官方题库,高频,JavaGuide,java,集合', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (11, 'HashMap 为什么线程不安全？', '请说明 HashMap 在并发场景下可能出现的问题。', 'HashMap 没有针对并发操作进行任何同步控制。在并发写入时，如果发生哈希冲突，可能会导致多个线程同时操作链表，出现后一个节点覆盖前一个节点的数据丢失问题。此外，在触发扩容（resize）时，多个线程同时进行数组迁移，在 JDK 7 中可能会形成环形链表导致死循环（CPU 100%），JDK 8 虽修复了死循环，但依然存在数据覆盖和可见性问题。并发场景应直接使用 ConcurrentHashMap。', 2, 'java', '官方题库,高频,JavaGuide,java,集合,并发', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (12, 'HashMap 和 ConcurrentHashMap 有什么区别？', '请从线程安全、实现思路和使用场景三个角度说明区别。', 'HashMap 线程不安全，仅适用于单线程场景。ConcurrentHashMap 是线程安全的。在 JDK 8 中，ConcurrentHashMap 放弃了 JDK 7 的分段锁（Segment）架构，改用更为细粒度的 CAS + synchronized 来锁定当前数组桶的首节点。这种设计不仅保证了并发安全，还极大提高了并发写冲突时的吞吐量，同时保留了数组+链表+红黑树的高效结构。', 2, 'java', '官方题库,高频,JavaGuide,java,集合,并发', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (13, 'synchronized 和 volatile 有什么区别？', '请说明 synchronized 和 volatile 在线程安全中的作用差异。', 'volatile 是一种轻量级的同步机制，它只能修饰变量，主要作用是保证变量在多线程间的内存可见性（强制从主存读写）并禁止指令重排序，但它不能保证复合操作（如 i++）的原子性。synchronized 是重量级锁，可以修饰方法或代码块，它不仅能保证可见性，还能确保被锁住的代码块在多线程下互斥执行，从而保证绝对的原子性和同步。', 2, 'java', '官方题库,高频,JavaGuide,java,并发', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (14, 'sleep() 和 wait() 有什么区别？', '请说明 sleep 与 wait 在所属类、锁释放行为和使用场景上的差异。', 'sleep() 是 Thread 类的静态方法，它的作用是让当前线程暂停执行指定时间，但如果在同步块中调用，它不会释放持有的对象锁。wait() 是 Object 类的方法，必须在 synchronized 同步代码块中由锁对象调用，调用后线程会进入等待状态并主动释放该对象的锁，直到被 notify()/notifyAll() 唤醒。sleep 用于单纯的线程阻塞，wait 用于多线程间的通信协作。', 1, 'java', '官方题库,高频,JavaGuide,java,并发', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (15, '线程池的核心参数有哪些？', '请说明 corePoolSize、maximumPoolSize、workQueue、keepAliveTime、threadFactory、handler 的作用。', '线程池主要包含六大核心参数：1. corePoolSize（核心线程数）：常驻线程数量；2. maximumPoolSize（最大线程数）：核心线程满且队列满时允许创建的最大线程数；3. workQueue（阻塞队列）：存放等待执行任务的队列；4. keepAliveTime（存活时间）：非核心线程空闲后的存活时间；5. threadFactory：用于创建线程的工厂，可自定义线程名称；6. handler（拒绝策略）：当队列满且达到最大线程数时，处理新任务的策略（如丢弃、抛异常或由调用者执行）。', 2, 'java', '官方题库,高频,JavaGuide,java,并发,线程池', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (16, '为什么不建议使用 Executors 直接创建线程池？', '请从资源风险角度说明阿里开发手册为什么不建议直接使用 Executors。', '阿里开发规范明确禁止使用 Executors 创建线程池，因为其提供的快捷工厂方法存在 OOM（内存溢出）风险。例如，FixedThreadPool 和 SingleThreadPool 默认使用的是无界的 LinkedBlockingQueue，在高并发下可能会堆积海量请求耗尽内存；CachedThreadPool 允许创建的线程数量为 Integer.MAX_VALUE，可能会创建过多线程导致系统资源耗尽。建议直接使用 ThreadPoolExecutor 显式指定参数以规避风险。', 2, 'java', '官方题库,高频,JavaGuide,java,并发,线程池', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (17, 'JMM 是什么？', '请解释 Java 内存模型的作用，以及可见性、原子性、有序性的含义。', 'JMM（Java 内存模型）并不是物理上的内存划分，而是一套规范。它定义了多线程如何通过主内存交互，用来屏蔽不同硬件和操作系统的内存访问差异。JMM 重点关注并发编程的三大特性：可见性（一个线程修改后其他线程立刻能看到）、原子性（操作不可中断）、有序性（指令重排优化不能改变多线程下的执行结果）。通过 volatile、synchronized 和 happens-before 原则，JMM 保证了线程安全。', 2, 'java', '官方题库,高频,JavaGuide,java,JVM,并发', 1, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (18, 'JVM 内存区域有哪些？', '请说明程序计数器、虚拟机栈、本地方法栈、堆、方法区分别存什么。', 'JVM 内存模型主要分为五大部分。线程私有的有：程序计数器（记录当前指令行号，唯一无 OOM 的区域）、虚拟机栈（执行 Java 方法时的局部变量、栈帧）和本地方法栈（服务于 Native 方法）。线程共享的有：堆（存储对象实例和数组，GC 的主要阵地）和方法区（存储已加载的类元数据、常量池、静态变量等，JDK 8 后由元空间实现，使用本地内存）。', 2, 'java', '官方题库,高频,JavaGuide,java,JVM', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (19, '垃圾回收算法有哪些？', '请列举常见 GC 算法，并说明它们的核心思想。', '常见的 GC 算法有四种：1. 标记-清除：先标记存活对象，然后直接清除未标记对象，容易产生内存碎片；2. 标记-复制：将内存分为两半，把存活对象紧凑地复制到另一半，没有碎片但内存利用率只有 50%，常用于年轻代；3. 标记-整理：在标记清除的基础上，把存活对象向一端移动，解决碎片问题但开销大，常用于老年代；4. 分代收集：将堆分为年轻代和老年代，根据对象存活周期采用不同的算法结合。', 2, 'java', '官方题库,高频,JavaGuide,java,JVM,GC', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (20, 'CMS 和 G1 有什么区别？', '请从回收目标、停顿控制、内存整理等角度说明 CMS 与 G1 的差异。', 'CMS 收集器以获取最短垃圾回收停顿时间为目标，采用标记-清除算法，在老年代收集时容易产生大量内存碎片，且在标记阶段占用较多 CPU 资源。G1 收集器则是面向服务端大内存机器设计的，它把堆内存划分为多个大小相等的 Region，通过跟踪各个 Region 的回收价值，在有限时间内优先回收垃圾最多的区域。G1 整体基于标记-整理，局部基于复制，基本不会产生内存碎片，并支持可预测的停顿时间模型。', 3, 'java', '官方题库,高频,JavaGuide,java,JVM,GC', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (21, '什么是 IoC 和 AOP？', '请解释 Spring 中 IoC 和 AOP 的核心思想，并说明它们分别解决什么问题。', 'IoC（控制反转）的核心是将对象的创建、配置和生命周期管理从业务代码中剥离出来，交由 Spring 容器统一管理。它通过 DI（依赖注入）实现，极大地降低了组件之间的耦合度。AOP（面向切面编程）则是 OOP 的补充，它允许开发者在不修改核心业务代码的情况下，把日志记录、事务管理、权限校验等横切关注点抽离出来，通过动态代理的方式在方法执行前后统一织入，提高了代码的复用性。', 1, 'spring', '官方题库,高频,JavaGuide,spring,IOC,AOP', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (22, 'Spring Bean 的生命周期是怎样的？', '请说明 Bean 从实例化、属性注入、初始化到销毁的大致流程。', 'Spring Bean 的生命周期主要分为四个核心阶段：1. 实例化：容器通过反射调用构造方法创建 Bean 的早期实例；2. 依赖注入：解析并填充 Bean 的各个属性（例如 @Autowired 处理）；3. 初始化：依次执行 Aware 回调接口、BeanPostProcessor 的前置处理、@PostConstruct 自定义初始化方法、BeanPostProcessor 的后置处理（这里常用于生成 AOP 代理对象）；4. 销毁：容器关闭时执行 @PreDestroy 及指定的销毁方法。', 2, 'spring', '官方题库,高频,JavaGuide,spring,bean', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (23, 'Spring Bean 的作用域有哪些？', '请列举 Spring Bean 常见作用域，并说明 singleton、prototype、request、session 的差异。', '常见的有五种：1. singleton：默认作用域，在整个 Spring IoC 容器中只存在一个单例；2. prototype：原型模式，每次通过容器获取都会创建一个全新的 Bean 实例；3. request：在一次 HTTP 请求内共享同一个 Bean，适用于 Web 环境；4. session：在一个 HTTP Session 内共享同一个 Bean；5. application/global-session：针对整个 ServletContext 的全局作用域。日常开发中绝大多数无状态的 Service 和 DAO 都是 singleton。', 1, 'spring', '官方题库,高频,JavaGuide,spring,bean', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (24, 'Spring 是怎么解决循环依赖的？', '请说明单例 Bean 循环依赖的三级缓存思路。', 'Spring 通过“三级缓存”机制巧妙解决了单例模式下 Setter 注入的循环依赖问题。一级缓存存放完全就绪的 Bean，二级缓存存放早期暴露的半成品 Bean，三级缓存存放可以生成半成品 Bean 的 ObjectFactory。当 A 依赖 B、B 又依赖 A 时，A 实例化后立刻将自己的工厂放入三级缓存，接着注入 B 时触发 B 的实例化，B 在注入 A 时会从三级缓存获取 A 的早期引用（如果有 AOP 则生成代理对象并放入二级缓存），从而打破死循环。注意：构造器注入的循环依赖 Spring 无法自动解决。', 3, 'spring', '官方题库,高频,JavaGuide,spring,bean,源码', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (25, '@Autowired 和 @Resource 有什么区别？', '请说明两者在按类型、按名称注入上的差异。', '@Autowired 是 Spring 提供的注解，默认按数据类型（byType）进行自动装配。如果匹配到多个同类型的 Bean，再根据参数名称进行匹配，通常可以结合 @Qualifier 注解显式指定名称。@Resource 是 JDK 提供的注解（JSR-250 规范），它默认按名称（byName）进行装配。如果找不到对应的名称，才会退化为按类型进行装配。在实际开发中，推荐使用 @Resource 保证注入的精确性，或者在构造器注入上使用 @Autowired。', 1, 'spring', '官方题库,高频,JavaGuide,spring,依赖注入', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (26, 'Spring 事务的底层原理是什么？', '请说明 Spring 声明式事务是如何基于 AOP 和代理实现的。', 'Spring 的声明式事务本质上是基于 AOP 动态代理和数据库本身的事务特性实现的。当类或方法被 @Transactional 注解修饰时，Spring 容器会为该类生成一个代理对象。在调用被代理的方法前，TransactionInterceptor 拦截器会从数据源获取连接、关闭自动提交开启事务；如果方法正常执行完毕，则拦截器控制提交事务；如果方法抛出了被配置为需回滚的异常（默认是 RuntimeException 或 Error），拦截器则执行回滚操作。', 2, 'spring', '官方题库,高频,JavaGuide,spring,事务', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (27, 'Spring 事务什么时候会失效？', '请说明 Spring 声明式事务常见失效场景，并给出排查思路。', '常见的事务失效场景包括：1. 发生了同类方法自调用，没有经过代理对象拦截；2. @Transactional 注解在了非 public 的方法上；3. 方法内部自行 catch 吞掉了异常，导致拦截器无法捕获；4. 抛出的异常类型属于 Checked Exception，不符合 Spring 默认只回滚 RuntimeException 和 Error 的规则（需配置 rollbackFor = Exception.class）；5. 数据库本身（如 MyISAM）不支持事务。', 2, 'spring', '官方题库,高频,JavaGuide,spring,事务', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (28, 'Spring 事务传播行为有哪些？', '请说明 REQUIRED、REQUIRES_NEW、SUPPORTS 等常见传播行为。', '事务传播行为定义了当一个事务方法被另一个事务方法调用时，应该如何处理事务。最常用的是默认的 REQUIRED：如果当前有事务，就加入其中，如果没有，就新建一个事务。另外经常遇到的是 REQUIRES_NEW：无论外层是否有事务，内部都会挂起当前事务并开启一个全新的、独立的物理事务，内层出错不影响外层。其他还包括 SUPPORTS（有则加入，无则非事务运行）、MANDATORY（必须有事务，否则报错）等。', 2, 'spring', '官方题库,高频,JavaGuide,spring,事务', 1, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (29, '@Transactional 为什么加在 private 方法上不生效？', '请从代理机制角度解释该问题。', '因为 Spring 的声明式事务是建立在动态代理之上的。对于 JDK 动态代理，它只能代理接口中定义的方法（均为 public）；对于 CGLIB 动态代理，它通过生成子类覆盖父类方法来实现，而 private 方法对子类不可见，无法被重写。因此，无论哪种代理方式，当外部调用者通过代理对象访问目标类的 private 方法时，都无法实现 AOP 的拦截增强，从而导致事务失效。', 2, 'spring', '官方题库,高频,JavaGuide,spring,事务', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (30, 'Spring MVC 的执行流程是什么？', '请说明从请求进入到响应返回的大致链路。', '核心链路如下：1. 用户请求到达核心入口 DispatcherServlet；2. 调度器委托 HandlerMapping 找到匹配的处理器并生成执行链（含拦截器）；3. 调度器调用 HandlerAdapter 去执行对应的 Controller 业务逻辑；4. Controller 执行完毕后返回 ModelAndView（现代 API 开发返回对象并标记 @ResponseBody）；5. 视图解析或 MessageConverter 对结果进行格式化（如转为 JSON）；6. 最终写回 HTTP 响应。', 2, 'spring', '官方题库,高频,JavaGuide,spring,mvc', 1, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (31, 'DispatcherServlet 的作用是什么？', '请说明它为什么被称为前端控制器。', 'DispatcherServlet 充当了整个 Spring MVC 请求处理的中枢系统，被称为“前端控制器”。它拦截所有匹配的 HTTP 请求，并统筹调度各个组件（如 HandlerMapping 路由、HandlerAdapter 适配执行、ViewResolver 视图渲染、ExceptionResolver 全局异常处理）。这种中央集权的设计让请求链路清晰可控，降低了各个组件直接的耦合。', 1, 'spring', '官方题库,高频,JavaGuide,spring,mvc', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (32, 'Spring Boot 自动配置原理是什么？', '请说明 Spring Boot 自动配置的核心流程，以及它为什么能做到开箱即用。', 'Spring Boot 自动配置的核心是 @EnableAutoConfiguration 注解。它借助 SpringFactoriesLoader 机制，扫描所有 classpath 依赖包下的 META-INF/spring.factories（较新版本移步 org.springframework.boot.autoconfigure.AutoConfiguration.imports）文件，加载其中的自动配置类。这些配置类大量使用了 @Conditional 派生注解（如 @ConditionalOnClass、@ConditionalOnMissingBean）。当满足特定条件（如引入了某个 Starter 的类）且用户没有自己定义覆盖配置时，Spring 就会自动把默认封装好的 Bean 注入到容器中，从而实现开箱即用。', 2, 'spring', '官方题库,高频,JavaGuide,spring,spring-boot,自动配置', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (33, 'starter 是什么？', '请解释 Spring Boot starter 的作用。', 'Starter 是 Spring Boot 提出的一种场景启动器概念。它本质上是一个空壳 Maven/Gradle 项目，不写具体的业务代码，只负责打包和管理某一类功能（例如 Web、Redis、MyBatis）所需的全部基础依赖，并对外提供一份约定好的默认配置。开发者只需引入一个 `spring-boot-starter-xxx`，就能获得完整的依赖树和与之配套的自动配置，彻底告别了以前繁琐的 XML 配置和依赖冲突排查。', 1, 'spring', '官方题库,高频,JavaGuide,spring,spring-boot', 1, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (34, '@SpringBootApplication 包含了哪些注解？', '请说明该组合注解的构成。', '@SpringBootApplication 是一个复合注解，核心由三个注解组成：1. @SpringBootConfiguration：实际上就是一个 @Configuration，标明该类是一个配置类；2. @ComponentScan：负责扫描当前主类所在包及其子包下的所有 @Component、@Service 等组件并将它们注册到容器；3. @EnableAutoConfiguration：最为核心，负责开启并触发 Spring Boot 的自动配置机制。', 1, 'spring', '官方题库,高频,JavaGuide,spring,spring-boot', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (35, 'Spring Boot 为什么能内嵌 Tomcat？', '请说明 Spring Boot 打成 jar 后仍能启动 Web 服务的原因。', '传统的 Java Web 依赖把项目打成 war 包扔进外部 Tomcat 运行。而 Spring Boot 将思路反转：它把 Tomcat 的核心依赖（如 tomcat-embed-core）作为普通的 Maven 依赖引入到项目中。在 Spring Boot 启动刷新容器的过程中，如果识别到属于 Web 环境，就会基于 Java 代码自动实例化一个嵌入式的 TomcatServletWebServerFactory，并以编程的方式初始化 ServletContext、注册 Filter 和 Servlet 并绑定端口，从而以一个可执行的 Fat Jar 独立跑起服务。', 1, 'spring', '官方题库,高频,JavaGuide,spring,spring-boot', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (36, 'BeanFactory 和 ApplicationContext 有什么区别？', '请从功能丰富度和使用场景角度说明差异。', 'BeanFactory 是 Spring IoC 容器的最底层接口，只提供了最基础的 Bean 实例化和依赖注入功能，且默认采用懒加载（延迟加载）策略。ApplicationContext 继承了 BeanFactory，它是我们在开发中最常用的容器。除了基础功能，它还扩展了国际化（MessageSource）、事件发布机制（ApplicationEvent）、AOP 集成以及环境抽象等企业级功能，并在容器启动时就提前实例化所有的单例 Bean。', 2, 'spring', '官方题库,高频,JavaGuide,spring,IOC', 1, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (37, 'Spring 中用到了哪些设计模式？', '请结合常见源码场景举例说明。', 'Spring 框架是设计模式的典范，常见的有：1. 工厂模式：通过 BeanFactory 或 ApplicationContext 创建并管理 Bean；2. 单例模式：Spring 默认的 Bean 作用域即为单例（采用单例注册表实现）；3. 代理模式：AOP 的底层大量使用 JDK 动态代理和 CGLIB；4. 模板方法模式：JdbcTemplate、RedisTemplate 封装了打开连接、异常处理等冗余步骤，只暴露核心操作给子类；5. 观察者模式：Spring 的 ApplicationEvent 和 ApplicationListener 事件驱动模型。', 2, 'spring', '官方题库,高频,JavaGuide,spring,设计模式', 1, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (38, '拦截器和过滤器有什么区别？', '请从生效位置、依赖框架和使用场景角度说明差异。', '过滤器（Filter）是 Servlet 规范的一部分，依赖于 Tomcat 等 Web 容器，在请求进入 Servlet 之前触发，能拦截几乎所有的请求（包含静态资源）。拦截器（Interceptor）则是 Spring MVC 框架自带的组件，依赖于 Spring 容器，只能拦截发往 Controller 的请求。在实际使用中，Filter 常用于全局性的字符编码、跨域设置和白名单拦截；Interceptor 则因为能获取到目标方法和 Spring 上下文，更适合做鉴权、日志记录和用户信息的 ThreadLocal 绑定。', 1, 'spring', '官方题库,高频,JavaGuide,spring,mvc', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (39, '如何统一处理全局异常？', '请说明 Spring Boot 中全局异常处理的常见做法。', '在 Spring Boot 项目中，标准的做法是使用 `@RestControllerAdvice`（或 `@ControllerAdvice` + `@ResponseBody`）配合 `@ExceptionHandler` 注解。开发者可以在这个切面类中定义多个方法拦截特定的异常类型（如业务异常 BusinessException、参数校验异常 MethodArgumentNotValidException 等）。当 Controller 抛出异常时，会被该组件捕获并转换为统一的 JSON 格式（如包含 code、message 的 Result 对象）返回给前端，既优雅又避免了满屏幕的 try-catch。', 1, 'spring', '官方题库,高频,JavaGuide,spring,spring-boot', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (40, 'Spring Boot 项目如何做接口幂等？', '请结合常见业务场景说明实现思路。', '幂等性指对于同一个请求，执行一次和执行多次产生的影响相同。常见解决方案有：1. 数据库唯一索引：防重表或业务表主键限制重复插入；2. 乐观锁：在更新操作时带上版本号 version，适合高并发扣减库存；3. Token 机制：前端在提交表单前先申请一次性 Token 并存入 Redis，提交时验证并删除 Token，解决重复提交；4. 分布式锁：在业务处理前以订单号或请求特征加 Redis 分布式锁，阻挡并发执行。', 2, 'spring', '官方题库,高频,牛客面经,spring,场景题', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (41, 'CHAR 和 VARCHAR 有什么区别？', '请说明 MySQL 中 CHAR 与 VARCHAR 的存储特点、性能差异和适用场景。', 'CHAR 是固定长度的字符串类型，存入数据若不够长度会在末尾用空格补齐，读取速度快但容易浪费空间，适合存储长度固定的数据（如手机号、MD5 密码）。VARCHAR 是可变长度字符串类型，实际占用空间取决于数据真实长度，另外需要 1-2 个字节记录长度前缀，节省空间但在更新导致长度变化时可能引发页分裂，适合长度不固定的业务描述字段。', 1, 'db', '官方题库,高频,JavaGuide,mysql,字段类型', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (42, 'int(11) 的 11 代表什么？', '请说明 int(11) 中显示宽度的含义，以及它是否影响存储大小。', 'int(11) 中的 11 仅仅是“显示宽度”，它完全不影响该字段占用的存储空间大小。int 类型固定占用 4 个字节，存储范围是从 -2147483648 到 2147483647。显示宽度只有在搭配 `ZEROFILL` 属性时才有意义，当数值长度不足 11 位时，查询结果会在前面自动补零。由于意义不大，MySQL 8.0 起已经不再推荐使用带显示宽度的 int 声明。', 1, 'db', '官方题库,高频,JavaGuide,mysql,字段类型', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (43, 'InnoDB 和 MyISAM 有什么区别？', '请从事务、锁、索引和使用场景角度说明差异。', 'InnoDB 是 MySQL 默认的存储引擎，它支持完整的 ACID 事务、行级锁（能支持更高的并发）以及外键约束，而且在崩溃后有基于 redo log 的安全恢复机制。MyISAM 不支持事务，不支持外键，使用的是表级锁（并发写性能较差），但它支持全文索引，在极少数纯读取和大量计数的场景下有优势。现代的业务系统强烈推荐全部使用 InnoDB 引擎。', 1, 'db', '官方题库,高频,JavaGuide,mysql,引擎', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (44, '什么是聚簇索引和非聚簇索引？', '请结合 InnoDB 说明聚簇索引与普通二级索引的差异。', '在 InnoDB 中，聚簇索引（通常是主键索引）的特点是“数据即索引”，B+ 树的叶子节点直接存放了整行的真实业务数据。而非聚簇索引（二级索引或辅助索引）的叶子节点并不存放完整数据，仅存放了“索引列的值”加上对应记录的“主键值”。因此，通过二级索引查询时，除非触发了覆盖索引，否则通常需要用得到的主键值再次到聚簇索引中搜索整行数据。', 2, 'db', '官方题库,高频,JavaGuide,mysql,索引', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (45, '什么是回表？', '请说明二级索引查询为什么会发生回表。', '当执行一条基于二级索引的 SELECT 查询时，如果 SELECT 选出的字段或者 WHERE 条件中的字段并没有完全包含在该二级索引中，InnoDB 在通过二级索引找到对应记录的“主键值”后，必须再拿着这个主键值回到“聚簇索引（主键索引）”中去查询完整的行数据。这个二次查找树的过程就叫做回表。回表会导致额外的磁盘 I/O 开销，优化时应尽量避免。', 2, 'db', '官方题库,高频,JavaGuide,mysql,索引', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (46, '什么是覆盖索引？', '请说明覆盖索引为什么通常更高效。', '如果一个查询所需的所有字段（无论是 SELECT 列表里的，还是 WHERE、ORDER BY 里的）恰好都在某一个二级索引的节点中包含了，那么 MySQL 就能直接从该索引的叶子节点中取到结果并返回，完全不需要“回表”去查询聚簇索引。这就是覆盖索引。由于它省去了二次查树的动作，大大减少了随机磁盘 I/O 耗时，是 SQL 调优的重要手段（Explain 中会显示 Using index）。', 2, 'db', '官方题库,高频,JavaGuide,mysql,索引', 1, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (47, '什么是最左前缀匹配？', '请说明联合索引生效时为什么强调最左前缀原则。', '当我们对表中的 A、B、C 三个字段建立联合索引 (A, B, C) 时，MySQL 构建 B+ 树是按照 A 排序，A 相同时按 B 排序，B 相同时按 C 排序的原则。因此，在查询时，条件必须包含最左边的列 A（即从左到右不能跳跃），索引才能利用树的有序性去快速过滤。如果直接跳过 A 用 B 查询，B 整体上看是无序的，索引就会失效，退化为全表扫描。', 2, 'db', '官方题库,高频,JavaGuide,mysql,索引', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (48, '联合索引在什么情况下会失效？', '请列举常见的联合索引失效场景。', '常见联合索引失效场景包括：1. 违背最左前缀匹配原则，直接查询联合索引的第二或第三列；2. 在最左列使用了范围查询（如 >、<、between），虽然最左列走了索引，但在联合索引中，范围查询字段后面的列将无法继续走索引（索引截断）；3. 查询条件对索引列做了函数运算或隐式类型转换，破坏了 B+ 树原本的有序性；4. 联合索引列使用了 like 左侧带通配符 \"%xx\"。', 2, 'db', '官方题库,高频,JavaGuide,mysql,索引', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (49, '什么情况下索引会失效？', '请列举常见的索引失效场景并说明原因。', '除了违反最左前缀匹配，常见的单列索引失效包括：1. 隐式类型转换，比如字段是 varchar 但传入了数字，MySQL 会对其套用函数进行类型强转，导致索引无法利用；2. 对索引列使用了运算或函数（如 DATE(create_time)）；3. 使用了前置通配符的模糊查询（like \'%abc\'）；4. 使用 OR 时，若 OR 连接的其中一个字段没有建立索引，会导致整体走全表扫描；5. 优化器认为走全表扫描的成本比走索引回表更低时。', 2, 'db', '官方题库,高频,JavaGuide,mysql,索引', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (50, '为什么 InnoDB 推荐使用自增主键？', '请说明 InnoDB 为什么通常推荐使用自增主键，而不是随机主键。', 'InnoDB 的数据本身是存放在以主键排序的聚簇索引 B+ 树中的。如果使用自增主键，新记录的插入就类似于顺序的末尾追加，几乎不会引起已有数据的挪动，页分裂少，缓存命中率高，写入效率极高。如果是用 UUID 等随机且无序的值做主键，每次插入的位置不固定，会引发大量频繁的页分裂、数据移动和磁盘随机写入，导致严重的内存碎片和写入放大，极大拖慢性能。', 2, 'db', '官方题库,高频,JavaGuide,mysql,主键,InnoDB', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (51, 'B+ 树为什么适合做数据库索引？', '请从磁盘 IO、范围查询和树高控制角度说明原因。', '相比红黑树等二叉树，B+ 树是“矮胖”的多路平衡查找树，非叶子节点不存数据只存索引，这使得单个节点（默认 16KB 页）能容纳海量索引，一棵 3 层高的 B+ 树就能支撑千万级数据，极大地降低了磁盘 I/O 次数。其次，相比 Hash 索引不支持范围查询、B 树非叶子节点存数据导致层高变高的缺点，B+ 树所有真实数据都在同一层叶子节点，且互相用双向链表相连，天然具备极强的范围遍历能力和排序优势。', 2, 'db', '官方题库,高频,小林coding,mysql,索引', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (52, 'MySQL 事务的四大特性是什么？', '请说明 ACID 的含义。', '事务四大特性统称 ACID。原子性（Atomicity）指事务是最小单位，要么全成功要么全回滚，由 undo log 保证；一致性（Consistency）指系统总是从一个一致的合法状态转换到另一个一致的状态，是事务追求的最终目标；隔离性（Isolation）指多个并发事务互不干扰，由锁机制和 MVCC 共同保证；持久性（Durability）指事务一旦提交，数据永不丢失，即使宕机也能恢复，由 redo log 保证。', 1, 'db', '官方题库,高频,JavaGuide,mysql,事务', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (53, 'MySQL 事务隔离级别有哪些？', '请列出常见隔离级别，并说明它们主要解决了哪些并发读写问题。', 'SQL 标准定义了四个级别：1. 读未提交（Read Uncommitted）：可能发生脏读；2. 读已提交（Read Committed）：解决脏读，但可能产生不可重复读，Oracle 默认级别；3. 可重复读（Repeatable Read）：解决了不可重复读，并在 InnoDB 中依靠 Next-Key Lock 极大缓解了幻读问题，这是 MySQL 的默认级别；4. 串行化（Serializable）：强制所有事务排队执行，绝对安全但性能最差。', 2, 'db', '官方题库,高频,JavaGuide,mysql,事务', 1, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (54, '脏读、不可重复读、幻读分别是什么？', '请解释三类并发读问题的区别。', '脏读：事务 A 读到了事务 B “已修改但还没提交”的数据，如果 B 回滚，A 读到的就是假数据；不可重复读：在同一个事务内，多次查询某条记录得到的值不一样，因为中途有其他事务“更新（UPDATE）并提交”了这条数据；幻读：在同一个事务内，多次执行同样的范围条件查询时，查出的记录行数不一样，因为中途有其他事务“插入（INSERT）并提交”了新行，就好像产生了幻觉一样。', 2, 'db', '官方题库,高频,JavaGuide,mysql,事务', 1, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (55, 'MVCC 是什么？', '请说明 MVCC 的核心思想，以及它主要解决什么问题。', 'MVCC（多版本并发控制）是 InnoDB 用于提升并发读写性能的一套机制。它通过隐藏字段（事务 ID、回滚指针）将每条数据的历史版本串成一个 Undo Log 版本链。当事务发起“快照读”（如普通的 Select）时，会生成一个 Read View（一致性视图），通过对比当前事务 ID 和版本链上的 ID，决定能读到哪个版本的数据。这就使得“读不加锁，读写不冲突”，极大地提升了并发能力。', 3, 'db', '官方题库,高频,小林coding,mysql,MVCC', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (56, 'redo log、undo log、binlog 分别是什么？', '请从崩溃恢复、一致性、回滚与主从复制角度说明三者作用。', 'redo log（重做日志）是 InnoDB 引擎特有的，记录物理页的修改，提供 crash-safe 能力（宕机恢复，保证持久性）；undo log（回滚日志）也是引擎层日志，记录数据修改前的逻辑快照，用于事务回滚（保证原子性）和支撑 MVCC 多版本控制；binlog（归档日志）是 Server 层维护的逻辑日志，记录所有改变数据的 SQL 语句，主要用于数据库主从同步复制以及误删数据的时间点恢复。', 3, 'db', '官方题库,高频,JavaGuide,mysql,日志', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (57, 'MySQL 中有哪些锁？', '请说明共享锁、排他锁、行锁、表锁、间隙锁的含义。', '按粒度分，有锁定全表的“表锁”（如元数据锁 MDL，开销小并发差）和只锁定索引行的“行锁”（由 InnoDB 提供，开销大并发好）。按属性分，有共享锁（S 锁，读锁，允许互读不许写）和排他锁（X 锁，写锁，完全独占）。在可重复读级别下，InnoDB 还引入了“间隙锁（Gap Lock）”，它锁定的是两条索引记录之间的间隙，防止其他事务插入新数据，结合行锁形成 Next-Key Lock 来解决幻读。', 2, 'db', '官方题库,高频,小林coding,mysql,锁', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (58, '死锁是怎么产生的？怎么排查？', '请说明数据库死锁形成原因以及常见排查方式。', '当两个或多个并发事务各自持有一部分锁资源，又互相等待对方释放资源时，就会形成闭环僵局，导致死锁。InnoDB 拥有死锁检测机制，发现闭环会主动回滚代价较小的事务打破僵局。排查死锁通常可以通过 `SHOW ENGINE INNODB STATUS` 查看最近的死锁日志，分析发生冲突的 SQL 加锁顺序和资源粒度。治理手段主要是保持一致的表更新顺序、将大事务拆小、尽量走索引加细粒度的行锁等。', 2, 'db', '官方题库,高频,JavaGuide,mysql,锁,死锁', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (59, 'SQL 优化一般从哪些方面入手？', '请从索引、SQL 写法、表结构、执行计划角度说明优化思路。', '1. 索引优化：是最有效的手段，包括建立合适的联合索引、避免索引失效、利用覆盖索引避免回表；2. SQL 语法层面：避免 SELECT *，限制查询数据量（limit），用小表驱动大表做 join，拆分复杂的子查询为简单 join；3. 表结构设计：选择合适的字段类型和长度，非核心大字段做垂直拆分；4. 工具分析：必须结合 Explain 命令查看 SQL 执行计划，针对具体的 type 级别和 Extra 信息进行针对性修改。', 2, 'db', '官方题库,高频,牛客面经,mysql,SQL优化', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (60, 'Explain 关键字段怎么分析？', '请说明 type、key、rows、extra 等字段的常见含义。', 'Explain 的核心看点有几个：`type` 反应了访问类型，性能由高到低依次是 system > const（主键精确匹配）> ref（非唯一索引匹配）> range（范围扫描）> index（扫全表索引树）> ALL（全表扫描），一般要求优化到 range 或 ref 级别；`key` 显示了实际使用的索引，若为 NULL 则索引未生效；`rows` 是预估要扫描的行数，越少越好；`Extra` 中，出现 Using index 代表完美覆盖索引，而出现 Using filesort 或 Using temporary 则是性能毒药，急需优化。', 2, 'db', '官方题库,高频,JavaGuide,mysql,SQL优化', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (61, 'Redis 为什么这么快？', '请从数据结构、线程模型和 IO 模型等方面说明 Redis 高性能的原因。', '第一，它是基于纯内存进行操作的，消除了磁盘 I/O 瓶颈，所有数据处理极其迅速。第二，Redis 针对数据结构做了大量专属设计和优化，比如紧凑的压缩列表、高效的跳表和哈希表等。第三，它的核心网络通信和命令处理采用了单线程模型，避免了多线程频繁的上下文切换以及各种加锁释放锁的开销。第四，底层采用 I/O 多路复用机制（如 epoll），能以非阻塞的方式高效处理海量并发客户端连接。', 1, 'redis', '官方题库,高频,JavaGuide,redis,基础', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (62, 'Redis 常见数据结构有哪些？', '请说明 String、Hash、List、Set、ZSet 的使用场景。', 'Redis 原生有五大核心数据结构：String 最常用，适合存 Session、JSON 序列化对象或者做点赞计数器；Hash 适合存储多字段复合实体（如用户信息对象）；List 维护有序链表，可用于简单的消息队列或者朋友圈时间线；Set 是无序不重复集合，适合做社交应用的好友求交集/并集或随机抽奖；ZSet（有序集合）能依据 Score 值自动排序，是做各种积分排行榜、延时队列的不二之选。', 1, 'redis', '官方题库,高频,JavaGuide,redis,数据结构', 1, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (63, 'String 底层是怎么实现的？', '请简要说明 Redis String 与 SDS 的关系。', 'Redis 没有直接使用 C 语言传统的字符数组做 String，而是自行设计了一种 SDS（简单动态字符串）结构。SDS 会同时记录当前字符串的“已用长度”和“分配空间”。这样做的好处是获取字符串长度的时间复杂度降为了 O(1)，而且通过空间预分配和惰性空间释放，极大降低了修改字符串时带来的内存重分配次数。此外，SDS 是二进制安全的，不仅能存文本还能存图片等字节流数据。', 2, 'redis', '官方题库,高频,小林coding,redis,数据结构', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (64, 'zset 为什么适合排行榜？', '请说明 zset 的有序性和底层实现。', 'ZSet 结合了 Set 不允许重复成员的特性，同时给每个成员关联了一个浮点数 score 权重值用于排序。它的底层是由“哈希表 + 跳跃表（SkipList）”构成的。哈希表保证了 O(1) 复杂度通过成员名找到对应分数，跳表则通过建立多级多层的前进指针，实现了极高效率的 O(logN) 平均时间复杂度的节点插入、删除和依据分数范围查询操作，这天然适配了排行榜中的“前 10 名”、“我的排名”等复杂业务场景。', 2, 'redis', '官方题库,高频,小林coding,redis,数据结构', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (65, 'Redis 是单线程吗？', '请说明单线程到底指什么。', '我们常说 Redis 是单线程的，主要是指它的“网络请求接收”、“命令解析解析”以及最核心的“执行具体数据操作和写回结果”这一整个闭环是由一个主线程顺序化处理的。这保证了即使并发再高，Redis 内部读写数据依然是串行原子性的，不需要加锁。但在后台，其实 Redis 早已配备了多个辅助线程去执行一些耗时的、不影响主逻辑的任务，比如异步删除大 key（UNLINK）、异步持久化和 AOF 刷盘等。', 2, 'redis', '官方题库,高频,JavaGuide,redis,基础', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (66, 'Redis 6 之后多线程体现在哪里？', '请说明 Redis 6 的多线程主要优化了什么。', '随着网络硬件飞速发展，Redis 的瓶颈逐渐从 CPU 运算转到了网络 I/O 的读写操作上。为此，Redis 6.0 引入了多线程优化模型。但这个多线程仅仅只用于并行处理客户端 Socket 的读写和协议栈解析，借此大幅提升 I/O 吞吐量；而最终那些真正触碰内存的核心读写命令的执行，依然坚定地沿用了纯粹的单线程模型，这样就巧妙地保留了 Redis 无需锁控制、实现简单的最大优势。', 2, 'redis', '官方题库,高频,小林coding,redis,基础', 1, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (67, '缓存穿透、缓存击穿、缓存雪崩分别是什么？', '请分别解释三者含义，并说明常见治理方案。', '缓存穿透指查询大量压根不存在的数据，缓存和 DB 均不命中导致请求全落 DB。方案是对空结果设短缓存或用布隆过滤器拦截。缓存击穿指某一个极热点的 Key 突然失效，瞬间高并发请求全部直接打爆 DB。方案是热点数据不设过期时间或使用分布式互斥锁排队重构缓存。缓存雪崩是指大量不同 Key 在同一时刻集体过期，或者 Redis 宕机。方案是为 TTL 附加随机偏移量打散过期时间，以及部署高可用集群兜底。', 2, 'redis', '官方题库,高频,JavaGuide,redis,缓存问题', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (68, '如何解决缓存和数据库双写不一致？', '请说明常见的一致性方案以及各自特点。', '通常采用“旁路缓存（Cache Aside）”模式，即先更新数据库，再删除缓存（而非更新缓存）。因为如果在并发下先删缓存再写库，极易被其他读请求把脏数据重新装载回缓存。即便采用先写库再删缓存，仍有极低概率出现删缓存失败的情况。为了追求更高的最终一致性，可以通过 RocketMQ 发送异步消息重试删除，或者更加解耦的方式是利用 Canal 监听 MySQL 的 Binlog 日志，消费者解析日志后异步进行缓存清理。', 3, 'redis', '官方题库,高频,牛客面经,redis,场景题', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (69, 'Redis 的过期删除策略有哪些？', '请说明惰性删除和定期删除的区别。', 'Redis 同时采用了两种策略混合使用：1. 惰性删除：平时放任不管，但在客户端每次尝试访问某个 Key 时，才会检查其是否过期，过期则当场删除。优点是节省 CPU 且无额外压力，缺点是如果冷门过期数据一直没人访问就会造成内存泄露；2. 定期删除：Redis 内部有一个定时任务周期性（默认 100ms 一次）地随机抽取一批设置了过期时间的 Key，清理掉其中已经过期的。通过控制执行时长来防止阻塞业务，算是对内存和 CPU 的一种权衡。', 2, 'redis', '官方题库,高频,JavaGuide,redis,过期', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (70, 'Redis 的内存淘汰策略有哪些？', '请列举常见淘汰策略并说明使用场景。', '当 Redis 内存达到配置上限（maxmemory）且还要存入新数据时触发。常见的有八种：1. noeviction（默认）：坚决不淘汰，抛出 OOM 异常；2. volatile-lru：在设置了过期时间的键中，淘汰最近最少使用的；3. allkeys-lru：在所有的键中，淘汰最近最少使用的，这是绝大多数做纯缓存服务的系统首选；4. 其他还有随机淘汰（random）以及更先进的 LFU（最不经常使用，按访问频率淘汰）策略。', 2, 'redis', '官方题库,高频,JavaGuide,redis,淘汰', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (71, 'Redis 持久化机制 RDB 和 AOF 有什么区别？', '请说明 RDB 和 AOF 的工作方式、优缺点，以及生产上如何取舍。', 'RDB 是一种全量内存快照持久化。它生成的是紧凑的二进制压缩文件，恢复速度极快，适合做异地灾备，但两次快照间距较长，宕机会丢失几分钟的新数据。AOF 是一种追加式日志持久化。它记录了服务器执行的所有写命令，通过 fsync 策略可以将丢数据的风险降低到 1 秒甚至 0 丢失，但带来的问题是文件体积庞大、加载恢复极其缓慢。目前生产环境的主流配置是“RDB-AOF 混合持久化”，汲取两者长处。', 2, 'redis', '官方题库,高频,JavaGuide,redis,持久化', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (72, 'Redis 如何实现分布式锁？', '请说明 setnx 类方案的基本思路和注意事项。', '最基础的实现依靠 `SET lock_key unique_value NX EX max_time` 指令，这条指令保证了原子性：只有当 key 不存在时才能设值成功（加锁），并强制加上超时时间（防止应用崩溃导致死锁）。value 必须设为客户端独一无二的标识（如 UUID），在释放锁时通过 Lua 脚本比对标识一致后才能执行 DEL 删除，防止误释放了别的线程因为超时而续上的新锁。在极端高可用要求下，会考虑使用 Redisson 提供的看门狗自动续期功能和 Redlock 算法。', 3, 'redis', '官方题库,高频,牛客面经,redis,分布式锁', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (73, '为什么要用 Lua 脚本操作 Redis？', '请说明 Lua 脚本在原子性和网络往返上的优势。', 'Lua 脚本在 Redis 中有两大不可替代的优势。首先是它具备“伪原子性”：Redis 在执行 Lua 脚本期间，会将其作为一个完整的整体单线程运行，中途绝不会穿插执行其他客户端打过来的命令，这就完美替代了传统事务中的 watch 机制应对并发竞争。其次，它可以把客户端原本需要多次拆分调用的读写逻辑合并在一起发送，大幅度节省了网络往返的 RTT 时间开销，提升了高并发下的执行效率。', 2, 'redis', '官方题库,高频,JavaGuide,redis,lua', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (74, 'Redis 事务支持 ACID 吗？', '请说明 Redis 事务与数据库事务的区别。', 'Redis 事务机制（MULTI/EXEC）十分简陋。它能保证原子性的一部分——命令统一排队并按序串行执行不被打断，但如果执行期间某条命令因为业务报错（如对字符串做加法），Redis 依然会固执地继续执行后续命令，完全不支持事务回滚（Rollback）。这跟 MySQL 等传统关系型数据库那种要么全成功要么全撤销的严格 ACID 事务相去甚远，仅仅只能算是一个命令打包批量执行器。', 2, 'redis', '官方题库,高频,小林coding,redis,事务', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (75, 'Redis 主从复制的原理是什么？', '请说明主从同步的大致流程。', '第一次建连时，主节点会通过 BGSAVE 生成 RDB 快照发送给从节点做“全量复制”，在这个过程中主节点持续收到的新写命令会被积压到专门的复制缓冲区中，等全量恢复后一并下发。之后，双方进入长连接阶段，主节点会将每一个写操作转化为命令流不断推送给从节点。如果因为网络抖动短暂断开重连，Redis 支持“部分复制”，通过对比 repl_id 和复制偏移量 offset，主节点只从 repl_backlog_buffer 中提取缺失的那一点点增量数据补发过去，效率极高。', 3, 'redis', '官方题库,高频,小林coding,redis,高可用', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (76, '哨兵模式解决了什么问题？', '请说明 Sentinel 的作用。', '主从复制极大地提升了读并发并做了数据冗余，但致命缺陷是主节点一旦宕机，需要人工介入去从库中提拔新的主库，导致服务长时间不可用。哨兵（Sentinel）集群的出现彻底解决了这个问题。它是一个独立的高可用后台监控系统，负责 24 小时不断地用心跳包监视集群。一旦发现主节点客观下线，哨兵们会通过选举机制协商选出一个新主，自动完成切换（故障转移），并将新主的地址更新给所有的客户端。', 2, 'redis', '官方题库,高频,JavaGuide,redis,高可用', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (77, 'Redis Cluster 是什么？', '请说明 Cluster 与主从+哨兵方案的差异。', '虽然主从+哨兵实现了高可用，但依然没有突破单机内存容量和单机写并发的瓶颈。Redis Cluster 是官方的分布式解决方案，它采用了无中心架构和哈希槽（16384 个槽位）算法，将海量数据打散分布到多个不同的主节点上，实现了数据的水平切分扩容。同时，Cluster 内部自带了故障探测机制，如果某个负责特定槽位的主节点倒下，其对应的从节点会自动顶替上去继续服务，是超大规模缓存的首选架构。', 2, 'redis', '官方题库,高频,小林coding,redis,高可用', 1, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (78, '大 Key 会带来什么问题？', '请说明 Redis 大 key 的风险以及治理方法。', 'BigKey 指的是体积非常大的 String，或者含有成千上万个元素的 Hash、List 等聚合结构。由于 Redis 是单线程处理命令，操作 BigKey 时会严重阻塞核心工作线程，导致后续所有请求排队等待，甚至引发主从复制超时断开。治理方法主要是从源头把大结构拆分成多个小结构分摊存储；如果在业务上要删除它们，千万不能直接使用 DEL，必须使用 Redis 4.0 引入的异步删除命令 UNLINK 去后台慢慢释放内存，防止卡顿。', 2, 'redis', '官方题库,高频,牛客面经,redis,性能优化', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (79, '热 Key 怎么处理？', '请说明热 key 带来的风险及优化思路。', 'HotKey 指的是在某一个时间点，比如明星八卦或秒杀抢购，对极其个别的某个缓存键产生了恐怖规模的突发读请求。这会瞬间把某台承载该 key 的 Redis 物理机单点网卡打满，导致节点过载瘫痪。最立竿见影的解决思路是“多级缓存”，将热点数据在应用的 JVM 内存（如 Caffeine、Guava）中也拦截一层本地缓存。或者可以把热点 key 复制 N 份加上后缀名打散到整个 Redis 集群的不同节点去，分散读压力。', 2, 'redis', '官方题库,高频,牛客面经,redis,性能优化', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (80, '布隆过滤器是什么？', '请说明布隆过滤器如何用于解决缓存穿透。', '布隆过滤器是一种空间效率极高的概率型数据结构，由一个超长的二进制位数组和多个不同的哈希函数组成。当一个数据被加入时，它会被哈希成多个点位并置为 1。用来解决缓存穿透时，我们将全量合法的 ID 都映射进布隆过滤器。当恶意请求打进来，先查布隆过滤器，如果过滤器判断为“绝对不存在”，那么请求直接在缓存拦截层就被干掉，绝不放行去查库；如果判断“可能存在”，才允许继续往后走，用极低的假阳性换取了数据库的绝对安全。', 2, 'redis', '官方题库,高频,JavaGuide,redis,缓存问题', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (81, '什么是时间复杂度和空间复杂度？', '请说明常见复杂度表示法以及分析意义。', '时间复杂度采用大 O 表示法，用来衡量算法执行耗时随着输入规模（N）增大时的增长趋势，常见的有 O(1)、O(logN)、O(N)、O(N^2) 等。空间复杂度同样采用大 O 表示法，指运行该算法所需的“额外”内存开销增长趋势。在面试中，往往需要在有限的时间内通过牺牲少量空间（哈希表）来换取时间的跨越式降低（O(N^2) 降至 O(N)）。', 1, 'algo', '官方题库,高频,牛客面经,算法,复杂度', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (82, '数组和链表各自的优缺点是什么？', '请从查找、插入、删除和内存布局角度说明差异。', '数组分配的是一块连续内存区域，支持基于索引的 O(1) 极致随机访问，且 CPU 缓存命中率高，但它的缺点是大小固定，如果从中间插入或删除元素需要挪动大量后续数据，耗时 O(N)。链表的内存在物理上是分散的，依靠指针维系关系，增删节点只需 O(1) 修改指针即可，非常灵活；但它无法支持随机访问，想找到第 K 个元素必须从头遍历，且每个节点自带指针消耗额外空间。', 1, 'algo', '官方题库,高频,牛客面经,算法,数据结构', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (83, '二分查找的适用条件是什么？', '请说明二分查找的前提以及常见边界问题。', '二分查找的核心前提是操作的数据结构必须支持 O(1) 的随机访问（通常是数组），并且内部元素具备一定的有序性（单调递增或递减）。它通过每次排除一半的候选区间将查找耗时压缩到 O(logN)。在手写二分时最容易踩坑的是区间开闭的定义（即 while(left <= right) 还是 <）以及 mid 的计算防溢出（采用 left + (right - left) / 2）。', 1, 'algo', '官方题库,高频,牛客面经,算法,二分', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (84, '双指针适合解决哪些问题？', '请结合数组和链表场景举例说明。', '双指针是一种极为优美的 O(N) 优化技巧。在数组中常常表现为“首尾相向双指针”（如用于排序数组的两数之和、反转字符串、盛水最多的容器）或“同向快慢指针”（如移除元素、有序数组去重）。在链表中，“快慢指针”几乎是标配，快指针走两步慢指针走一步，可完美解决寻找链表中点、判定链表是否有环等经典问题。', 1, 'algo', '官方题库,高频,牛客面经,算法,双指针', 1, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (85, '滑动窗口的核心思想是什么？', '请说明滑动窗口适用于什么类型的题。', '滑动窗口本质上是同向双指针的进阶版。它通过维护一个左右指针限定的“窗口”，在遍历过程中不断向右扩张窗口接纳新元素，当窗口内数据违反了某项条件时，再收缩左边界将旧元素吐出。这种机制将时间复杂度维持在 O(N)，极其适合解决数组或字符串中的“连续子序列”问题，比如求无重复字符的最长子串、长度最小的连续子数组等。', 1, 'algo', '官方题库,高频,牛客面经,算法,滑动窗口', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (86, '什么是哈希表思想？', '请说明哈希表在算法题中的常见作用。', '哈希表思想的精髓在于用少量的空间换取 O(1) 的极致查找时间。在遇到需要频繁校验某个元素“是否存在”、统计元素“出现频率”、或者建立“键值映射”记录之前访问过的数据下标时，哈希表是当之无愧的首选。典型的例题是力扣第一题“两数之和”，利用 HashMap 记录遍历过的值即可将暴力的 O(N^2) 降维打击到 O(N)。', 1, 'algo', '官方题库,高频,牛客面经,算法,哈希', 1, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (87, '栈和队列的典型应用有哪些？', '请说明两种线性结构在算法题中的常见场景。', '栈具有“后进先出（LIFO）”的特征，它天然适合处理具有对称性、递归回溯特点的问题，比如有效的括号匹配、逆波兰表达式求值以及系统的函数调用栈。队列则具有“先进先出（FIFO）”的特征，排队机制使得它在广度优先搜索（BFS）、二叉树的层序遍历以及操作系统任务调度中发挥核心作用。', 1, 'algo', '官方题库,高频,牛客面经,算法,栈队列', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (88, '什么是单调栈？', '请说明单调栈的使用场景。', '单调栈是指内部元素保持严格单调递增或递减的栈结构。当我们需要针对数组中的某个元素，找出它左边或右边“第一个比它大”或“第一个比它小”的元素时，单调栈能够做到 O(N) 一次遍历出结果。这常常用在寻找下一个更大元素、计算柱状图的最大矩形面积，或者是著名的“接雨水”问题中。', 2, 'algo', '官方题库,高频,牛客面经,算法,单调栈', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (89, '什么是单调队列？', '请说明单调队列为何适合滑动窗口最值问题。', '单调队列（通常用双端队列 Deque 实现）是一种一边从队头弹出过期元素，一边从队尾剔除无法成为最值的劣势元素，从而维持队列内部递减或递增的数据结构。它最经典的战场就是解决“滑动窗口最大值”问题，因为它能保证队头元素始终是当前窗口的最值，获取最值只需 O(1)，整体维持了 O(N) 的线性时间。', 2, 'algo', '官方题库,高频,牛客面经,算法,单调队列', 1, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (90, '二叉树的前序、中序、后序遍历分别是什么？', '请说明三种 DFS 遍历顺序。', '这三种遍历都属于深度优先搜索（DFS）。它们的命名是根据“根节点”的处理时机来划分的。前序遍历是“根-左-右”，常用于复制或打印整棵树；中序遍历是“左-根-右”，如果是二叉搜索树（BST），中序遍历能够得到一个完美升序的序列；后序遍历是“左-右-根”，常用于当你处理某节点需要依靠它子树的返回值时（如求树的高度、删除树）。', 1, 'algo', '官方题库,高频,牛客面经,算法,二叉树', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (91, '层序遍历和深度优先遍历有什么区别？', '请说明 BFS 与 DFS 的思想和使用场景。', '层序遍历对应广度优先搜索（BFS），通常借助队列实现，像水波纹一样按层级向外扩散。因为这种特性，它在图和树中是求“无权最短路径”或“最小跳跃次数”的唯一解法。深度优先遍历（DFS）通常借助递归或栈实现，沿着一条道走到黑再回头，它极其擅长做全排列、寻找所有可能路线组合以及树结构的自底向上统计分析。', 1, 'algo', '官方题库,高频,牛客面经,算法,图树', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (92, '如何判断一棵树是不是平衡二叉树？', '请说明常见解法思路。', '平衡二叉树要求所有节点的左右子树高度差绝对值不超过 1。如果从上往下暴力判断，会导致树的节点高度被重复计算，时间复杂度退化至 O(N^2)。高效做法是利用后序遍历（自底向上 DFS），在遍历计算高度的同时直接比对差值，一旦发现任何一个子树失衡，立刻返回一个特定的负数作为错误码层层短路，这样只需扫描一次 O(N) 即可判定。', 2, 'algo', '官方题库,高频,牛客面经,算法,二叉树', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (93, '回溯算法的核心模板是什么？', '请说明回溯与 DFS 的关系，以及常见题型。', '回溯算法本质上就是带有“撤销动作”的纯暴力深度优先搜索（DFS），用来穷举所有的解空间。它的模板非常固定：在 for 循环中尝试“做选择”，接着“递归”进入下一层继续尝试，等递归完成后立马执行“撤销选择”，以便让状态干净地迎接下一次尝试。它主要用于搞定各种组合、全排列、子集以及经典的 N 皇后和数独问题。', 2, 'algo', '官方题库,高频,牛客面经,算法,回溯', 1, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (94, '动态规划的本质是什么？', '请说明动态规划为什么强调最优子结构和状态转移。', '动态规划（DP）的本质是用空间换时间，将一个庞大复杂的原问题拆解成若干个会“重叠”的子问题。为了避免像纯递归那样做海量重复计算，DP 通过一个数组（dp-table）把子问题的结果存起来。这要求原问题必须具备“最优子结构”（即局部最优可以推导全局最优），并通过明确的“状态转移方程”一步步由小问题的结果推导出终点大问题的答案。', 2, 'algo', '官方题库,高频,牛客面经,算法,动态规划', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (95, '背包问题为什么是动态规划经典题？', '请说明 0-1 背包的状态设计思路。', '背包问题包含了最典型的决策特征（选或不选）。在 0-1 背包中，我们定义状态 dp[i][j] 为：在前 i 个物品中做选择，当背包剩余容量为 j 时，能获取的最大价值。对于第 i 个物品，我们只有两种策略：装不下直接继承上层价值；装得下就在“继承上层价值”和“当前价值+留出空间装前面物品价值”两者中取最大值。这种极具代表性的二维状态转移也是所有进阶 DP 的地基。', 2, 'algo', '官方题库,高频,牛客面经,算法,动态规划', 1, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (96, '贪心算法和动态规划有什么区别？', '请说明两者的决策方式差异。', '贪心算法像个短视者，每一步都毫不犹豫地做出当前状态下局部看起来最好的选择，而且绝不回头，只有当这种局部最优一定能无伤推演到全局最优时（如发饼干、找零钱），贪心才管用。而动态规划更像统筹全局的智者，它在每一个阶段都会全面考量所有的历史子状态，在推演中对比并选取真正具有长远效益的最优解，更稳妥严谨。', 2, 'algo', '官方题库,高频,牛客面经,算法,贪心', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (97, 'BFS 常用于哪些题型？', '请说明 BFS 的典型应用。', '广度优先搜索（BFS）通过队列按距离起始点的远近圈层一层层往外扒。因为这种天然的水波纹扩散特性，只要是在无向无权网格图或树结构中碰到了“求最短路径”、“最少移动步数”以及“走迷宫”问题，BFS 绝对是唯一也是最高效的正解。同时在二叉树的序列化及层序打印问题中也是必用手段。', 1, 'algo', '官方题库,高频,牛客面经,算法,BFS', 1, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (98, '并查集适合解决什么问题？', '请说明并查集的典型使用场景。', '并查集（Union-Find）是一个专为判定“动态图连通性”而生的极致数据结构。对于海量节点判定谁和谁属于同一个阵营、判定新加一条边是否会构成环，传统的深搜广搜效率低下。并查集通过维护一个 parent 数组，配合精妙的“路径压缩（查找时直接将节点挂载根节点下）”与“按秩合并（小树挂大树）”，让查找两点是否连通以及合并集合的操作近乎于 O(1)。常用于岛屿数量变形、冗余连接题型。', 2, 'algo', '官方题库,高频,牛客面经,算法,并查集', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (99, 'Top K 问题有哪些常见做法？', '请说明排序、堆、快速选择的适用场景。', '解决取前 K 大/前 K 小的数据场景：1. 如果数据量极小，直接全局快排取前 K 个即可；2. 对于实时的大批量流水数据或海量流数据，采用大小为 K 的顶堆最佳（求前 K 大用小顶堆），它能将时间控制在 O(NlogK)，且内存消耗极低；3. 对于确定大小的静态庞大数组，借助快排的 partition 划分思想做“快速选择”，可以在平均 O(N) 的极限时间内快速卡出 Top K 边界。', 2, 'algo', '官方题库,高频,牛客面经,算法,TopK', 1, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (100, '如何在面试中讲清一道算法题？', '请说明比较完整的答题结构。', '面试考察的不光是代码能力更是沟通水平。第一步，先复述并确认边界条件（如空数组、负数情况）；第二步，别急着写最优解，先抛出一种好理解的暴力或者朴素解法兜底；第三步，点出朴素解法的时间/空间痛点，自然引出你的优化思路和对应的数据结构；第四步，在写伪代码或真实代码时加入注释并向面试官讲解流转过程；最后一步，主动抛出两三个极端的测试用例来证明代码鲁棒性。', 1, 'algo', '官方题库,高频,牛客面经,算法,面试技巧', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (101, '进程和线程有什么区别？', '请从资源分配、调度切换、通信方式和崩溃影响范围等角度说明区别。', '进程是操作系统进行“资源分配”的基本单位，拥有独立的代码和数据空间；线程则是 CPU 进行“调度执行”的最小单位，多个线程共享所在进程的堆内存与资源。由于共享，线程间的切换非常轻量、通信很方便（不需要陷入内核的 IPC 通信），但同时也会因为资源共享带来严重的并发安全问题；而进程之间互相隔离，一个进程崩溃哪怕出现段错误通常不会牵连别人，系统更健壮。', 1, 'linux', '官方题库,高频,JavaGuide,linux,操作系统,进程线程', 1, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (102, '用户态和内核态有什么区别？', '请说明用户态、内核态的职责划分，以及为什么要有系统调用。', '为了保护系统安全，CPU 把指令划分为高特权级和低特权级。普通业务代码都跑在用户态，权限被严格限制，不能直接去操作磁盘读写网络和物理内存。而操作系统的核心代码跑在内核态，拥有对底层硬件的绝对掌控权。当用户程序需要读个文件或发个网卡包时，不能自己硬来，必须主动触发“系统调用”产生中断，将执行权移交给更高级的内核态来代为执行，完成后再切回用户态。', 2, 'linux', '官方题库,高频,JavaGuide,linux,操作系统,内核', 1, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (103, '什么是系统调用？', '请说明系统调用在应用程序和操作系统之间的作用。', '系统调用（System Call）就像是操作系统开放给第三方应用的“安全 API 防火墙”。诸如创建进程（fork）、读写文件（read/write）以及建立网络（socket）这类直接调动机器核心部件的操作，全部被内核封装成了系统调用。程序一旦执行系统调用，CPU 会保存当前现场并立马切换陷入内核态处理。这是维持操作系统底层秩序与稳定的最关键隔离墙。', 1, 'linux', '官方题库,高频,小林coding,linux,操作系统', 0, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (104, 'CPU 100% 时如何排查？', '请给出线上排查高 CPU 的常见步骤和命令。', '首先使用 top 命令观察系统概况，揪出那个占据 CPU 最高百分比的故障进程 PID。紧接着，用 top -Hp <PID> 或 ps -mp <PID> -o THREAD 查看该进程内部是哪个倒霉线程疯狂运转，记下它的线程 ID。把该十进制 ID 转为十六进制。最后，使用 jdk 自带的 jstack <PID> | grep -A 20 <十六进制ID>，直接在打印出的运行栈中精准锁定导致死循环或疯狂 GC 的具体 Java 代码行数。', 2, 'linux', '官方题库,高频,牛客面经,linux,排查', 1, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (105, '内存占用过高时如何排查？', '请说明常见排查思路和命令。', '先用 free -m 命令看一下整体物理内存消耗和 Swap 区使用率，判断大环境。接着 top 排一下占用内存最多的嫌疑进程。如果是 Java 服务，十有八九是发生了 OOM 或者严重泄露。立刻使用 jmap -histo <PID> 粗略查看哪个对象实例数量飙得最狠，或者直接用 jmap -dump 导出一份实时的堆内存快照 Dump 文件，拉到本地用 MAT（Memory Analyzer）去分析那些占据深层大内存对象到 GC Root 的引用链条。', 2, 'linux', '官方题库,高频,牛客面经,linux,排查', 1, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (106, '端口被占用时常用哪些命令？', '请列举查看端口占用和定位进程的常见命令。', '排查端口占用最常用的是 lsof -i:端口号（例如 lsof -i:8080），它会直接罗列出是哪个进程的名字和 PID 正在窃听这个端口。如果没有装 lsof，老牌的 netstat -tunlp | grep 端口号 或者更新更好的 ss -tunlp | grep 端口号 同样能精确暴露出背后占用该 TCP/UDP 端口的程序详情。拿到 PID 后直接 kill -9 即可恢复端口自由。', 1, 'linux', '官方题库,高频,JavaGuide,linux,命令,排查', 1, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (107, 'top、ps、grep、tail 各有什么用途？', '请说明这些常见 Linux 命令在线上排查中的典型作用。', '线上救火四大天王：top 是“全景监控雷达”，能动态刷新机器整体 CPU、内存以及各大耗时进程排名；ps 是“进程照妖镜”，ps -ef 常用来精确捕捉某个服务当前的启动参数和状态；grep 则是无所不能的“文本过滤神器”，常接在管道符后迅速在茫茫日志海中捞出关键词报错；tail 是“实时追踪器”，tail -f xx.log 让开发能够一边在界面点击，一边动态紧盯后台滚动的日志。', 1, 'linux', '官方题库,高频,JavaGuide,linux,命令', 1, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (108, '软链接和硬链接有什么区别？', '请说明软链接和硬链接在 inode、跨文件系统和删除源文件后的差异。', '软链接（符号链接）就类似 Windows 里的“快捷方式”，它拥有自己独立的 inode 号，里头存的内容仅仅是目标文件的具体路径，它可以随意跨分区跨文件系统建立；源文件一旦被删，软链接直接爆红失效。而硬链接则是直接指向了同一个文件的实体 inode，它只是给文件多绑了一个名字而已，严禁跨文件系统创建；哪怕你把源文件名给删了，只要硬链接还在，那个底层文件实体的数据依然安然无恙且可正常使用。', 2, 'linux', '官方题库,高频,小林coding,linux,文件系统', 3, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (109, '文件权限 rwx 分别代表什么？', '请说明 Linux 文件权限模型的基本含义。', '这是 Linux 文件安全基石。r 代表 Read 可读，针对文件意味着能查看文件内容，针对目录意味着能 ls 看见里面装了什么文件；w 代表 Write 可写，对文件是能修改甚至删除内容，对目录是能往里新建或删除文件；x 代表 eXecute 可执行，对文件是能把它当脚本或二进制程序运行，对目录则是能 cd 切进该目录的绝对通行证。', 1, 'linux', '官方题库,高频,linux,命令,权限', 4, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (110, 'chmod 755 代表什么？', '请说明 755 这种数字权限表示方式。', 'Linux 把 r、w、x 映射成了三个数字：r=4，w=2，x=1。第一位数字代表“文件拥有者（User）”的权限，第二位代表“同用户组（Group）”权限，第三位则是“其他陌生人（Other）”权限。755 即：属主拥有 4+2+1=7 的满贯读写执行权；属组和外人只有 4+1=5 的纯读和执行权，唯独没有写入篡改的权限。这是存放大部分脚本最常见也最安全的设置。', 1, 'linux', '官方题库,高频,linux,命令,权限', 4, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (111, '死锁的必要条件有哪些？', '请说明死锁的四个必要条件，并给出常见规避思路。', '想酿成死锁灾难必须集齐四颗龙珠：1. 互斥条件：一把锁同时只能一个人占着；2. 请求并保持条件：自己拿着锁不放，还眼巴巴去申请别人手里的锁；3. 不可剥夺条件：别人拿到的锁，除非他主动放手，你不能去硬抢；4. 环路等待条件：A 等 B，B 等 C，C 又等 A，形成闭环。破解死锁最有效的手段通常是破坏第四点，即在整个架构设计中规定“一律按照固定先后顺序申请锁”。', 2, 'linux', '官方题库,高频,JavaGuide,linux,操作系统,死锁', 3, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (112, '什么是页表和虚拟内存？', '请说明虚拟内存如何帮助进程获得独立地址空间。', '在现代操作系统中，程序里的代码根本不知道什么是物理内存。操作系统用一种叫“虚拟内存”的障眼法，给每一个进程都骗取了一块私有的、连续巨大的虚拟地址空间。当进程需要动用内存时，硬件上的 MMU 配合操作系统维护的“页表”，将这个虚拟地址快速翻译并映射到真实世界中碎片化的物理内存块上。这种虚拟化不仅极大拉高了隔离安全性，配合 Swap 还能让内存显得比物理条还大。', 2, 'linux', '官方题库,高频,小林coding,linux,内存', 6, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (113, '什么是缺页中断？', '请说明缺页中断发生的原因。', '因为物理内存金贵，操作系统通常采取“用到再说”的懒汉模式。当一个进程傻乎乎地去访问自己虚拟地址里某块内存时，CPU 查页表发现：“抱歉，这块虚拟页面压根还没对应任何实质物理内存，或者早就被 Swap 踢到磁盘上去了”。此时 CPU 就会立刻抛出一个硬件级别的异常信号，这叫缺页中断（Page Fault）。内核捕捉到这个信号后，会火速去硬盘里找数据，搬进空闲的物理内存中，更新好页表，最后再让程序继续跑。', 2, 'linux', '官方题库,高频,小林coding,linux,内存', 4, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (114, '阻塞 IO、非阻塞 IO、IO 多路复用有什么区别？', '请说明三者的基本差异。', '老掉牙的“阻塞 I/O”在网卡还没收到数据前，会让线程彻底陷入沉睡挂起，干不了任何杂事，十分浪费资源。“非阻塞 I/O”则是个急性子，没数据就立刻报错返回不睡，但为了拿到数据它只能频繁疯狂去轮询试探，极其消耗 CPU。“I/O 多路复用”才是高并发真理：它派出一个高管线程拿着一份清单统一去监听成百上千个网络连接，哪个连接有了动静它就回调通知谁，用极低的人工成本盘活了整个大网网络。', 2, 'linux', '官方题库,高频,小林coding,linux,IO', 8, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (115, 'select、poll、epoll 有什么区别？', '请从 fd 数量限制、遍历成本和触发机制角度说明差异。', 'select 是爷爷辈，它受限于古老的宏定义，最多只能盯着 1024 个 fd，而且每次有动静它都得全量 O(N) 盲目遍历查明是谁。poll 改善了底层的数组结构，打破了数量天花板，但依然改变不了 O(N) 遍历的蠢方法。而 epoll 是神，它在内核中维持一棵红黑树记录监控列表，并且利用高级的事件回调驱动，哪个 fd 数据就绪了就把它塞进就绪队列里，时间复杂度 O(1)，这是如今 Nginx、Redis、Netty 封神的共同基石。', 3, 'linux', '官方题库,高频,小林coding,linux,IO', 17, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (116, '僵尸进程和孤儿进程是什么？', '请说明两者产生原因和影响。', '孤儿进程很惨，它的父进程因为异常先崩溃死掉了，导致它成了没人管的孤儿，但别慌，系统里的 1 号 init 进程会大慈大悲收养它，等它结束后妥善安排后事，对机器无害。而僵尸进程很恶心，子进程已经跑完了死掉了，但它的傻父亲一直不调用 wait() 去接收它的遗言状态，导致这个死人一直霸占着内核里的“进程控制块槽位”。一旦僵尸进程积累太多，系统的可用 PID 将会被彻底耗尽，无法再开启新任务。', 2, 'linux', '官方题库,高频,小林coding,linux,进程', 61, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (117, '什么是上下文切换？', '请说明进程/线程上下文切换为何会带来性能开销。', '由于 CPU 的核心太少而要跑的任务太多，系统必须轮流给每个任务分配时间片。当时间片用完或者任务被高级信号打断，CPU 必须把当前任务进行到一半的私人现场（各种高贵的 CPU 寄存器、程序计数器指令位置）原封不动地全打包保存到内存里，紧接着再去加载下一个任务上次保留的烂摊子。如果是进程间切换，甚至还要清空并刷新页表、TLB 缓存。这种繁复的打包和装载极其浪费 CPU 宝贵的计算周期。', 2, 'linux', '官方题库,高频,linux,操作系统', 51, 1, '2026-04-14 09:31:36', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (122, '请解释数据库索引失效的常见原因。', '请解释数据库索引失效的常见原因。', '函数计算、隐式类型转换、前导模糊匹配等会导致索引失效。测试阿里时代峰峻拉屎拉时间过啦送达给拉萨家里的赶紧阿里可视对讲干垃圾阿拉手打高考啦伤筋动骨 辣椒水劳动工具辣三丁过拉屎', 2, '其他', 'official-bank,题库导入模板', 14, 1, '2026-04-21 10:27:13', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (123, '请说明缓存穿透和缓存击穿的区别。', '请说明缓存穿透和缓存击穿的区别。', '缓存穿透是查询不存在数据，缓存击穿是热点 key 失效瞬间并发打到数据库。', 2, '其他', 'official-bank,题库导入模板', 11, 1, '2026-04-21 10:27:13', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (124, '请解释单体架构、分层架构、微服务架构的区别，以及它们分别适用于什么场景。', '请解释单体架构、分层架构、微服务架构的区别，以及它们分别适用于什么场景。', '单体架构通常把业务模块部署成一个整体应用，开发、测试和部署链路相对简单，适合业务规模较小、团队人数不多、需求变化节奏可控的场景。分层架构更强调表示层、业务层、数据访问层的职责分离，适合中小型业务系统的规范化开发。微服务架构则按业务能力拆分服务，每个服务可独立开发、部署和扩缩容，更适合复杂业务、大团队协作和需要高弹性的系统，但同时也会带来服务治理、调用链复杂、数据一致性和运维成本上升的问题。', 2, 'arch', 'official-bank,ai', 0, 1, '2026-04-21 18:24:14', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (125, '什么是高内聚、低耦合？为什么这是架构设计中的核心原则？', '什么是高内聚、低耦合？为什么这是架构设计中的核心原则？', '高内聚指一个模块内部的职责尽量集中、围绕单一业务目标组织；低耦合指模块之间依赖尽量少，且依赖边界清晰稳定。这样做的价值在于降低修改一个模块时对其他模块的影响范围，提升系统的可维护性、可测试性和可演进性。很多架构问题的本质并不是“技术选型错了”，而是模块边界混乱、职责不清导致系统逐渐失控。', 2, 'arch', 'official-bank,ai', 0, 1, '2026-04-21 18:24:14', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (126, '为什么说架构设计的第一步不是选技术，而是识别业务边界？', '为什么说架构设计的第一步不是选技术，而是识别业务边界？', '因为架构首先服务于业务目标，而不是为了展示技术栈。只有先识别核心域、子域、职责边界、关键链路和非功能性需求，才能决定系统该按什么方式拆分、哪些能力应该优先抽象、哪些数据应该隔离。脱离业务边界直接谈微服务、消息队列或缓存，往往会把问题复杂化，而不是解决问题。', 2, 'arch', 'official-bank,ai', 0, 1, '2026-04-21 18:24:14', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (127, '请说明垂直拆分和水平拆分的区别。', '请说明垂直拆分和水平拆分的区别。', '垂直拆分一般是按业务领域或模块职责来拆，比如订单、支付、库存各自独立；水平拆分通常是针对同一类服务或数据做分片扩展，比如按用户 ID 或地域拆库拆表。垂直拆分解决的是职责边界和团队协作问题，水平拆分解决的是单点容量和性能瓶颈问题。两者并不冲突，复杂系统中通常会组合使用。', 2, 'arch', 'official-bank,ai', 1, 1, '2026-04-21 18:24:14', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (128, '什么是领域驱动设计（DDD）中的限界上下文？为什么它对服务拆分有指导意义？', '什么是领域驱动设计（DDD）中的限界上下文？为什么它对服务拆分有指导意义？', '限界上下文可以理解为某一套业务模型、术语和规则成立的边界。在这个边界内，概念定义是一致的；出了边界，同一个词可能有不同含义。它的重要性在于帮助团队避免“一个模型试图统治全系统”的问题，为服务拆分和数据归属提供自然边界。微服务拆分如果脱离了限界上下文，最终很容易拆成“技术服务”而不是“业务服务”。', 2, 'arch', 'official-bank,ai', 0, 1, '2026-04-21 18:24:14', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (129, '请解释 CAP 理论，并说明在真实系统中应该如何理解它。', '请解释 CAP 理论，并说明在真实系统中应该如何理解它。', 'CAP 理论指出，在分布式系统发生网络分区时，一致性（Consistency）和可用性（Availability）无法同时被完美满足，系统需要做取舍。真实系统里更准确的理解不是“平时三选二”，而是“当网络分区发生时必须在一致性和可用性之间权衡”。因此很多架构决策本质上是在不同业务场景下选择强一致、最终一致，还是优先保障服务可用。', 2, 'arch', 'official-bank,ai', 0, 1, '2026-04-21 18:24:14', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (130, '什么是最终一致性？它适合哪些业务，不适合哪些业务？', '什么是最终一致性？它适合哪些业务，不适合哪些业务？', '最终一致性指系统在短时间内允许副本或子系统状态暂时不一致，但经过同步和补偿后最终达到一致。它适合消息驱动、异步处理、数据分析、库存预留、通知下发等可以容忍短暂延迟的业务。不适合资金实时扣减、核心账务记账这类对强一致要求极高的场景。是否选择最终一致性，本质上取决于业务是否能接受“短暂不一致”。', 2, 'arch', 'official-bank,ai', 0, 1, '2026-04-21 18:24:14', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (131, '请说明为什么“每个微服务独立数据库”是一条重要原则。', '请说明为什么“每个微服务独立数据库”是一条重要原则。', '如果多个服务共享同一个数据库，虽然短期开发会更快，但服务边界会被数据库反向耦合：表结构修改互相影响、服务之间绕过接口直接读写数据、自治能力下降。独立数据库的核心价值不是“形式上隔离”，而是确保服务真正拥有自己的数据和演进节奏。当然，这也会带来跨服务查询和分布式事务问题，所以需要结合 CQRS、缓存视图、事件同步等手段处理。', 2, 'arch', 'official-bank,ai', 0, 1, '2026-04-21 18:24:14', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (132, '请解释 CQRS 的核心思想，以及它为什么常出现在复杂系统中。', '请解释 CQRS 的核心思想，以及它为什么常出现在复杂系统中。', 'CQRS 即命令查询职责分离，核心思想是把“写模型”和“读模型”分开设计。写侧强调业务约束、事务和一致性，读侧强调查询性能和展示效率。它适合读写模式差异很大、查询维度复杂、聚合代价高的系统。之所以常见于复杂系统，是因为很多场景用同一套模型同时兼顾事务写入和高性能查询会变得越来越困难。', 2, 'arch', 'official-bank,ai', 0, 1, '2026-04-21 18:24:14', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (133, '请解释事件驱动架构的优势与代价。', '请解释事件驱动架构的优势与代价。', '事件驱动架构通过事件总线或消息中间件把系统从同步调用链改为异步协作，优势是削峰填谷、降低耦合、提升扩展性，并让不同子系统围绕事件做独立演进。代价是链路变得不可见、排障更复杂、数据一致性更依赖补偿机制，且对幂等、重试、消息顺序和监控告警的要求更高。它并不是“天然更高级”，而是更适合需要异步解耦和高吞吐的系统。', 2, 'arch', 'official-bank,ai', 0, 1, '2026-04-21 18:24:14', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (134, '什么是 Saga 模式？它解决了什么问题？', '什么是 Saga 模式？它解决了什么问题？', 'Saga 是一种用于微服务分布式事务的设计模式，它把跨多个服务的长事务拆成一系列本地事务，并通过补偿操作在失败时回滚业务影响。它解决的是“多个自治服务之间无法使用单库事务，但业务上又需要整体完成”的问题。Saga 不保证严格意义上的原子性，而是通过前滚或补偿的方式尽量恢复业务一致。', 2, 'arch', 'official-bank,ai', 0, 1, '2026-04-21 18:24:14', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (135, '请说明为什么大型系统更关注“隔离”和“降级”，而不仅仅是“性能优化”。', '请说明为什么大型系统更关注“隔离”和“降级”，而不仅仅是“性能优化”。', '因为真实线上故障很多不是慢一点，而是局部异常迅速放大成级联故障。隔离是为了把问题限制在局部，比如线程池隔离、服务隔离、租户隔离；降级是为了在资源不足或依赖异常时优先保住核心功能。相比单纯追求极限性能，这些机制更能决定系统在高压和故障下能否活下来。', 2, 'arch', 'official-bank,ai', 0, 1, '2026-04-21 18:24:14', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (136, '什么是负载均衡？它在架构设计中不只是“分流请求”这么简单，为什么？', '什么是负载均衡？它在架构设计中不只是“分流请求”这么简单，为什么？', '负载均衡表面上是把流量分配给多台实例，本质上承担了高可用入口、故障摘除、扩缩容承接、就近访问和流量治理等职责。一个设计良好的负载均衡体系，不仅能提高吞吐，还能配合健康检查、区域容灾、会话策略和限流策略共同保障服务稳定。所以它并不是单纯的“流量均分器”，而是架构可用性的重要支点。', 2, 'arch', 'official-bank,ai', 1, 1, '2026-04-21 18:24:14', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (137, '为什么说幂等设计是分布式系统中的基础能力？', '为什么说幂等设计是分布式系统中的基础能力？', '因为在网络抖动、超时、重试、消息重复投递等情况下，同一个请求被执行多次是很常见的。如果操作不具备幂等性，就可能出现重复扣款、重复下单、重复发券等严重问题。幂等设计的本质，是让“多次执行”和“一次执行”的最终业务结果一致，这是安全重试和异步解耦的前提。', 2, 'arch', 'official-bank,ai', 3, 1, '2026-04-21 18:24:14', 'OFFICIAL', NULL, NULL);
INSERT INTO `questions` VALUES (138, '请解释为什么现代架构设计越来越强调可观测性，而不是只靠日志排查。', '请解释为什么现代架构设计越来越强调可观测性，而不是只靠日志排查。', '因为分布式系统中的故障往往跨越多个服务、多个线程池、多个中间件，仅靠单点日志很难还原完整链路。可观测性强调日志、指标、链路追踪三者结合，用来回答“系统发生了什么、为什么发生、影响到哪里”。随着服务数量和异步链路增加，可观测性已经从运维增强项变成架构必备能力。', 2, 'arch', 'official-bank,ai', 1, 1, '2026-04-21 18:24:14', 'OFFICIAL', NULL, NULL);

-- ----------------------------
-- Table structure for user_question_mastery
-- ----------------------------
DROP TABLE IF EXISTS `user_question_mastery`;
CREATE TABLE `user_question_mastery`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '鍋氶鐢ㄦ埛ID',
  `question_id` bigint NOT NULL COMMENT '棰樼洰ID',
  `confirmed_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鐢ㄦ埛纭浼氬仛鐨勬椂闂�',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_question_mastery_user_question`(`user_id` ASC, `question_id` ASC) USING BTREE,
  INDEX `idx_user_question_mastery_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_user_question_mastery_question`(`question_id` ASC) USING BTREE,
  CONSTRAINT `fk_user_question_mastery_question` FOREIGN KEY (`question_id`) REFERENCES `questions` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_user_question_mastery_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 72 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '鐢ㄦ埛棰樼洰鎺屾彙纭璁板綍' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_question_mastery
-- ----------------------------
INSERT INTO `user_question_mastery` VALUES (5, 40, 1, '2026-04-01 12:11:11', '2026-04-01 12:11:11', '2026-04-01 12:11:11');
INSERT INTO `user_question_mastery` VALUES (6, 40, 2, '2026-04-01 13:11:12', '2026-04-01 13:11:12', '2026-04-01 13:11:12');
INSERT INTO `user_question_mastery` VALUES (7, 40, 3, '2026-04-01 14:11:12', '2026-04-01 14:11:12', '2026-04-01 14:11:12');
INSERT INTO `user_question_mastery` VALUES (8, 40, 4, '2026-04-01 15:11:12', '2026-04-01 15:11:12', '2026-04-01 15:11:12');
INSERT INTO `user_question_mastery` VALUES (9, 40, 5, '2026-04-01 16:11:12', '2026-04-01 16:11:12', '2026-04-01 16:11:12');
INSERT INTO `user_question_mastery` VALUES (10, 40, 6, '2026-04-01 17:11:12', '2026-04-01 17:11:12', '2026-04-01 17:11:12');
INSERT INTO `user_question_mastery` VALUES (11, 40, 7, '2026-04-01 18:11:12', '2026-04-01 18:11:12', '2026-04-01 18:11:12');
INSERT INTO `user_question_mastery` VALUES (12, 40, 8, '2026-04-01 19:11:12', '2026-04-01 19:11:12', '2026-04-01 19:11:12');
INSERT INTO `user_question_mastery` VALUES (13, 41, 5, '2026-04-02 12:11:12', '2026-04-02 12:11:12', '2026-04-02 12:11:12');
INSERT INTO `user_question_mastery` VALUES (14, 41, 6, '2026-04-02 13:11:12', '2026-04-02 13:11:12', '2026-04-02 13:11:12');
INSERT INTO `user_question_mastery` VALUES (15, 41, 7, '2026-04-02 14:11:12', '2026-04-02 14:11:12', '2026-04-02 14:11:12');
INSERT INTO `user_question_mastery` VALUES (16, 41, 8, '2026-04-02 15:11:12', '2026-04-02 15:11:12', '2026-04-02 15:11:12');
INSERT INTO `user_question_mastery` VALUES (17, 41, 9, '2026-04-02 16:11:12', '2026-04-02 16:11:12', '2026-04-02 16:11:12');
INSERT INTO `user_question_mastery` VALUES (18, 41, 10, '2026-04-02 17:11:12', '2026-04-02 17:11:12', '2026-04-02 17:11:12');
INSERT INTO `user_question_mastery` VALUES (19, 41, 11, '2026-04-02 18:11:12', '2026-04-02 18:11:12', '2026-04-02 18:11:12');
INSERT INTO `user_question_mastery` VALUES (20, 42, 9, '2026-04-03 12:11:12', '2026-04-03 12:11:12', '2026-04-03 12:11:12');
INSERT INTO `user_question_mastery` VALUES (21, 42, 10, '2026-04-03 13:11:12', '2026-04-03 13:11:12', '2026-04-03 13:11:12');
INSERT INTO `user_question_mastery` VALUES (22, 42, 11, '2026-04-03 14:11:12', '2026-04-03 14:11:12', '2026-04-03 14:11:12');
INSERT INTO `user_question_mastery` VALUES (23, 42, 12, '2026-04-03 15:11:12', '2026-04-03 15:11:12', '2026-04-03 15:11:12');
INSERT INTO `user_question_mastery` VALUES (24, 42, 13, '2026-04-03 16:11:12', '2026-04-03 16:11:12', '2026-04-03 16:11:12');
INSERT INTO `user_question_mastery` VALUES (25, 42, 14, '2026-04-03 17:11:12', '2026-04-03 17:11:12', '2026-04-03 17:11:12');
INSERT INTO `user_question_mastery` VALUES (26, 43, 13, '2026-04-04 12:11:12', '2026-04-04 12:11:12', '2026-04-04 12:11:12');
INSERT INTO `user_question_mastery` VALUES (27, 43, 14, '2026-04-04 13:11:12', '2026-04-04 13:11:12', '2026-04-04 13:11:12');
INSERT INTO `user_question_mastery` VALUES (28, 43, 15, '2026-04-04 14:11:12', '2026-04-04 14:11:12', '2026-04-04 14:11:12');
INSERT INTO `user_question_mastery` VALUES (29, 43, 16, '2026-04-04 15:11:12', '2026-04-04 15:11:12', '2026-04-04 15:11:12');
INSERT INTO `user_question_mastery` VALUES (30, 43, 17, '2026-04-04 16:11:12', '2026-04-04 16:11:12', '2026-04-04 16:11:12');
INSERT INTO `user_question_mastery` VALUES (31, 43, 18, '2026-04-04 17:11:12', '2026-04-04 17:11:12', '2026-04-04 17:11:12');
INSERT INTO `user_question_mastery` VALUES (32, 44, 17, '2026-04-05 12:11:12', '2026-04-05 12:11:12', '2026-04-05 12:11:12');
INSERT INTO `user_question_mastery` VALUES (33, 44, 18, '2026-04-05 13:11:12', '2026-04-05 13:11:12', '2026-04-05 13:11:12');
INSERT INTO `user_question_mastery` VALUES (34, 44, 19, '2026-04-05 14:11:12', '2026-04-05 14:11:12', '2026-04-05 14:11:12');
INSERT INTO `user_question_mastery` VALUES (35, 44, 20, '2026-04-05 15:11:12', '2026-04-05 15:11:12', '2026-04-05 15:11:12');
INSERT INTO `user_question_mastery` VALUES (36, 44, 21, '2026-04-05 16:11:12', '2026-04-05 16:11:12', '2026-04-05 16:11:12');
INSERT INTO `user_question_mastery` VALUES (37, 45, 21, '2026-04-06 12:11:12', '2026-04-06 12:11:12', '2026-04-06 12:11:12');
INSERT INTO `user_question_mastery` VALUES (38, 45, 22, '2026-04-06 13:11:12', '2026-04-06 13:11:12', '2026-04-06 13:11:12');
INSERT INTO `user_question_mastery` VALUES (39, 45, 23, '2026-04-06 14:11:12', '2026-04-06 14:11:12', '2026-04-06 14:11:12');
INSERT INTO `user_question_mastery` VALUES (40, 45, 24, '2026-04-06 15:11:12', '2026-04-06 15:11:12', '2026-04-06 15:11:12');
INSERT INTO `user_question_mastery` VALUES (41, 46, 25, '2026-04-07 12:11:12', '2026-04-07 12:11:12', '2026-04-07 12:11:12');
INSERT INTO `user_question_mastery` VALUES (42, 46, 26, '2026-04-07 13:11:12', '2026-04-07 13:11:12', '2026-04-07 13:11:12');
INSERT INTO `user_question_mastery` VALUES (43, 46, 27, '2026-04-07 14:11:12', '2026-04-07 14:11:12', '2026-04-07 14:11:12');
INSERT INTO `user_question_mastery` VALUES (44, 46, 28, '2026-04-07 15:11:12', '2026-04-07 15:11:12', '2026-04-07 15:11:12');
INSERT INTO `user_question_mastery` VALUES (45, 47, 29, '2026-04-08 12:11:12', '2026-04-08 12:11:12', '2026-04-08 12:11:12');
INSERT INTO `user_question_mastery` VALUES (46, 47, 30, '2026-04-08 13:11:12', '2026-04-08 13:11:12', '2026-04-08 13:11:12');
INSERT INTO `user_question_mastery` VALUES (47, 47, 31, '2026-04-08 14:11:12', '2026-04-08 14:11:12', '2026-04-08 14:11:12');
INSERT INTO `user_question_mastery` VALUES (48, 48, 33, '2026-04-09 12:11:12', '2026-04-09 12:11:12', '2026-04-09 12:11:12');
INSERT INTO `user_question_mastery` VALUES (49, 48, 34, '2026-04-09 13:11:12', '2026-04-09 13:11:12', '2026-04-09 13:11:12');
INSERT INTO `user_question_mastery` VALUES (50, 48, 35, '2026-04-09 14:11:12', '2026-04-09 14:11:12', '2026-04-09 14:11:12');
INSERT INTO `user_question_mastery` VALUES (51, 39, 117, '2026-04-19 14:22:56', '2026-04-19 14:22:56', '2026-04-19 14:22:56');
INSERT INTO `user_question_mastery` VALUES (52, 49, 116, '2026-04-19 14:23:45', '2026-04-19 14:23:45', '2026-04-19 14:23:45');
INSERT INTO `user_question_mastery` VALUES (53, 39, 116, '2026-04-19 14:28:08', '2026-04-19 14:28:08', '2026-04-19 14:28:08');
INSERT INTO `user_question_mastery` VALUES (54, 49, 115, '2026-04-19 14:38:42', '2026-04-19 14:38:42', '2026-04-19 14:38:42');
INSERT INTO `user_question_mastery` VALUES (55, 49, 114, '2026-04-19 14:38:45', '2026-04-19 14:38:45', '2026-04-19 14:38:45');
INSERT INTO `user_question_mastery` VALUES (56, 49, 113, '2026-04-19 14:38:48', '2026-04-19 14:38:48', '2026-04-19 14:38:48');
INSERT INTO `user_question_mastery` VALUES (57, 49, 112, '2026-04-19 14:38:52', '2026-04-19 14:38:52', '2026-04-19 14:38:52');
INSERT INTO `user_question_mastery` VALUES (58, 49, 111, '2026-04-19 14:38:55', '2026-04-19 14:38:55', '2026-04-19 14:38:55');
INSERT INTO `user_question_mastery` VALUES (59, 49, 110, '2026-04-19 14:38:59', '2026-04-19 14:38:59', '2026-04-19 14:38:59');
INSERT INTO `user_question_mastery` VALUES (60, 49, 109, '2026-04-19 14:39:02', '2026-04-19 14:39:02', '2026-04-19 14:39:02');
INSERT INTO `user_question_mastery` VALUES (61, 49, 108, '2026-04-19 14:39:06', '2026-04-19 14:39:06', '2026-04-19 14:39:06');
INSERT INTO `user_question_mastery` VALUES (62, 1, 117, '2026-04-20 22:51:30', '2026-04-20 22:51:30', '2026-04-20 22:51:30');
INSERT INTO `user_question_mastery` VALUES (63, 1, 116, '2026-04-20 22:51:41', '2026-04-20 22:51:41', '2026-04-20 22:51:41');
INSERT INTO `user_question_mastery` VALUES (64, 1, 115, '2026-04-20 22:55:30', '2026-04-20 22:55:30', '2026-04-20 22:55:30');
INSERT INTO `user_question_mastery` VALUES (65, 1, 114, '2026-04-20 22:55:54', '2026-04-20 22:55:54', '2026-04-20 22:55:54');
INSERT INTO `user_question_mastery` VALUES (66, 1, 113, '2026-04-20 22:58:28', '2026-04-20 22:58:28', '2026-04-20 22:58:28');
INSERT INTO `user_question_mastery` VALUES (67, 1, 112, '2026-04-20 22:58:58', '2026-04-20 22:58:58', '2026-04-20 22:58:58');
INSERT INTO `user_question_mastery` VALUES (68, 49, 123, '2026-04-21 11:23:05', '2026-04-21 11:23:05', '2026-04-21 11:23:05');
INSERT INTO `user_question_mastery` VALUES (69, 49, 122, '2026-04-21 11:24:34', '2026-04-21 11:24:34', '2026-04-21 11:24:34');
INSERT INTO `user_question_mastery` VALUES (70, 49, 37, '2026-04-21 18:32:26', '2026-04-21 18:32:26', '2026-04-21 18:32:26');
INSERT INTO `user_question_mastery` VALUES (71, 49, 3, '2026-04-21 18:57:46', '2026-04-21 18:57:46', '2026-04-21 18:57:46');

-- ----------------------------
-- Table structure for users
-- ----------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户名/账号',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '密码 (BCrypt)',
  `recovery_phrase` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '找回短语 (BCrypt)',
  `nickname` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '昵称',
  `role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'USER' COMMENT '角色: ADMIN, USER, GUEST',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `username`(`username` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 50 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of users
-- ----------------------------
INSERT INTO `users` VALUES (1, 'admin', '$2a$10$pkyS3hs.5CNlIc1dyAyLBeEsPGdpSkYqxa//uROU3SjABBO4xvK8q', NULL, 'admin', 'ADMIN', '2026-03-24 11:20:40');
INSERT INTO `users` VALUES (39, 'zzq3247804029@gmail.com', '$2a$10$GwyJ9aKNrWolN7bQNCQWGuAp29itcApUtC4oozW4oZf1AQb/YQMDW', NULL, 'Serendipity', 'USER', '2026-04-18 13:25:15');
INSERT INTO `users` VALUES (40, 'seed_joy_offline', '$2a$10$ZXZEMADZTsMT5MH2a8rKIOKsIiJLG4bRNvBI8wbWkjPjcdpFVo3Tq', NULL, '快乐不在服务区', 'USER', '2026-04-01 11:11:10');
INSERT INTO `users` VALUES (41, 'seed_cloudy_today', '$2a$10$xfBy8MvVWzns2.WbgFysu.sXXPEYotfNISwKg1mQ8CI.MRt3pgdj6', NULL, '天气转阴不转晴', 'USER', '2026-04-02 11:11:10');
INSERT INTO `users` VALUES (42, 'seed_too_quiet', '$2a$10$4.CJve/jHGwI4fgDcy7q0ehYptGEazG.aDdfKs.b43k5t/LFYOD8q', NULL, '最近常常太安静', 'USER', '2026-04-03 11:11:10');
INSERT INTO `users` VALUES (43, 'seed_low_pressure', '$2a$10$IO8OUObDIZNlkcAa4hBueejHsQ7Uan.g4RiWAXwftLnQAkhe.ThEm', NULL, '低气压区', 'USER', '2026-04-04 11:11:11');
INSERT INTO `users` VALUES (44, 'seed_mood_overcast', '$2a$10$KMHY5331pfl7MzGfIY9Tleu86xGO9lCVndsayqnVF93j7pCgpk1RG', NULL, '情绪阴天', 'USER', '2026-04-05 11:11:11');
INSERT INTO `users` VALUES (45, 'seed_late_happy', '$2a$10$x4WcFY7Ynsx6tRrN9FYliuEq/hXK6YDsBX9ac2/FPkjts5hd6hZ3K', NULL, '晚点开心', 'USER', '2026-04-06 11:11:11');
INSERT INTO `users` VALUES (46, 'seed_happy_loading', '$2a$10$lKpQ7xuxcHLBi5V7Ibnf2u00Ge2qdlGYet5p85CP1KLqzWMJ06Hmu', NULL, '开心加载中', 'USER', '2026-04-07 11:11:11');
INSERT INTO `users` VALUES (47, 'seed_human_observer', '$2a$10$zyDnFWspo7qrNj9gaJdClOdx5bBuSofhNe07bGckL.5LWGmMAbovi', NULL, '人间观察员', 'USER', '2026-04-08 11:11:11');
INSERT INTO `users` VALUES (48, 'seed_silent_mode', '$2a$10$NVWqjizCRwD5F0mMGkZx4O98i2gBZTgy7r0r3os9sNVe8ClJ52hRG', NULL, '间歇性沉默', 'USER', '2026-04-09 11:11:11');
INSERT INTO `users` VALUES (49, '3247804029@qq.com', '$2a$10$5zw.A2jgAJMVA1VUeuHUd.bM38hATDSRkAZUegpCAZdVA6V7c3fhq', NULL, 'ZhiQi', 'USER', '2026-04-19 11:20:36');

-- ----------------------------
-- Table structure for wish_comments
-- ----------------------------
DROP TABLE IF EXISTS `wish_comments`;
CREATE TABLE `wish_comments`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `wish_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `nickname` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `content` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT 'logical delete',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_wish_comments_wish_id_created`(`wish_id` ASC, `created_at` ASC) USING BTREE,
  INDEX `idx_wish_comments_user_id`(`user_id` ASC) USING BTREE,
  CONSTRAINT `fk_wish_comments_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_wish_comments_wish` FOREIGN KEY (`wish_id`) REFERENCES `wishes` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 20 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Wish wall comments' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of wish_comments
-- ----------------------------
INSERT INTO `wish_comments` VALUES (11, 43, 41, 'seed_cloudy_today', '天气转阴不转晴', '愿你这次真的收到想要的回音。', '2026-04-11 14:11:13', 0);
INSERT INTO `wish_comments` VALUES (12, 44, 42, 'seed_too_quiet', '最近常常太安静', '先把今天过好，后面的好消息会慢慢靠近。', '2026-04-12 14:11:13', 0);
INSERT INTO `wish_comments` VALUES (13, 45, 43, 'seed_low_pressure', '低气压区', '有些愿望不是晚到，是在路上。', '2026-04-13 14:11:13', 0);
INSERT INTO `wish_comments` VALUES (14, 46, 44, 'seed_mood_overcast', '情绪阴天', '愿你这次真的收到想要的回音。', '2026-04-14 14:11:13', 0);
INSERT INTO `wish_comments` VALUES (15, 47, 45, 'seed_late_happy', '晚点开心', '先把今天过好，后面的好消息会慢慢靠近。', '2026-04-15 14:11:13', 0);
INSERT INTO `wish_comments` VALUES (16, 48, 46, 'seed_happy_loading', '开心加载中', '有些愿望不是晚到，是在路上。', '2026-04-16 14:11:13', 0);
INSERT INTO `wish_comments` VALUES (17, 49, 47, 'seed_human_observer', '人间观察员', '愿你这次真的收到想要的回音。', '2026-04-17 14:11:13', 0);
INSERT INTO `wish_comments` VALUES (18, 50, 48, 'seed_silent_mode', '间歇性沉默', '先把今天过好，后面的好消息会慢慢靠近。', '2026-04-18 14:11:13', 0);
INSERT INTO `wish_comments` VALUES (19, 51, 40, 'seed_joy_offline', '快乐不在服务区', '有些愿望不是晚到，是在路上。', '2026-04-18 14:11:13', 0);

-- ----------------------------
-- Table structure for wish_likes
-- ----------------------------
DROP TABLE IF EXISTS `wish_likes`;
CREATE TABLE `wish_likes`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `wish_id` bigint NOT NULL,
  `actor_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'user/guest/visitor',
  `actor_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'username or visitor id',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_wish_like_actor`(`wish_id` ASC, `actor_type` ASC, `actor_id` ASC) USING BTREE,
  INDEX `idx_wish_likes_wish_id`(`wish_id` ASC) USING BTREE,
  INDEX `idx_wish_likes_actor`(`actor_type` ASC, `actor_id` ASC) USING BTREE,
  CONSTRAINT `fk_wish_likes_wish` FOREIGN KEY (`wish_id`) REFERENCES `wishes` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 26 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'Wish wall likes' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of wish_likes
-- ----------------------------
INSERT INTO `wish_likes` VALUES (6, 1, 'user', '123456', '2026-03-26 00:25:36');
INSERT INTO `wish_likes` VALUES (12, 43, 'user', 'seed_too_quiet', '2026-04-11 11:11:13');
INSERT INTO `wish_likes` VALUES (13, 44, 'user', 'seed_low_pressure', '2026-04-12 11:11:13');
INSERT INTO `wish_likes` VALUES (14, 45, 'user', 'seed_mood_overcast', '2026-04-13 11:11:13');
INSERT INTO `wish_likes` VALUES (15, 46, 'user', 'seed_late_happy', '2026-04-14 11:11:13');
INSERT INTO `wish_likes` VALUES (16, 47, 'user', 'seed_happy_loading', '2026-04-15 11:11:13');
INSERT INTO `wish_likes` VALUES (17, 48, 'user', 'seed_human_observer', '2026-04-16 11:11:13');
INSERT INTO `wish_likes` VALUES (18, 49, 'user', 'seed_silent_mode', '2026-04-17 11:11:13');
INSERT INTO `wish_likes` VALUES (19, 50, 'user', 'seed_joy_offline', '2026-04-18 11:11:13');
INSERT INTO `wish_likes` VALUES (20, 51, 'user', 'seed_cloudy_today', '2026-04-18 11:11:13');
INSERT INTO `wish_likes` VALUES (21, 4, 'user', '3247804029@qq.com', '2026-04-21 18:08:07');
INSERT INTO `wish_likes` VALUES (25, 43, 'user', '3247804029@qq.com', '2026-04-21 18:52:32');

-- ----------------------------
-- Table structure for wishes
-- ----------------------------
DROP TABLE IF EXISTS `wishes`;
CREATE TABLE `wishes`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '愿望内容',
  `emotion` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '情绪分析结果 (happy, hopeful, confused, anxious)',
  `color` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'UI 面板颜色',
  `city` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '城市/坐标',
  `pos_x` int NULL DEFAULT NULL COMMENT 'X坐标百分比 0-100',
  `pos_y` int NULL DEFAULT NULL COMMENT 'Y坐标百分比 0-100',
  `float_speed` double NULL DEFAULT NULL COMMENT '漂浮速度调节因子',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '0: 待审核 1: 已上线 2: 违规',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除标记: 1是, 0否',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 52 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '星愿墙数据表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of wishes
-- ----------------------------
INSERT INTO `wishes` VALUES (1, '希望能拿到字节跳动的 Offer，加油冲呀！?', 'hopeful', '#D4E0D0', '河北', 15, 20, 1.2, 1, '2026-03-24 11:31:15', 0);
INSERT INTO `wishes` VALUES (2, 'Q3结束前彻底掌握 React Hooks！?', 'determined', '#DDBFD1', '上海', 55, 35, 1, 1, '2026-03-24 11:31:15', 0);
INSERT INTO `wishes` VALUES (3, '今年想为一个大型开源项目贡献代码。一起飞跃吧！✨', 'hopeful', '#C4D0DE', '北京', 25, 60, 0.9, 1, '2026-03-24 11:31:15', 0);
INSERT INTO `wishes` VALUES (4, '搞定系统设计面试，不再畏惧架构图！?️', 'confident', '#D7E0CA', '成都', 65, 70, 1.5, 1, '2026-03-24 11:31:15', 0);
INSERT INTO `wishes` VALUES (5, '正从 UI 转型全栈开发。一步一个脚印。?', 'determined', '#E0D2C3', '深圳', 75, 25, 0.8, 1, '2026-03-24 11:31:15', 0);
INSERT INTO `wishes` VALUES (6, '希望能拿到字节跳动的 Offer，加油冲呀！?', 'hopeful', '#D4E0D0', '河北', 15, 20, 1.2, 1, '2026-03-24 11:31:17', 0);
INSERT INTO `wishes` VALUES (7, 'Q3结束前彻底掌握 React Hooks！?', 'determined', '#DDBFD1', '上海', 55, 35, 1, 1, '2026-03-24 11:31:17', 0);
INSERT INTO `wishes` VALUES (8, '今年想为一个大型开源项目贡献代码。一起飞跃吧！✨', 'hopeful', '#C4D0DE', '北京', 25, 60, 0.9, 1, '2026-03-24 11:31:17', 0);
INSERT INTO `wishes` VALUES (9, '搞定系统设计面试，不再畏惧架构图！?️', 'confident', '#D7E0CA', '成都', 65, 70, 1.5, 1, '2026-03-24 11:31:17', 0);
INSERT INTO `wishes` VALUES (10, '正从 UI 转型全栈开发。一步一个脚印。?', 'determined', '#E0D2C3', '深圳', 75, 25, 0.8, 1, '2026-03-24 11:31:17', 0);
INSERT INTO `wishes` VALUES (11, '希望能拿到字节跳动的 Offer，加油冲呀！?', 'hopeful', '#D4E0D0', '河北', 15, 20, 1.2, 1, '2026-03-24 11:31:17', 0);
INSERT INTO `wishes` VALUES (12, 'Q3结束前彻底掌握 React Hooks！?', 'determined', '#DDBFD1', '上海', 55, 35, 1, 1, '2026-03-24 11:31:17', 0);
INSERT INTO `wishes` VALUES (13, '今年想为一个大型开源项目贡献代码。一起飞跃吧！✨', 'hopeful', '#C4D0DE', '北京', 25, 60, 0.9, 1, '2026-03-24 11:31:17', 0);
INSERT INTO `wishes` VALUES (14, '搞定系统设计面试，不再畏惧架构图！?️', 'confident', '#D7E0CA', '成都', 65, 70, 1.5, 1, '2026-03-24 11:31:17', 0);
INSERT INTO `wishes` VALUES (42, '加油', 'hopeful', '#DDBFD1', '来自星海', 26, 16, 1.2, 1, '2026-04-15 22:57:55', 0);
INSERT INTO `wishes` VALUES (43, '快乐不在服务区，但我想把生活重新连上信号。', 'hopeful', '#D7E8D4', '杭州', 14, 18, 1.05, 1, '2026-04-10 22:36:13', 0);
INSERT INTO `wishes` VALUES (44, '希望最近投出的岗位里，能收到一封认真一点的回复。', 'hopeful', '#C9D8E8', '上海', 53, 28, 0.96, 1, '2026-04-11 22:36:13', 0);
INSERT INTO `wishes` VALUES (45, '想把那套一直没说顺的项目经历，终于讲到自己也满意。', 'confused', '#D6D3E8', '南京', 27, 62, 1.12, 1, '2026-04-12 22:36:13', 0);
INSERT INTO `wishes` VALUES (46, '低气压的时候，也想稳稳把今天过完。', 'anxious', '#E6D7C8', '武汉', 68, 73, 1.28, 1, '2026-04-13 22:36:13', 0);
INSERT INTO `wishes` VALUES (47, '希望下一次复盘的时候，我会因为坚持而不是因为懊悔写字。', 'hopeful', '#E7E0CF', '成都', 76, 34, 0.92, 1, '2026-04-14 22:36:13', 0);
INSERT INTO `wishes` VALUES (48, '想把晚点开心真的等到，而不是半路先放弃。', 'hopeful', '#E5CFE0', '厦门', 44, 52, 1.08, 1, '2026-04-15 22:36:13', 0);
INSERT INTO `wishes` VALUES (49, '给自己攒一点看得见的进度，别再总觉得原地踏步。', 'determined', '#D7E4C6', '深圳', 82, 21, 0.88, 1, '2026-04-16 22:36:13', 0);
INSERT INTO `wishes` VALUES (50, '愿我还能保持观察世界的耐心，也保持一点温柔。', 'happy', '#F0E2C6', '苏州', 33, 41, 1, 1, '2026-04-17 22:36:13', 0);
INSERT INTO `wishes` VALUES (51, '沉默的时候，也能被理解，不用总靠热闹证明存在。', 'confused', '#CFD8E2', '北京', 59, 14, 1.14, 1, '2026-04-18 22:36:13', 0);

SET FOREIGN_KEY_CHECKS = 1;
