-- MySQL dump 10.13  Distrib 5.7.39, for Win64 (x86_64)
--
-- Host: localhost    Database: novaleap
-- ------------------------------------------------------
-- Server version	5.7.39-log

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `custom_question_banks`
--

DROP TABLE IF EXISTS `custom_question_banks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `custom_question_banks` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL COMMENT '上传用户 ID',
  `name` varchar(255) NOT NULL COMMENT '题库名',
  `original_file_name` varchar(255) NOT NULL COMMENT '原始文件名',
  `file_type` varchar(16) NOT NULL COMMENT 'txt/doc/docx',
  `raw_content` mediumtext NOT NULL COMMENT '解析后的原始文本',
  `category` varchar(64) NOT NULL DEFAULT 'java' COMMENT '默认分类',
  `difficulty` tinyint(4) NOT NULL DEFAULT '2' COMMENT '默认难度',
  `status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '0: 待审核 1: 已通过 2: 已驳回',
  `question_count` int(11) NOT NULL DEFAULT '0' COMMENT '解析题目数',
  `imported_question_count` int(11) NOT NULL DEFAULT '0' COMMENT '正式入库题目数',
  `reject_reason` varchar(240) DEFAULT NULL COMMENT '驳回原因',
  `audited_at` datetime DEFAULT NULL COMMENT '审核时间',
  `imported_at` datetime DEFAULT NULL COMMENT '正式入库时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_custom_question_banks_user_status` (`user_id`,`status`,`created_at`),
  KEY `idx_custom_question_banks_status_created` (`status`,`created_at`),
  CONSTRAINT `fk_custom_question_banks_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COMMENT='用户自定义题库审核表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `custom_question_banks`
--

LOCK TABLES `custom_question_banks` WRITE;
/*!40000 ALTER TABLE `custom_question_banks` DISABLE KEYS */;
INSERT INTO `custom_question_banks` VALUES (1,1,'题库导入模板','题库导入模板.txt','txt','题目：请解释数据库索引失效的常见原因哈哈哈。\n答案：函数计算、隐式类型转换、前导模糊匹配等会导致索引失效。\n\n题目：请说明缓存穿透和缓存击穿的区别啦啦啦。\n答案：缓存穿透是查询不存在数据，缓存击穿是热点 key 失效瞬间并发打到数据库。','java',2,1,2,2,NULL,'2026-03-26 13:17:01','2026-03-26 13:17:01','2026-03-26 13:16:41','2026-03-26 13:17:01'),(2,1,'Linux','题库导入模板.txt','txt','题目：请解释数据库索引失效的常见原因哈哈哈。\n答案：函数计算、隐式类型转换、前导模糊匹配等会导致索引失效。\n\n题目：请说明缓存穿透和缓存击穿的区别啦啦啦。\n答案：缓存穿透是查询不存在数据，缓存击穿是热点 key 失效瞬间并发打到数据库。','java',2,1,2,2,NULL,'2026-03-29 01:59:55','2026-03-29 01:59:55','2026-03-26 22:19:47','2026-03-29 01:59:55');
/*!40000 ALTER TABLE `custom_question_banks` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `game_score_logs`
--

DROP TABLE IF EXISTS `game_score_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `game_score_logs` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL COMMENT 'users.id',
  `username` varchar(64) NOT NULL COMMENT 'username snapshot',
  `round_id` varchar(64) DEFAULT NULL COMMENT 'one game round/session id',
  `score` int(11) NOT NULL COMMENT 'reported score at this time',
  `is_final` tinyint(4) NOT NULL DEFAULT '0' COMMENT '1 final score for round',
  `recorded_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'record time',
  PRIMARY KEY (`id`),
  KEY `idx_game_score_logs_user_time` (`user_id`,`recorded_at`),
  KEY `idx_game_score_logs_round_time` (`round_id`,`recorded_at`),
  KEY `idx_game_score_logs_score` (`score`),
  CONSTRAINT `fk_game_score_logs_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=110 DEFAULT CHARSET=utf8mb4 COMMENT='cuihua runner live score logs';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `game_score_logs`
--

LOCK TABLES `game_score_logs` WRITE;
/*!40000 ALTER TABLE `game_score_logs` DISABLE KEYS */;
INSERT INTO `game_score_logs` VALUES (1,25,'123456','cuihua-1774507904808-731792',61,0,'2026-03-26 14:51:46'),(2,25,'123456','cuihua-1774507904808-731792',127,0,'2026-03-26 14:51:47'),(3,25,'123456','cuihua-1774507904808-731792',208,0,'2026-03-26 14:51:48'),(4,25,'123456','cuihua-1774507904808-731792',296,0,'2026-03-26 14:51:50'),(5,25,'123456','cuihua-1774507904808-731792',402,0,'2026-03-26 14:51:51'),(6,25,'123456','cuihua-1774507904808-731792',508,0,'2026-03-26 14:51:52'),(7,25,'123456','cuihua-1774507904808-731792',634,0,'2026-03-26 14:51:53'),(8,25,'123456','cuihua-1774507904808-731792',770,0,'2026-03-26 14:51:54'),(9,25,'123456','cuihua-1774507904808-731792',800,1,'2026-03-26 14:51:55'),(10,25,'123456','cuihua-1774507915816-555694',60,0,'2026-03-26 14:51:57'),(11,25,'123456','cuihua-1774507915816-555694',126,0,'2026-03-26 14:51:58'),(12,25,'123456','cuihua-1774509144683-109803',60,0,'2026-03-26 15:12:26'),(13,25,'123456','cuihua-1774509144683-109803',125,0,'2026-03-26 15:12:27'),(14,25,'123456','cuihua-1774509144683-109803',203,0,'2026-03-26 15:12:28'),(15,25,'123456','cuihua-1774509144683-109803',288,0,'2026-03-26 15:12:29'),(16,25,'123456','cuihua-1774509144683-109803',417,1,'2026-03-26 15:12:30'),(17,25,'123456','cuihua-1774509150608-649159',61,0,'2026-03-26 15:12:32'),(18,25,'123456','cuihua-1774509150608-649159',124,0,'2026-03-26 15:12:33'),(19,25,'123456','cuihua-1774509150608-649159',203,0,'2026-03-26 15:12:34'),(20,25,'123456','cuihua-1774509150608-649159',288,0,'2026-03-26 15:12:35'),(21,25,'123456','cuihua-1774509150608-649159',381,0,'2026-03-26 15:12:36'),(22,25,'123456','cuihua-1774509150608-649159',491,0,'2026-03-26 15:12:37'),(23,25,'123456','cuihua-1774509150608-649159',560,1,'2026-03-26 15:12:38'),(24,25,'123456','cuihua-1774509159007-501256',60,0,'2026-03-26 15:12:40'),(25,25,'123456','cuihua-1774509159007-501256',124,0,'2026-03-26 15:12:41'),(26,25,'123456','cuihua-1774509159007-501256',202,0,'2026-03-26 15:12:42'),(27,25,'123456','cuihua-1774509159007-501256',288,0,'2026-03-26 15:12:43'),(28,25,'123456','cuihua-1774509159007-501256',390,0,'2026-03-26 15:12:45'),(29,25,'123456','cuihua-1774509159007-501256',416,1,'2026-03-26 15:12:45'),(30,25,'123456','cuihua-1774509165386-305680',60,0,'2026-03-26 15:12:47'),(31,25,'123456','cuihua-1774509165386-305680',124,0,'2026-03-26 15:12:48'),(32,25,'123456','cuihua-1774509165386-305680',194,0,'2026-03-26 15:12:49'),(33,25,'123456','cuihua-1774509165386-305680',288,0,'2026-03-26 15:12:50'),(34,25,'123456','cuihua-1774509165386-305680',391,0,'2026-03-26 15:12:51'),(35,25,'123456','cuihua-1774509165386-305680',432,1,'2026-03-26 15:12:51'),(36,25,'123456','cuihua-1774511041946-210461',59,0,'2026-03-26 15:44:03'),(37,25,'123456','cuihua-1774511041946-210461',124,0,'2026-03-26 15:44:04'),(38,25,'123456','cuihua-1774511041946-210461',202,0,'2026-03-26 15:44:05'),(39,25,'123456','cuihua-1774511041946-210461',287,0,'2026-03-26 15:44:06'),(40,25,'123456','cuihua-1774511041946-210461',414,0,'2026-03-26 15:44:07'),(41,25,'123456','cuihua-1774511041946-210461',516,0,'2026-03-26 15:44:09'),(42,25,'123456','cuihua-1774511041946-210461',659,0,'2026-03-26 15:44:10'),(43,25,'123456','cuihua-1774511041946-210461',814,0,'2026-03-26 15:44:11'),(44,25,'123456','cuihua-1774511041946-210461',944,0,'2026-03-26 15:44:12'),(45,25,'123456','cuihua-1774511041946-210461',1098,0,'2026-03-26 15:44:13'),(46,25,'123456','cuihua-1774511041946-210461',1275,0,'2026-03-26 15:44:14'),(47,25,'123456','cuihua-1774511041946-210461',1452,0,'2026-03-26 15:44:15'),(48,25,'123456','cuihua-1774511041946-210461',1640,0,'2026-03-26 15:44:16'),(49,25,'123456','cuihua-1774511041946-210461',1844,0,'2026-03-26 15:44:17'),(50,25,'123456','cuihua-1774511041946-210461',2032,1,'2026-03-26 15:44:18'),(51,25,'123456','cuihua-1774511058342-738151',60,0,'2026-03-26 15:44:19'),(52,25,'123456','cuihua-1774511058342-738151',124,0,'2026-03-26 15:44:21'),(53,25,'123456','cuihua-1774511058342-738151',226,0,'2026-03-26 15:44:22'),(54,25,'123456','cuihua-1774511058342-738151',338,0,'2026-03-26 15:44:23'),(55,25,'123456','cuihua-1774511058342-738151',432,0,'2026-03-26 15:44:24'),(56,25,'123456','cuihua-1774511058342-738151',557,0,'2026-03-26 15:44:25'),(57,25,'123456','cuihua-1774511058342-738151',666,1,'2026-03-26 15:44:26'),(58,25,'123456','cuihua-1774511067423-666149',60,0,'2026-03-26 15:44:29'),(59,25,'123456','cuihua-1774511067423-666149',124,0,'2026-03-26 15:44:30'),(60,25,'123456','cuihua-1774511067423-666149',203,0,'2026-03-26 15:44:31'),(61,25,'123456','cuihua-1774511067423-666149',288,0,'2026-03-26 15:44:32'),(62,25,'123456','cuihua-1774511067423-666149',390,0,'2026-03-26 15:44:33'),(63,25,'123456','cuihua-1774511067423-666149',502,0,'2026-03-26 15:44:34'),(64,25,'123456','cuihua-1774511067423-666149',513,1,'2026-03-26 15:44:34'),(65,25,'123456','cuihua-1774511083414-283983',60,0,'2026-03-26 15:44:45'),(66,25,'123456','cuihua-1774511083414-283983',124,0,'2026-03-26 15:44:46'),(67,25,'123456','cuihua-1774511083414-283983',202,0,'2026-03-26 15:44:47'),(68,25,'123456','cuihua-1774511083414-283983',288,0,'2026-03-26 15:44:48'),(69,25,'123456','cuihua-1774511083414-283983',380,0,'2026-03-26 15:44:49'),(70,25,'123456','cuihua-1774511083414-283983',502,0,'2026-03-26 15:44:50'),(71,25,'123456','cuihua-1774511083414-283983',610,1,'2026-03-26 15:44:51'),(72,25,'123456','cuihua-1774511092415-491509',60,0,'2026-03-26 15:44:54'),(73,25,'123456','cuihua-1774511092415-491509',124,0,'2026-03-26 15:44:55'),(74,25,'123456','cuihua-1774511092415-491509',203,0,'2026-03-26 15:44:56'),(75,25,'123456','cuihua-1774511092415-491509',288,0,'2026-03-26 15:44:57'),(76,25,'123456','cuihua-1774511092415-491509',391,0,'2026-03-26 15:44:58'),(77,25,'123456','cuihua-1774511092415-491509',492,0,'2026-03-26 15:44:59'),(78,25,'123456','cuihua-1774511092415-491509',622,0,'2026-03-26 15:45:00'),(79,25,'123456','cuihua-1774511092415-491509',752,0,'2026-03-26 15:45:01'),(80,25,'123456','cuihua-1774511092415-491509',765,1,'2026-03-26 15:45:01'),(81,25,'123456','cuihua-1774511102326-286130',60,0,'2026-03-26 15:45:03'),(82,25,'123456','cuihua-1774511102326-286130',124,0,'2026-03-26 15:45:05'),(83,25,'123456','cuihua-1774511102326-286130',202,0,'2026-03-26 15:45:06'),(84,25,'123456','cuihua-1774511102326-286130',288,0,'2026-03-26 15:45:07'),(85,25,'123456','cuihua-1774511102326-286130',391,0,'2026-03-26 15:45:08'),(86,25,'123456','cuihua-1774511102326-286130',491,0,'2026-03-26 15:45:09'),(87,25,'123456','cuihua-1774511102326-286130',610,0,'2026-03-26 15:45:10'),(88,25,'123456','cuihua-1774511102326-286130',740,0,'2026-03-26 15:45:11'),(89,25,'123456','cuihua-1774511102326-286130',750,1,'2026-03-26 15:45:11'),(90,25,'123456','cuihua-1774511937808-120415',60,0,'2026-03-26 15:58:59'),(91,25,'123456','cuihua-1774511937808-120415',124,0,'2026-03-26 15:59:00'),(92,25,'123456','cuihua-1774511937808-120415',202,0,'2026-03-26 15:59:01'),(93,25,'123456','cuihua-1774511937808-120415',288,0,'2026-03-26 15:59:02'),(94,25,'123456','cuihua-1774511937808-120415',391,0,'2026-03-26 15:59:03'),(95,25,'123456','cuihua-1774511937808-120415',491,0,'2026-03-26 15:59:04'),(96,25,'123456','cuihua-1774511937808-120415',610,0,'2026-03-26 15:59:06'),(97,25,'123456','cuihua-1774511937808-120415',751,0,'2026-03-26 15:59:07'),(98,25,'123456','cuihua-1774511937808-120415',877,0,'2026-03-26 15:59:08'),(99,25,'123456','cuihua-1774511937808-120415',1039,0,'2026-03-26 15:59:09'),(100,25,'123456','cuihua-1774511937808-120415',1159,1,'2026-03-26 15:59:10'),(101,25,'123456','cuihua-1774511950750-707825',60,0,'2026-03-26 15:59:12'),(102,25,'123456','cuihua-1774511950750-707825',124,0,'2026-03-26 15:59:13'),(103,25,'123456','cuihua-1774511950750-707825',203,0,'2026-03-26 15:59:14'),(104,25,'123456','cuihua-1774511950750-707825',288,0,'2026-03-26 15:59:15'),(105,25,'123456','cuihua-1774511950750-707825',390,0,'2026-03-26 15:59:16'),(106,25,'123456','cuihua-1774511950750-707825',491,0,'2026-03-26 15:59:17'),(107,25,'123456','cuihua-1774511950750-707825',600,0,'2026-03-26 15:59:18'),(108,25,'123456','cuihua-1774511950750-707825',727,0,'2026-03-26 15:59:20'),(109,25,'123456','cuihua-1774511950750-707825',805,1,'2026-03-26 15:59:20');
/*!40000 ALTER TABLE `game_score_logs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `note_comments`
--

DROP TABLE IF EXISTS `note_comments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `note_comments` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `note_id` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  `username` varchar(64) NOT NULL,
  `nickname` varchar(64) NOT NULL,
  `content` varchar(300) NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted` tinyint(4) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_note_comments_note_id_created` (`note_id`,`created_at`),
  KEY `idx_note_comments_user_id` (`user_id`),
  CONSTRAINT `fk_note_comments_note` FOREIGN KEY (`note_id`) REFERENCES `notes` (`id`),
  CONSTRAINT `fk_note_comments_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COMMENT='Notes comments';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `note_comments`
--

LOCK TABLES `note_comments` WRITE;
/*!40000 ALTER TABLE `note_comments` DISABLE KEYS */;
INSERT INTO `note_comments` VALUES (1,28,1,'admin','admin','??????','2026-03-25 23:30:47',0),(2,27,1,'admin','admin','?','2026-03-25 23:35:13',0);
/*!40000 ALTER TABLE `note_comments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `note_likes`
--

DROP TABLE IF EXISTS `note_likes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `note_likes` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `note_id` bigint(20) NOT NULL,
  `actor_type` varchar(16) NOT NULL COMMENT 'user / guest',
  `actor_id` varchar(128) NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_note_like_actor` (`note_id`,`actor_type`,`actor_id`),
  KEY `idx_note_likes_note_id` (`note_id`),
  KEY `idx_note_likes_actor` (`actor_type`,`actor_id`),
  CONSTRAINT `fk_note_likes_note` FOREIGN KEY (`note_id`) REFERENCES `notes` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COMMENT='Notes likes';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `note_likes`
--

LOCK TABLES `note_likes` WRITE;
/*!40000 ALTER TABLE `note_likes` DISABLE KEYS */;
INSERT INTO `note_likes` VALUES (1,28,'user','admin','2026-03-25 23:30:47');
/*!40000 ALTER TABLE `note_likes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `notes`
--

DROP TABLE IF EXISTS `notes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `notes` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `title` varchar(500) NOT NULL COMMENT '手记标题',
  `content` mediumtext NOT NULL COMMENT '手记正文',
  `summary` varchar(500) DEFAULT NULL COMMENT '手记摘要',
  `category` varchar(64) NOT NULL DEFAULT '技术手记' COMMENT '手记分类',
  `emoji` varchar(16) DEFAULT '?' COMMENT '封面表情',
  `author` varchar(64) NOT NULL DEFAULT 'Nova 学员' COMMENT '作者名',
  `user_id` bigint(20) DEFAULT NULL COMMENT '作者用户ID，关联 users.id',
  `view_count` int(11) NOT NULL DEFAULT '0' COMMENT '浏览次数',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '0: 隐藏 1: 上线',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT '逻辑删除标记: 1是, 0否',
  `reject_reason` varchar(240) DEFAULT NULL COMMENT '瀹℃牳澶辫触鍘熷洜',
  `audit_source` varchar(32) DEFAULT NULL COMMENT '瀹℃牳鏉ユ簮锛欰I/MANUAL/RULES',
  `audited_at` datetime DEFAULT NULL COMMENT '瀹℃牳鏃堕棿',
  PRIMARY KEY (`id`),
  KEY `idx_notes_status_created` (`status`,`created_at`),
  KEY `idx_notes_category` (`category`),
  KEY `idx_notes_user_id` (`user_id`),
  CONSTRAINT `fk_notes_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=57 DEFAULT CHARSET=utf8mb4 COMMENT='灵感手记表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `notes`
--

LOCK TABLES `notes` WRITE;
/*!40000 ALTER TABLE `notes` DISABLE KEYS */;
INSERT INTO `notes` VALUES (1,'深入浅出 Vue 3 渲染器设计与实现','## 为什么需要自定义渲染器？\nVue 3 的渲染核心与宿主环境解耦，使得同一套响应式机制可以在 DOM、Canvas 等场景复用。\n\n### 核心思路\n通过 `createRenderer` 注入宿主操作：createElement / insert / setElementText 等，实现跨平台渲染。','Vue 3 渲染器的设计目标与 createRenderer 关键机制拆解。','前端工程化','🟢','志琪的笔记',NULL,1302,1,'2026-03-25 03:33:52','2026-03-31 01:09:39',0,NULL,NULL,NULL),(2,'Kafka 分区与偏移量设计精解','Kafka 通过 Partition + Offset 管理消息顺序与消费进度。消费者组 Rebalance 会在实例变更时重新分配分区，需关注幂等消费与位点提交策略。','分区、位点、Rebalance 的实战要点与常见坑位。','分布式系统','📝','志琪的笔记',NULL,148,1,'2026-03-25 03:33:52','2026-04-01 01:36:07',0,NULL,NULL,NULL),(3,'????????','????????????????','????','??','??','超级管理员',1,1,1,'2026-03-25 03:37:41','2026-03-25 03:37:41',1,NULL,NULL,NULL),(4,'smoke_note_034503','smoke note body','smoke summary','smoke','馃И','smoke_user',NULL,1,1,'2026-03-25 03:45:04','2026-03-25 03:45:03',1,NULL,NULL,NULL),(5,'smoke_note_034549','smoke note body','smoke summary','smoke','馃И','smoke_user',NULL,1,1,'2026-03-25 03:45:49','2026-03-25 03:45:49',1,NULL,NULL,NULL),(6,'admin_smoke_note_034558','admin smoke note','admin smoke summary','smoke','馃И','admin',NULL,0,1,'2026-03-25 03:45:59','2026-03-25 03:45:58',1,NULL,NULL,NULL),(7,'smoke_note_034821','smoke note body','smoke summary','smoke','馃И','smoke_user',NULL,1,1,'2026-03-25 03:48:22','2026-03-25 03:48:22',1,NULL,NULL,NULL),(8,'admin_smoke_note_034848','admin smoke note','admin smoke summary','smoke','馃И','admin',NULL,0,1,'2026-03-25 03:48:48','2026-03-25 03:48:48',1,NULL,NULL,NULL),(9,'smoke_note_034933','smoke note body','smoke summary','smoke','馃И','smoke_updated',NULL,1,1,'2026-03-25 03:49:34','2026-03-25 03:49:33',1,NULL,NULL,NULL),(10,'admin_smoke_note_034956','admin smoke note','admin smoke summary','smoke','馃И','admin',NULL,0,1,'2026-03-25 03:49:57','2026-03-25 03:49:56',1,NULL,NULL,NULL),(11,'我是谁','张志琪','','技术手记','📝','Nova 学员',NULL,67,1,'2026-03-25 13:02:53','2026-04-01 01:36:07',0,NULL,NULL,NULL),(12,'smoke_note_135948','smoke note body','smoke summary','smoke','馃И','smoke_updated',NULL,1,1,'2026-03-25 13:59:48','2026-03-25 13:59:48',1,NULL,NULL,NULL),(13,'admin_smoke_note_140016','admin smoke note','admin smoke summary','smoke','馃И','admin',NULL,0,1,'2026-03-25 14:00:17','2026-03-25 14:00:16',1,NULL,NULL,NULL),(14,'smoke_note_142255','smoke note body','smoke summary','smoke','馃И','smoke_updated',NULL,1,1,'2026-03-25 14:22:55','2026-03-25 14:22:55',1,NULL,NULL,NULL),(15,'admin_smoke_note_142316','admin smoke note','admin smoke summary','smoke','馃И','admin',NULL,0,1,'2026-03-25 14:23:17','2026-03-25 14:23:16',1,NULL,NULL,NULL),(16,'smoke_note_143103','smoke note body','smoke summary','smoke','馃И','smoke_updated',NULL,1,1,'2026-03-25 14:31:03','2026-03-25 14:31:03',1,NULL,NULL,NULL),(17,'admin_smoke_note_143118','admin smoke note','admin smoke summary','smoke','馃И','admin',NULL,0,1,'2026-03-25 14:31:18','2026-03-25 14:31:18',1,NULL,NULL,NULL),(18,'smoke_note_151259','smoke note body','smoke note body','smoke','馃И','smoke_updated',NULL,44,1,'2026-03-25 15:13:00','2026-04-01 01:36:06',0,NULL,NULL,NULL),(19,'smoke_note_151449','smoke note body','smoke summary','smoke','馃И','smoke_updated',NULL,1,0,'2026-03-25 15:14:50','2026-03-25 15:14:50',1,NULL,NULL,NULL),(20,'admin_smoke_note_151509','admin smoke note','admin smoke summary','smoke','馃И','admin',NULL,0,1,'2026-03-25 15:15:09','2026-03-25 15:15:09',1,NULL,NULL,NULL),(21,'smoke_note_152352','smoke note body','smoke summary','smoke','馃И','smoke_updated',NULL,1,0,'2026-03-25 15:23:53','2026-03-25 15:23:52',1,NULL,NULL,NULL),(22,'admin_smoke_note_152404','admin smoke note','admin smoke summary','smoke','馃И','admin',NULL,0,1,'2026-03-25 15:24:04','2026-03-25 15:24:04',1,NULL,NULL,NULL),(23,'啊啥的发生','撒发','撒发','技术手记','📘','QQ用户4029',2,0,2,'2026-03-25 18:05:17','2026-03-25 18:05:50',0,NULL,NULL,NULL),(24,'smoke_note_185314','smoke note body','smoke summary','smoke','馃И','smoke_updated',NULL,1,0,'2026-03-25 18:53:14','2026-03-25 18:53:14',1,NULL,NULL,NULL),(25,'admin_smoke_note_185326','admin smoke note','admin smoke summary','smoke','馃И','admin',NULL,0,1,'2026-03-25 18:53:27','2026-03-25 18:53:26',1,NULL,NULL,NULL),(26,'afds','adsf','adsf','技术手记','📘','admin',1,1,2,'2026-03-25 21:11:24','2026-03-26 17:14:39',1,NULL,NULL,NULL),(27,'asfas','asdfasdf','asdfasdf','技术手记','📘','admin',1,32,1,'2026-03-25 21:22:28','2026-03-26 17:14:32',1,NULL,NULL,NULL),(28,'去玩儿','娃儿','娃儿','技术手记','📘','admin',1,55,1,'2026-03-25 22:00:58','2026-03-26 17:14:15',1,NULL,NULL,NULL),(29,'ga v','asdf ','asdf','技术手记','📘','admin',1,0,2,'2026-03-25 23:37:35','2026-03-26 11:02:41',1,NULL,NULL,NULL),(30,'ASDF','ASDF','ASDF','技术手记','ASDF','admin',1,0,2,'2026-03-26 00:19:45','2026-03-26 01:53:19',1,NULL,NULL,NULL),(31,'1去','d','d','技术手记','💡','admin',1,0,2,'2026-03-26 01:17:06','2026-03-26 17:14:37',1,NULL,NULL,NULL),(32,'ce','ce','ce','技术手记','📕','admin',1,0,2,'2026-03-26 01:30:39','2026-03-26 02:12:12',1,'manual-reason-32','MANUAL','2026-03-26 02:06:13'),(33,'????-1774461784813','????','??','????','??','admin',1,0,2,'2026-03-26 02:03:05','2026-03-26 02:12:18',1,'管理端驳回原因-1774461784813','MANUAL','2026-03-26 02:03:05'),(34,'e2e-1774461803969','e2e','e2e','????','??','admin',1,0,2,'2026-03-26 02:03:24','2026-03-26 02:12:15',1,'TEST-REASON-1774461803969','MANUAL','2026-03-26 02:03:24'),(35,'11','11','11','技术手记','📘','admin',1,2,1,'2026-03-26 10:59:22','2026-03-26 17:14:18',1,NULL,'AI_FALLBACK','2026-03-26 10:59:22'),(36,'草','草尼玛','草尼玛','技术手记','📘','admin',1,3,1,'2026-03-26 10:59:43','2026-03-26 17:14:21',1,NULL,'AI_FALLBACK','2026-03-26 10:59:43'),(37,'你妈','你妈','你妈','技术手记','📘','admin',1,9,1,'2026-03-26 11:00:07','2026-03-26 17:14:12',1,NULL,'AI_FALLBACK','2026-03-26 11:00:07'),(38,'傻逼','傻逼','傻逼','技术手记','📘','admin',1,2,1,'2026-03-26 11:05:23','2026-03-26 13:55:53',1,NULL,'AI_FALLBACK','2026-03-26 11:05:23'),(39,'?????','??????????,???????','??????????,???????','tech','??','????',NULL,0,2,'2026-03-26 11:42:34','2026-03-26 12:03:54',0,'manual reject from admin','MANUAL','2026-03-26 12:03:54'),(40,'?????','????????????,??????','????????????,??????','tech','??','????',NULL,0,0,'2026-03-26 11:42:35','2026-03-26 11:42:35',0,NULL,'AI_FALLBACK','2026-03-26 11:42:35'),(41,'unicode-test','这里有你妈这个词','这里有你妈这个词','tech','📘','????',NULL,0,2,'2026-03-26 12:02:57','2026-03-26 12:02:57',0,'检测到违禁词：你妈','RULES','2026-03-26 12:02:57'),(42,'clean-no-key','normal text for moderation without banned words','normal text for moderation without banned words','tech','📘','????',NULL,0,0,'2026-03-26 12:06:10','2026-03-26 12:06:10',0,NULL,'RULES_ONLY','2026-03-26 12:06:10'),(43,'bad-no-key','这里有你妈这个敏感词','这里有你妈这个敏感词','tech','📘','????',NULL,0,2,'2026-03-26 12:06:11','2026-03-26 12:06:11',0,'检测到违禁词：你妈','RULES','2026-03-26 12:06:11'),(44,'manual-reject-check','plain text','plain text','tech','📘','????',NULL,0,2,'2026-03-26 12:06:38','2026-03-26 12:06:38',0,'manual reason from admin side','MANUAL','2026-03-26 12:06:38'),(45,'clean-final-check','normal text for moderation without banned words','normal text for moderation without banned words','tech','📘','????',NULL,0,2,'2026-03-26 12:13:19','2026-03-26 12:13:30',0,'admin reject reason final','MANUAL','2026-03-26 12:13:30'),(46,'bad-final-check','这里有你妈这个敏感词','这里有你妈这个敏感词','tech','📘','????',NULL,0,2,'2026-03-26 12:13:19','2026-03-26 12:13:19',0,'检测到违禁词：你妈','RULES','2026-03-26 12:13:19'),(47,'傻逼','傻逼','傻逼','技术手记','📘','admin',1,0,2,'2026-03-26 13:19:36','2026-03-26 13:55:50',1,'检测到违禁词：傻逼','RULES','2026-03-26 13:19:36'),(48,'尼玛','尼玛','尼玛','技术手记','📘','admin',1,0,0,'2026-03-26 13:19:52','2026-03-26 13:55:47',1,NULL,'AI_FALLBACK','2026-03-26 13:19:52'),(49,'你妈','尼玛','尼玛','技术手记','📘','admin',1,0,2,'2026-03-26 13:20:56','2026-03-26 17:14:30',1,'检测到违禁词：你妈','RULES','2026-03-26 13:20:56'),(50,'11','11','11','技术手记','📘','admin',1,4,1,'2026-03-26 13:56:03','2026-03-26 17:14:23',1,NULL,'AI','2026-03-26 13:56:03'),(51,'！！！！！！','！！！！！！！！！','！！！！！！！！！','技术手记','📘','admin',1,0,2,'2026-03-26 13:56:24','2026-03-26 13:56:57',1,'标题正文无意义，疑似刷屏','AI','2026-03-26 13:56:24'),(52,'张志琪sb','sb','sb','技术手记','📘','admin',1,0,2,'2026-03-26 13:56:44','2026-03-26 13:56:52',1,'仇恨言论','AI','2026-03-26 13:56:44'),(53,'zhehsi','shouji ','shouji','技术手记','📘','admin',1,0,2,'2026-03-26 13:57:28','2026-03-26 17:14:25',1,'无意义','MANUAL','2026-03-26 13:58:20'),(54,'adf','adfs','adfs','技术手记','📘','admin',1,14,1,'2026-03-26 17:15:29','2026-04-01 01:36:08',0,NULL,'AI','2026-03-26 17:15:29'),(55,'hhhhhhhh','asdfafasdfsadfasdsd','asdfafasdfsadfasdsd','技术手记','📘','111',NULL,0,2,'2026-03-30 00:45:14','2026-03-30 00:45:14',0,'内容无意义，不符合发布标准','AI','2026-03-30 00:45:14'),(56,'我知道我知','真的知道','','技术手记','📘','111',NULL,0,2,'2026-03-30 00:45:40','2026-03-30 00:45:57',0,'傻逼','MANUAL','2026-03-30 00:45:57');
/*!40000 ALTER TABLE `notes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `question_categories`
--

DROP TABLE IF EXISTS `question_categories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `question_categories` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '鍒嗙被缂栫爜',
  `name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '鍒嗙被鍚嶇О',
  `sort_order` int(11) NOT NULL DEFAULT '0' COMMENT '鎺掑簭',
  `enabled` tinyint(4) NOT NULL DEFAULT '1' COMMENT '鏄惁鍚敤: 1鍚敤 0绂佺敤',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=86 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `question_categories`
--

LOCK TABLES `question_categories` WRITE;
/*!40000 ALTER TABLE `question_categories` DISABLE KEYS */;
INSERT INTO `question_categories` VALUES (1,'java','Java 核心',1,1,'2026-03-26 20:59:35','2026-03-26 23:47:06'),(2,'spring','Spring 生态',2,1,'2026-03-26 20:59:35','2026-03-26 23:47:06'),(3,'db','数据存储',3,1,'2026-03-26 20:59:35','2026-03-26 23:47:06'),(4,'redis','Redis',4,1,'2026-03-26 20:59:35','2026-03-26 20:59:35'),(5,'algo','算法',5,1,'2026-03-26 20:59:35','2026-03-26 22:44:59'),(14,'arch','架构设计',9,1,'2026-03-26 21:04:19','2026-03-26 23:47:06'),(15,'linux','Linux',8,1,'2026-03-26 21:20:36','2026-03-26 23:47:06'),(40,'network','计算机网络',6,1,'2026-03-26 23:47:06','2026-03-26 23:47:06'),(41,'system-design','系统设计',7,1,'2026-03-26 23:47:06','2026-03-26 23:47:06'),(58,'语文','语文',10,1,'2026-03-26 23:50:06','2026-03-26 23:50:06');
/*!40000 ALTER TABLE `question_categories` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `questions`
--

DROP TABLE IF EXISTS `questions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `questions` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `title` varchar(500) NOT NULL COMMENT '题目标题',
  `content` text COMMENT '题目详细描述',
  `standard_answer` mediumtext COMMENT '数据库标准答案（供AI回答）',
  `difficulty` tinyint(4) NOT NULL DEFAULT '2' COMMENT '难度: 1简单 2中等 3困难',
  `category` varchar(64) NOT NULL DEFAULT 'java' COMMENT '分类: java, spring, db, redis, algo, network',
  `tags` varchar(255) DEFAULT NULL COMMENT '标签 (逗号分隔)',
  `view_count` int(11) NOT NULL DEFAULT '0' COMMENT '浏览次数',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '0: 下架 1: 上架',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `source_type` varchar(16) NOT NULL DEFAULT 'OFFICIAL' COMMENT '来源类型：OFFICIAL/CUSTOM',
  `custom_bank_id` bigint(20) DEFAULT NULL COMMENT '自定义题库 ID',
  `owner_user_id` bigint(20) DEFAULT NULL COMMENT '题目所属用户 ID',
  PRIMARY KEY (`id`),
  KEY `idx_questions_source_created` (`source_type`,`created_at`),
  KEY `idx_questions_custom_bank` (`custom_bank_id`),
  KEY `idx_questions_owner_source` (`owner_user_id`,`source_type`)
) ENGINE=InnoDB AUTO_INCREMENT=91 DEFAULT CHARSET=utf8mb4 COMMENT='题库表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `questions`
--

LOCK TABLES `questions` WRITE;
/*!40000 ALTER TABLE `questions` DISABLE KEYS */;
INSERT INTO `questions` VALUES (1,'Redis 缓存雪崩、穿透、击穿如何解决？','请详细描述这三种问题的不同场景，并给出对应的解决方案和预防措施。特别强调在分布式锁和布隆过滤器中的应用细节。','',2,'redis','Redis,缓存机制,高可用',342,1,'2026-03-24 11:20:40','OFFICIAL',NULL,NULL),(2,'Spring Bean 的生命周期全解析','面试高频题：请从 Bean 的实例化、属性填充、初始化前/后处理、销毁等阶段完整描述，并说明 BeanPostProcessor 的作用。','【数据库标准答案】\n面试高频题：请从 Bean 的实例化、属性填充、初始化前/后处理、销毁等阶段完整描述，并说明 BeanPostProcessor 的作用。',3,'spring','Spring Core,源码剖析',512,1,'2026-03-24 11:20:40','OFFICIAL',NULL,NULL),(3,'TCP 三次握手与四次挥手','解释为什么握手是三次而挥手需要四次？如果服务端收到 SYN 后立即进入 ESTABLISHED 状态会有什么风险？','【数据库标准答案】\n解释为什么握手是三次而挥手需要四次？如果服务端收到 SYN 后立即进入 ESTABLISHED 状态会有什么风险？',2,'network','计算机网络,TCP/IP',190,1,'2026-03-24 11:20:40','OFFICIAL',NULL,NULL),(4,'MySQL 索引失效场景总结','请列举 5 种以上会导致 MySQL 索引失效的场景，并说明底层的 B+ 树为什么会失效。','【数据库标准答案】\n请列举 5 种以上会导致 MySQL 索引失效的场景，并说明底层的 B+ 树为什么会失效。',2,'db','MySQL,SQL优化',230,1,'2026-03-24 11:20:40','OFFICIAL',NULL,NULL),(5,'如何实现一个线程安全的单例模式？','请手写 DCL（双重检查锁）单例模式，并解释 volatile 关键字在此处的具体作用（指令重排）。','【数据库标准答案】\n请手写 DCL（双重检查锁）单例模式，并解释 volatile 关键字在此处的具体作用（指令重排）。',1,'java','设计模式,多线程',444,1,'2026-03-24 11:20:40','OFFICIAL',NULL,NULL),(6,'Redis 缓存雪崩、穿透、击穿如何解决？','请详细描述这三种问题的不同场景，并给出对应的解决方案和预防措施。特别强调在分布式锁和布隆过滤器中的应用细节。','【数据库标准答案】\n请详细描述这三种问题的不同场景，并给出对应的解决方案和预防措施。特别强调在分布式锁和布隆过滤器中的应用细节。',2,'redis','Redis,缓存机制,高可用',344,1,'2026-03-24 11:20:43','OFFICIAL',NULL,NULL),(7,'Spring Bean 的生命周期全解析','面试高频题：请从 Bean 的实例化、属性填充、初始化前/后处理、销毁等阶段完整描述，并说明 BeanPostProcessor 的作用。','【数据库标准答案】\n面试高频题：请从 Bean 的实例化、属性填充、初始化前/后处理、销毁等阶段完整描述，并说明 BeanPostProcessor 的作用。',3,'spring','Spring Core,源码剖析',515,1,'2026-03-24 11:20:43','OFFICIAL',NULL,NULL),(8,'TCP 三次握手与四次挥手','解释为什么握手是三次而挥手需要四次？如果服务端收到 SYN 后立即进入 ESTABLISHED 状态会有什么风险？','【数据库标准答案】\n解释为什么握手是三次而挥手需要四次？如果服务端收到 SYN 后立即进入 ESTABLISHED 状态会有什么风险？',2,'network','计算机网络,TCP/IP',197,1,'2026-03-24 11:20:43','OFFICIAL',NULL,NULL),(9,'MySQL 索引失效场景总结','请列举 5 种以上会导致 MySQL 索引失效的场景，并说明底层的 B+ 树为什么会失效。','【数据库标准答案】\n请列举 5 种以上会导致 MySQL 索引失效的场景，并说明底层的 B+ 树为什么会失效。',2,'db','MySQL,SQL优化',228,1,'2026-03-24 11:20:43','OFFICIAL',NULL,NULL),(10,'如何实现一个线程安全的单例模式？','请手写 DCL（双重检查锁）单例模式，并解释 volatile 关键字在此处的具体作用（指令重排）。','【数据库标准答案】\n请手写 DCL（双重检查锁）单例模式，并解释 volatile 关键字在此处的具体作用（指令重排）。',1,'java','设计模式,多线程',453,1,'2026-03-24 11:20:43','OFFICIAL',NULL,NULL),(11,'Redis 缓存雪崩、穿透、击穿如何解决？','请详细描述这三种问题的不同场景，并给出对应的解决方案和预防措施。特别强调在分布式锁和布隆过滤器中的应用细节。','【数据库标准答案】\n请详细描述这三种问题的不同场景，并给出对应的解决方案和预防措施。特别强调在分布式锁和布隆过滤器中的应用细节。',2,'redis','Redis,缓存机制,高可用',359,1,'2026-03-24 11:20:57','OFFICIAL',NULL,NULL),(12,'Spring Bean 的生命周期全解析','面试高频题：请从 Bean 的实例化、属性填充、初始化前/后处理、销毁等阶段完整描述，并说明 BeanPostProcessor 的作用。','【数据库标准答案】\n面试高频题：请从 Bean 的实例化、属性填充、初始化前/后处理、销毁等阶段完整描述，并说明 BeanPostProcessor 的作用。',3,'spring','Spring Core,源码剖析',522,1,'2026-03-24 11:20:57','OFFICIAL',NULL,NULL),(13,'TCP 三次握手与四次挥手','解释为什么握手是三次而挥手需要四次？如果服务端收到 SYN 后立即进入 ESTABLISHED 状态会有什么风险？','【数据库标准答案】\n解释为什么握手是三次而挥手需要四次？如果服务端收到 SYN 后立即进入 ESTABLISHED 状态会有什么风险？',2,'network','计算机网络,TCP/IP',200,1,'2026-03-24 11:20:57','OFFICIAL',NULL,NULL),(14,'MySQL 索引失效场景总结','请列举 5 种以上会导致 MySQL 索引失效的场景，并说明底层的 B+ 树为什么会失效。','',2,'db','MySQL,SQL优化',268,1,'2026-03-24 11:20:57','OFFICIAL',NULL,NULL),(76,'系统设计：如何设计一个高可用秒杀系统？','请从流量削峰、库存一致性、幂等处理、降级熔断和监控报警几个角度回答。','结论\r\nMySQL 索引失效是指查询优化器未使用预期索引，转而采用全表扫描或低效访问路径，导致查询性能显著下降。常见的索引失效场景包括：对索引列使用函数或表达式、隐式类型转换、左模糊匹配（LIKE \'%xxx\'）、复合索引不遵循最左前缀原则、OR 条件中非索引列参与判断、IS NULL / IS NOT NULL 在部分引擎下的特殊行为、以及索引列参与运算等。这些场景本质上破坏了 B+ 树索引的有序性和可预测性，使优化器无法高效定位数据。\r\n\r\n核心原理\r\nB+ 树索引的核心优势在于其有序性和范围查询能力。索引按照键值顺序组织，支持快速定位、范围扫描和排序。当查询条件无法利用这种有序性时，B+ 树就“失效”了。例如：\r\n\r\n对索引列使用函数（如 WHERE YEAR(create_time) = 2023）会破坏原始键值的连续性，B+ 树无法直接定位到对应年份的起始位置，必须逐行计算函数结果再比对，等同于全表扫描。\r\n隐式类型转换（如字符串列与数字比较 WHERE phone = 13800138000）会导致 MySQL 将列值转换为数字，同样破坏索引键的原始顺序，无法走索引。\r\n左模糊 LIKE \'%abc\' 无法确定起始匹配点，B+ 树无法从根节点向下精准定位，只能全表扫描。\r\n复合索引 (a, b, c) 若跳过 a 直接使用 b 或 c，违反了最左前缀原则，B+ 树在该子树上无序，无法有效剪枝。\r\nOR 条件中若包含非索引列（如 WHERE a = 1 OR d = 2，d 无索引），优化器可能认为走索引不如全表扫描划算。\r\n这些操作都使得 B+ 树无法发挥“有序快速定位”的特性，被迫退化为低效访问方式。\r\n\r\n高频追问\r\n\r\n为什么 LIKE \'abc%\' 可以走索引而 LIKE \'%abc\' 不行？\r\n→ 前者是右模糊，前缀固定，B+ 树可按前缀定位起始点；后者无固定前缀，无法利用有序性。\r\n复合索引 (a, b) 下，WHERE b = 5 AND a = 1 是否走索引？\r\n→ 可以，MySQL 会重排条件顺序，仍满足最左前缀。\r\nIS NULL 是否一定不走索引？\r\n→ 不一定。InnoDB 中 NULL 值也存储在索引中（作为最小值），若索引包含 NULL，IS NULL 可走索引；但优化器可能因选择性差而放弃使用。\r\n类型转换一定导致索引失效吗？\r\n→ 通常是。若常量可无损转为列类型（如字符串转数字），可能仍走索引；但若列需转换（如字符串列转数字），则必然失效。\r\n面试加分点\r\n\r\n强调“索引列不要参与运算或函数处理”是黄金准则，应把计算放到应用层或改写为等价形式（如 YEAR(create_time)=2023 改写为 create_time BETWEEN \'2023-01-01\' AND \'2023-12-31\'）。\r\n指出 MySQL 8.0 对函数索引（Functional Index）的支持，可通过创建函数索引解决部分场景（如 CREATE INDEX idx_year ON tbl ((YEAR(create_time)))）。\r\n提及执行计划分析：使用 EXPLAIN 查看 key、type、Extra 字段，若 type=ALL 或 key=NULL，则索引未使用。\r\n补充：统计信息过时也可能导致优化器误判，即使语法正确也不走索引，需定期 ANALYZE TABLE 更新统计信息。\r\n强调业务设计层面：避免在 WHERE 条件中对索引列进行不必要的操作，是保障索引生效的根本。',3,'db','系统设计,高并发,削峰填谷',208,1,'2026-03-25 14:20:07','OFFICIAL',NULL,NULL),(81,'请解释数据库索引失效的常见原因哈哈哈。','请解释数据库索引失效的常见原因哈哈哈。','函数计算、隐式类型转换、前导模糊匹配等会导致索引失效。',2,'java','自定义题库,题库导入模板',17,1,'2026-03-26 13:17:01','CUSTOM',1,1),(82,'请说明缓存穿透和缓存击穿的区别啦啦啦。','请说明缓存穿透和缓存击穿的区别啦啦啦。','缓存穿透是查询不存在数据，缓存击穿是热点 key 失效瞬间并发打到数据库。',2,'java','自定义题库,题库导入模板',22,1,'2026-03-26 13:17:01','CUSTOM',1,1),(85,'arch-test','c','a',2,'arch','t',22,1,'2026-03-26 21:04:19','OFFICIAL',NULL,NULL),(86,'Linux命令','ls','显示所有文件',2,'java','',23,1,'2026-03-26 22:30:47','OFFICIAL',NULL,NULL),(87,'Linux命令','pwd','显示当前文件路径',2,'linux','',37,1,'2026-03-26 22:31:43','OFFICIAL',NULL,NULL),(88,'请解释数据库索引失效的常见原因哈哈哈。','请解释数据库索引失效的常见原因哈哈哈。','函数计算、隐式类型转换、前导模糊匹配等会导致索引失效。',2,'java','官方题库/Linux',0,1,'2026-03-29 01:59:55','CUSTOM',2,1),(89,'请说明缓存穿透和缓存击穿的区别啦啦啦。','请说明缓存穿透和缓存击穿的区别啦啦啦。','缓存穿透是查询不存在数据，缓存击穿是热点 key 失效瞬间并发打到数据库。',2,'java','自定义题库/Linux',0,1,'2026-03-29 01:59:55','CUSTOM',2,1),(90,'语文1','语文1','创强名誉光',2,'语文','official-bank,题库导入模板',13,1,'2026-03-31 18:18:25','OFFICIAL',NULL,NULL);
/*!40000 ALTER TABLE `questions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_question_mastery`
--

DROP TABLE IF EXISTS `user_question_mastery`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `user_question_mastery` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL COMMENT '鍋氶鐢ㄦ埛ID',
  `question_id` bigint(20) NOT NULL COMMENT '棰樼洰ID',
  `confirmed_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鐢ㄦ埛纭浼氬仛鐨勬椂闂�',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_question_mastery_user_question` (`user_id`,`question_id`),
  KEY `idx_user_question_mastery_user` (`user_id`),
  KEY `idx_user_question_mastery_question` (`question_id`),
  CONSTRAINT `fk_user_question_mastery_question` FOREIGN KEY (`question_id`) REFERENCES `questions` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_user_question_mastery_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COMMENT='鐢ㄦ埛棰樼洰鎺屾彙纭璁板綍';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_question_mastery`
--

LOCK TABLES `user_question_mastery` WRITE;
/*!40000 ALTER TABLE `user_question_mastery` DISABLE KEYS */;
INSERT INTO `user_question_mastery` VALUES (1,27,76,'2026-03-26 18:46:09','2026-03-26 18:46:09','2026-03-26 18:46:09'),(2,1,5,'2026-03-26 22:28:39','2026-03-26 22:28:39','2026-03-26 22:28:39'),(3,1,85,'2026-03-26 22:28:53','2026-03-26 22:28:53','2026-03-26 22:28:53');
/*!40000 ALTER TABLE `user_question_mastery` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `users` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `username` varchar(64) NOT NULL COMMENT '用户名/账号',
  `password` varchar(255) NOT NULL COMMENT '密码 (BCrypt)',
  `recovery_phrase` varchar(255) DEFAULT NULL COMMENT '找回短语 (BCrypt)',
  `nickname` varchar(64) NOT NULL COMMENT '昵称',
  `role` varchar(20) NOT NULL DEFAULT 'USER' COMMENT '角色: ADMIN, USER, GUEST',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=35 DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'admin','$2a$10$pkyS3hs.5CNlIc1dyAyLBeEsPGdpSkYqxa//uROU3SjABBO4xvK8q',NULL,'admin','ADMIN','2026-03-24 11:20:40'),(2,'qq:3247804029','$2a$10$I4XAfJhMyhS.sqSCEov/QuGlTCJ0ry38pMbmkfa1l4uNqKV83fpNO',NULL,'张志琪','USER','2026-03-25 00:46:49'),(3,'_tmp_user_20260325','$2a$10$v1mrU2XjQg48dWeRVVIgNeMu.LHtGd5UcjzfF.LydzDZuL7hX1W.i',NULL,'tmp','USER','2026-03-25 02:38:08'),(25,'123456','$2a$10$2DWZbdRm31rf3Fqx5VnruetCLufb2DPJQgbsqL83bu3sYR9M8fSqq',NULL,'123456','USER','2026-03-26 00:24:11'),(27,'codex_uqm_1774521967','$2a$10$IJK9FFl0GjMBcge5y0rC6u.nl2GITAH1lm.DymlrRRcKJETjHn3ZW',NULL,'CodexTest','USER','2026-03-26 18:46:08'),(30,'aaaa','$2a$10$QvTr/78QPmNj8w.7PVFkMuk/9K5axClEate6d7vEK/mhqm6c4IRn2',NULL,'aaaa','USER','2026-03-27 14:36:49'),(31,'zzq3247804029@gmail.com','$2a$10$VMzowKqxJMp0v3iCtFHQkeLf4wvlM6ta221G89I2.LYmgx/.EyxYW',NULL,'zzq3247804029@gmail.com','USER','2026-03-31 19:12:29'),(34,'3247804029@qq.com','$2a$10$ijaegGBSrCwxomVXaB1G2u7gJRqhxMtCakSREYMxYUW4ZoY5r97qi',NULL,'超级管理','USER','2026-04-01 01:27:39');
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `wish_comments`
--

DROP TABLE IF EXISTS `wish_comments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `wish_comments` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `wish_id` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  `username` varchar(64) NOT NULL,
  `nickname` varchar(64) NOT NULL,
  `content` varchar(300) NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT 'logical delete',
  PRIMARY KEY (`id`),
  KEY `idx_wish_comments_wish_id_created` (`wish_id`,`created_at`),
  KEY `idx_wish_comments_user_id` (`user_id`),
  CONSTRAINT `fk_wish_comments_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_wish_comments_wish` FOREIGN KEY (`wish_id`) REFERENCES `wishes` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COMMENT='Wish wall comments';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `wish_comments`
--

LOCK TABLES `wish_comments` WRITE;
/*!40000 ALTER TABLE `wish_comments` DISABLE KEYS */;
INSERT INTO `wish_comments` VALUES (3,4,1,'admin','admin','在','2026-03-25 22:03:50',0),(4,1,25,'123456','123456','我是123456','2026-03-26 00:25:31',0),(5,1,1,'admin','admin','?','2026-03-26 00:26:15',0);
/*!40000 ALTER TABLE `wish_comments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `wish_likes`
--

DROP TABLE IF EXISTS `wish_likes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `wish_likes` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `wish_id` bigint(20) NOT NULL,
  `actor_type` varchar(16) NOT NULL COMMENT 'user/guest/visitor',
  `actor_id` varchar(128) NOT NULL COMMENT 'username or visitor id',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_wish_like_actor` (`wish_id`,`actor_type`,`actor_id`),
  KEY `idx_wish_likes_wish_id` (`wish_id`),
  KEY `idx_wish_likes_actor` (`actor_type`,`actor_id`),
  CONSTRAINT `fk_wish_likes_wish` FOREIGN KEY (`wish_id`) REFERENCES `wishes` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COMMENT='Wish wall likes';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `wish_likes`
--

LOCK TABLES `wish_likes` WRITE;
/*!40000 ALTER TABLE `wish_likes` DISABLE KEYS */;
INSERT INTO `wish_likes` VALUES (6,1,'user','123456','2026-03-26 00:25:36');
/*!40000 ALTER TABLE `wish_likes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `wishes`
--

DROP TABLE IF EXISTS `wishes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `wishes` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `content` text NOT NULL COMMENT '愿望内容',
  `emotion` varchar(32) DEFAULT NULL COMMENT '情绪分析结果 (happy, hopeful, confused, anxious)',
  `color` varchar(32) DEFAULT NULL COMMENT 'UI 面板颜色',
  `city` varchar(64) DEFAULT NULL COMMENT '城市/坐标',
  `pos_x` int(11) DEFAULT NULL COMMENT 'X坐标百分比 0-100',
  `pos_y` int(11) DEFAULT NULL COMMENT 'Y坐标百分比 0-100',
  `float_speed` double DEFAULT NULL COMMENT '漂浮速度调节因子',
  `status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '0: 待审核 1: 已上线 2: 违规',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT '逻辑删除标记: 1是, 0否',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=42 DEFAULT CHARSET=utf8mb4 COMMENT='星愿墙数据表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `wishes`
--

LOCK TABLES `wishes` WRITE;
/*!40000 ALTER TABLE `wishes` DISABLE KEYS */;
INSERT INTO `wishes` VALUES (1,'希望能拿到字节跳动的 Offer，加油冲呀！🚀','hopeful','#D4E0D0','河北',15,20,1.2,1,'2026-03-24 11:31:15',0),(2,'Q3结束前彻底掌握 React Hooks！💻','determined','#DDBFD1','上海',55,35,1,1,'2026-03-24 11:31:15',0),(3,'今年想为一个大型开源项目贡献代码。一起飞跃吧！✨','hopeful','#C4D0DE','北京',25,60,0.9,1,'2026-03-24 11:31:15',0),(4,'搞定系统设计面试，不再畏惧架构图！🏗️','confident','#D7E0CA','成都',65,70,1.5,1,'2026-03-24 11:31:15',0),(5,'正从 UI 转型全栈开发。一步一个脚印。🎨','determined','#E0D2C3','深圳',75,25,0.8,1,'2026-03-24 11:31:15',0),(6,'希望能拿到字节跳动的 Offer，加油冲呀！🚀','hopeful','#D4E0D0','河北',15,20,1.2,1,'2026-03-24 11:31:17',0),(7,'Q3结束前彻底掌握 React Hooks！💻','determined','#DDBFD1','上海',55,35,1,1,'2026-03-24 11:31:17',0),(8,'今年想为一个大型开源项目贡献代码。一起飞跃吧！✨','hopeful','#C4D0DE','北京',25,60,0.9,1,'2026-03-24 11:31:17',0),(9,'搞定系统设计面试，不再畏惧架构图！🏗️','confident','#D7E0CA','成都',65,70,1.5,1,'2026-03-24 11:31:17',0),(10,'正从 UI 转型全栈开发。一步一个脚印。🎨','determined','#E0D2C3','深圳',75,25,0.8,1,'2026-03-24 11:31:17',0),(11,'希望能拿到字节跳动的 Offer，加油冲呀！🚀','hopeful','#D4E0D0','河北',15,20,1.2,1,'2026-03-24 11:31:17',0),(12,'Q3结束前彻底掌握 React Hooks！💻','determined','#DDBFD1','上海',55,35,1,1,'2026-03-24 11:31:17',0),(13,'今年想为一个大型开源项目贡献代码。一起飞跃吧！✨','hopeful','#C4D0DE','北京',25,60,0.9,1,'2026-03-24 11:31:17',0),(14,'搞定系统设计面试，不再畏惧架构图！🏗️','confident','#D7E0CA','成都',65,70,1.5,1,'2026-03-24 11:31:17',0),(39,'smoke wish 9b09bb','hopeful','#DDBFD1','Shanghai',61,51,1.2,1,'2026-03-25 18:53:14',0),(41,'dsf a','confused','#B8C4D4','来自星海',67,14,0.8,1,'2026-03-25 23:18:53',0);
/*!40000 ALTER TABLE `wishes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping events for database 'novaleap'
--

--
-- Dumping routines for database 'novaleap'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-04-03  1:08:47
