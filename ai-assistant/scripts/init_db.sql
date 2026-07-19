-- ============================================================
-- AI 助手 - 数据库初始化脚本
-- 数据库: ai_assistant
-- 用于存储对话会话与消息历史
-- ============================================================

CREATE DATABASE IF NOT EXISTS `ai_assistant`
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `ai_assistant`;

-- ============================================================
-- 1. 会话表
-- ============================================================
CREATE TABLE IF NOT EXISTS `chat_session` (
  `id`         VARCHAR(64)  NOT NULL COMMENT '会话ID (UUID)',
  `user_id`    BIGINT       NOT NULL COMMENT '用户ID',
  `title`      VARCHAR(200) NOT NULL DEFAULT '新对话' COMMENT '会话标题',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`    BIGINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0=正常, >0=已删除时间戳',
  PRIMARY KEY (`id`),
  KEY `idx_user_updated` (`user_id`, `updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 对话会话';

-- ============================================================
-- 2. 消息表
-- ============================================================
CREATE TABLE IF NOT EXISTS `chat_message` (
  `id`            VARCHAR(64)   NOT NULL COMMENT '消息ID (UUID)',
  `session_id`    VARCHAR(64)   NOT NULL COMMENT '会话ID',
  `role`          VARCHAR(16)   NOT NULL COMMENT 'user / assistant / system / tool',
  `content`       LONGTEXT      NOT NULL COMMENT '消息内容',
  `metadata_json` TEXT          DEFAULT NULL COMMENT '工具调用等附加信息的JSON',
  `created_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_session_created` (`session_id`, `created_at`),
  CONSTRAINT `fk_message_session` FOREIGN KEY (`session_id`) REFERENCES `chat_session`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 对话消息';
