-- ============================================================
-- AI 对话管理 — 建表语句（供 Java Spring Backend + MyBatis-Plus 使用）
-- 直接在你的外接数据库中执行即可
-- 兼容：MySQL 8.0+
-- ============================================================

-- 1. 对话会话表
CREATE TABLE IF NOT EXISTS `chat_session` (
  `id`         VARCHAR(64)  NOT NULL COMMENT '会话ID (UUID)',
  `user_id`    BIGINT       NOT NULL COMMENT '用户ID (关联 user.id)',
  `title`      VARCHAR(200) NOT NULL DEFAULT '新对话' COMMENT '会话标题',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`    BIGINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0=正常, >0=已删除(UNIX时间戳)',
  PRIMARY KEY (`id`),
  KEY `idx_user_updated` (`user_id`, `updated_at`) COMMENT '用户维度按更新时间倒序查会话列表'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 对话会话';

-- 2. 对话消息表
CREATE TABLE IF NOT EXISTS `chat_message` (
  `id`            VARCHAR(64) NOT NULL COMMENT '消息ID (UUID)',
  `session_id`    VARCHAR(64) NOT NULL COMMENT '会话ID',
  `role`          VARCHAR(16) NOT NULL COMMENT 'user / assistant / system / tool',
  `content`       LONGTEXT    NOT NULL COMMENT '消息内容',
  `metadata_json` TEXT         DEFAULT NULL COMMENT '工具调用等附加信息的 JSON',
  `created_at`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted`       TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_session_created` (`session_id`, `created_at`) COMMENT '按会话和时间顺序查消息',
  CONSTRAINT `fk_message_session` FOREIGN KEY (`session_id`) REFERENCES `chat_session`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 对话消息';
