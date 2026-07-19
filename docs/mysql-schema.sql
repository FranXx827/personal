-- ============================================================
-- 智能电商平台 - 数据库初始化脚本
-- 技术栈: Spring Boot + MyBatis-Plus + MySQL 8.0
-- 数据库: ecommerce
-- ============================================================

CREATE DATABASE IF NOT EXISTS ecommerce
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE ecommerce;

-- ============================================================
-- 1. 用户表（买家 / 商户统一）
-- ============================================================
DROP TABLE IF EXISTS `order_item`;
DROP TABLE IF EXISTS `payment`;
DROP TABLE IF EXISTS `cart_item`;
DROP TABLE IF EXISTS `seckill_goods`;
DROP TABLE IF EXISTS `sku`;
DROP TABLE IF EXISTS `product`;
DROP TABLE IF EXISTS `merchant`;
DROP TABLE IF EXISTS `order`;
DROP TABLE IF EXISTS `user`;

CREATE TABLE `user` (
  `id`            BIGINT        NOT NULL COMMENT '雪花ID',
  `username`      VARCHAR(64)   NOT NULL COMMENT '登录名',
  `password_hash` VARCHAR(255)  NOT NULL COMMENT 'BCrypt密文',
  `nickname`      VARCHAR(64)   DEFAULT NULL COMMENT '昵称',
  `phone`         VARCHAR(32)   DEFAULT NULL,
  `avatar`        VARCHAR(512)  DEFAULT NULL COMMENT '头像URL',
  `role`          VARCHAR(32)   NOT NULL DEFAULT 'BUYER' COMMENT 'BUYER/MERCHANT',
  `status`        INT           NOT NULL DEFAULT 0 COMMENT '0-正常 1-禁用',
  `created_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`       TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删 1-已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_phone` (`phone`),
  KEY `idx_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户（买家/商户统一）';

-- ============================================================
-- 2. 商户/店铺表
-- ============================================================
CREATE TABLE `merchant` (
  `id`            BIGINT         NOT NULL,
  `user_id`       BIGINT         NOT NULL COMMENT '关联 user.id',
  `name`          VARCHAR(128)   NOT NULL COMMENT '店铺名称',
  `description`   VARCHAR(1024)  DEFAULT NULL,
  `contact_phone` VARCHAR(32)    DEFAULT NULL,
  `contact_email` VARCHAR(128)   DEFAULT NULL,
  `audit_status`  VARCHAR(32)    NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED',
  `reject_reason` VARCHAR(512)   DEFAULT NULL,
  `created_at`    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`       TINYINT(1)     NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  KEY `idx_audit_status` (`audit_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户/店铺';

-- ============================================================
-- 3. 商品表（含复合索引优化商品搜索）
-- ============================================================
CREATE TABLE `product` (
  `id`           BIGINT         NOT NULL,
  `merchant_id`  BIGINT         NOT NULL,
  `title`        VARCHAR(255)   NOT NULL,
  `description`  TEXT,
  `category_id`  BIGINT         DEFAULT NULL COMMENT '分类ID',
  `price`        DECIMAL(12,2)  NOT NULL DEFAULT 0.00,
  `main_image`   VARCHAR(512)   DEFAULT NULL COMMENT '主图URL',
  `sales`        INT            NOT NULL DEFAULT 0 COMMENT '销量',
  `rating`       DECIMAL(5,2)   NOT NULL DEFAULT 5.00 COMMENT '评分1.00-5.00',
  `status`       INT            NOT NULL DEFAULT 0 COMMENT '0-上架 1-下架',
  `created_at`   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`      TINYINT(1)     NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  -- 商品搜索复合索引：覆盖 category + status 避免回表
  KEY `idx_category_status` (`category_id`, `status`),
  -- 商户维度查询索引
  KEY `idx_merchant` (`merchant_id`),
  -- 价格范围查询索引
  KEY `idx_price` (`price`),
  -- 销量排序索引
  KEY `idx_sales` (`sales`),
  -- 评分排序索引
  KEY `idx_rating` (`rating`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品';

-- ============================================================
-- 4. SKU 表（库存扣减核心表 - 唯一索引防超卖）
-- ============================================================
CREATE TABLE `sku` (
  `id`         BIGINT         NOT NULL,
  `product_id` BIGINT         NOT NULL,
  `specs`      VARCHAR(1024)  DEFAULT NULL COMMENT 'JSON规格 {"color":"红","size":"L"}',
  `stock`      INT            NOT NULL DEFAULT 0 COMMENT '库存（乐观锁字段）',
  `price`      DECIMAL(12,2)  NOT NULL DEFAULT 0.00,
  `version`    INT            NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_at` DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`    TINYINT(1)     NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_product` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品SKU';

-- ============================================================
-- 5. 购物车表
-- ============================================================
CREATE TABLE `cart_item` (
  `id`         BIGINT      NOT NULL,
  `user_id`    BIGINT      NOT NULL,
  `sku_id`     BIGINT      NOT NULL,
  `product_id` BIGINT      DEFAULT NULL,
  `quantity`   INT         NOT NULL DEFAULT 1,
  `selected`   TINYINT(1)  NOT NULL DEFAULT 1 COMMENT '是否选中结算',
  `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`    TINYINT(1)  NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  -- 用户+SKU唯一约束：同一用户同一SKU只能有一条购物车记录
  UNIQUE KEY `uk_user_sku` (`user_id`, `sku_id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车项';

-- ============================================================
-- 6. 订单主表（order 为 MySQL 保留字，必须反引号包裹）
-- ============================================================
CREATE TABLE `order` (
  `id`               BIGINT        NOT NULL,
  `order_no`         VARCHAR(32)   NOT NULL COMMENT '订单号',
  `user_id`          BIGINT        NOT NULL,
  `merchant_id`      BIGINT        DEFAULT NULL,
  `total_amount`     DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '订单总金额',
  `pay_amount`       DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '实付金额',
  `status`           VARCHAR(32)   NOT NULL DEFAULT 'PENDING_PAY' COMMENT '订单状态',
  `receiver_name`    VARCHAR(64)   DEFAULT NULL,
  `receiver_phone`   VARCHAR(32)   DEFAULT NULL,
  `receiver_address` VARCHAR(512)  DEFAULT NULL,
  `remark`           VARCHAR(512)  DEFAULT NULL COMMENT '买家备注',
  `expire_time`      DATETIME      DEFAULT NULL COMMENT '支付截止时间（超时取消）',
  `created_at`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`          TINYINT(1)    NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  -- 订单状态调度：按用户+状态查询待处理订单
  KEY `idx_user_status` (`user_id`, `status`),
  -- 商户维度订单查询
  KEY `idx_merchant` (`merchant_id`),
  -- 超时订单扫描索引（RabbitMQ延迟队列补偿扫描）
  KEY `idx_status_expire` (`status`, `expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单主表';

-- ============================================================
-- 7. 订单明细表
-- ============================================================
CREATE TABLE `order_item` (
  `id`             BIGINT         NOT NULL,
  `order_id`       BIGINT         NOT NULL,
  `sku_id`         BIGINT         DEFAULT NULL,
  `product_id`     BIGINT         DEFAULT NULL,
  `product_title`  VARCHAR(255)   DEFAULT NULL,
  `product_image`  VARCHAR(512)   DEFAULT NULL,
  `sku_specs`      VARCHAR(1024)  DEFAULT NULL COMMENT 'JSON规格',
  `quantity`       INT            NOT NULL DEFAULT 1,
  `price`          DECIMAL(12,2)  NOT NULL DEFAULT 0.00 COMMENT '下单时快照价格',
  `created_at`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted`        TINYINT(1)     NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_order` (`order_id`),
  KEY `idx_product` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细';

-- ============================================================
-- 8. 支付表
-- ============================================================
CREATE TABLE `payment` (
  `id`             BIGINT         NOT NULL,
  `order_id`       BIGINT         NOT NULL,
  `user_id`        BIGINT         DEFAULT NULL,
  `amount`         DECIMAL(12,2)  NOT NULL DEFAULT 0.00,
  `pay_method`     VARCHAR(32)    DEFAULT NULL COMMENT 'ALIPAY/WECHAT/BALANCE',
  `pay_status`     VARCHAR(32)    DEFAULT NULL COMMENT 'PENDING/SUCCESS/FAILED/REFUNDED',
  `transaction_id` VARCHAR(128)   DEFAULT NULL COMMENT '第三方支付流水号',
  `created_at`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`        TINYINT(1)     NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_id` (`order_id`) COMMENT '一单一次支付',
  KEY `idx_user` (`user_id`),
  KEY `idx_pay_status` (`pay_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付记录';

-- ============================================================
-- 9. 秒杀商品表
-- ============================================================
CREATE TABLE `seckill_goods` (
  `id`            BIGINT        NOT NULL,
  `sku_id`        BIGINT        NOT NULL,
  `product_id`    BIGINT        DEFAULT NULL,
  `seckill_stock` INT           NOT NULL DEFAULT 0 COMMENT '秒杀库存',
  `seckill_price` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '秒杀价',
  `start_time`    DATETIME      DEFAULT NULL COMMENT '秒杀开始时间',
  `end_time`      DATETIME      DEFAULT NULL COMMENT '秒杀结束时间',
  `created_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted`       TINYINT(1)    NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sku_id` (`sku_id`) COMMENT '每个SKU只能有一个秒杀活动',
  KEY `idx_time_range` (`start_time`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀商品';


-- ############################################################
#
#                    测试数据 INSERT
#
-- ############################################################

-- ======================== 用户测试数据 ========================
INSERT INTO `user` (`id`, `username`, `password_hash`, `nickname`, `phone`, `avatar`, `role`, `status`) VALUES
(180000000001, 'zhangsan', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '张三', '13800138001', 'https://example.com/avatar/zhangsan.jpg', 'BUYER', 0),
(180000000002, 'lisi', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '李四', '13800138002', NULL, 'BUYER', 0),
(180000000003, 'wangwu', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '王五', '13800138003', 'https://example.com/avatar/wangwu.jpg', 'BUYER', 0),
(180000000004, 'merchant_01', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '华为官方旗舰店', '13900139001', NULL, 'MERCHANT', 0),
(180000000005, 'merchant_02', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '小米精品店', '13900139002', NULL, 'MERCHANT', 0),
(180000000006, 'merchant_03', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'Nike运动专营', '13900139003', NULL, 'MERCHANT', 0),
(180000000007, 'zhaoliu', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '赵六', '13800138004', NULL, 'BUYER', 1);

-- ======================== 商户测试数据 ========================
INSERT INTO `merchant` (`id`, `user_id`, `name`, `description`, `contact_phone`, `contact_email`, `audit_status`) VALUES
(190000000001, 180000000004, '华为官方旗舰店', '华为全系列手机、平板、穿戴设备正品保障，全国联保。', '400-123-4567', 'service@huawei.com', 'APPROVED'),
(190000000002, 180000000005, '小米精品店', '小米生态链产品，性价比之选，新品首发。', '010-88889999', 'xiaomi@shop.com', 'APPROVED'),
(190000000003, 180000000006, 'Nike运动专营', '正品Nike运动鞋服，限量联名款首发。', '021-66667777', 'nike@sports.com', 'REJECTED'),
(190000000004, 180000000007, '苹果专营店', 'Apple授权经销商，iPhone/Mac/iPad全系现货', '0755-99998888', 'apple@shop.com', 'PENDING');

UPDATE `merchant` SET `reject_reason` = '营业执照已过期，请更新后重新提交审核' WHERE `id` = 190000000003;

-- ======================== 商品测试数据 ========================
INSERT INTO `product` (`id`, `merchant_id`, `title`, `description`, `category_id`, `price`, `main_image`, `sales`, `rating`, `status`) VALUES
(200000000001, 190000000001, '华为 Mate 70 Pro 12GB+256GB 曜石黑', '麒麟9000S芯片，卫星通话，昆仑玻璃，HarmonyOS 4', 1, 6999.00, 'https://example.com/img/mate70pro.jpg', 12580, 4.85, 0),
(200000000002, 190000000001, '华为 FreeBuds Pro 3 星闪连接耳机', '星闪连接技术，47dB智能动态降噪，30小时综合续航', 2, 1499.00, 'https://example.com/img/freebuds_pro3.jpg', 8920, 4.78, 0),
(200000000003, 190000000002, '小米15 Ultra 徕卡影像旗舰', '徕卡一英寸大底，骁龙8 Gen3，双向卫星通信', 1, 5999.00, 'https://example.com/img/mi15ultra.jpg', 23450, 4.92, 0),
(200000000004, 190000000002, '小米手环8 Pro 全面屏智能手环', '1.74英寸AMOLED大屏，血氧检测，150+运动模式', 3, 299.00, 'https://example.com/img/miband8pro.jpg', 45670, 4.65, 0),
(200000000005, 190000000002, '小米电视 S Pro 75英寸 Mini LED', 'Mini LED背光，144Hz高刷，量子点广色域', 4, 6999.00, 'https://example.com/img/mitv_spro75.jpg', 3200, 4.80, 0),
(200000000006, 190000000001, '华为 WATCH GT 4 智能手表', '41mm曜石黑，心率血氧监测，100+运动模式，14天续航', 3, 1488.00, 'https://example.com/img/watch_gt4.jpg', 5670, 4.72, 0),
(200000000007, 190000000006, 'Nike Air Jordan 1 Retro High OG', '芝加哥配色，经典复古篮球鞋，头层牛皮材质', 5, 1599.00, 'https://example.com/img/aj1_chicago.jpg', 890, 4.95, 0),
(200000000008, 190000000002, 'Redmi K70 Pro 第三代骁龙8', '第三代骁龙8处理器，2K中国屏，120W神仙秒充', 1, 3299.00, 'https://example.com/img/redmi_k70pro.jpg', 38920, 4.68, 0);

-- SKU 测试数据
INSERT INTO `sku` (`id`, `product_id`, `specs`, `stock`, `price`, `version`) VALUES
-- Mate 70 Pro SKU
(210000000001, 200000000001, '{"color":"曜石黑","storage":"256GB"}', 200, 6999.00, 0),
(210000000002, 200000000001, '{"color":"曜石黑","storage":"512GB"}', 100, 7999.00, 0),
(210000000003, 200000000001, '{"color":"皓月白","storage":"256GB"}', 150, 6999.00, 0),
(210000000004, 200000000001, '{"color":"皓月白","storage":"512GB"}', 80, 7999.00, 0),
-- FreeBuds Pro 3 SKU
(210000000005, 200000000002, '{"color":"星河蓝"}', 500, 1499.00, 0),
(210000000006, 200000000002, '{"color":"陶瓷白"}', 300, 1499.00, 0),
-- 小米15 Ultra SKU
(210000000007, 200000000003, '{"color":"黑色","storage":"12GB+256GB"}', 180, 5999.00, 0),
(210000000008, 200000000003, '{"color":"黑色","storage":"16GB+512GB"}', 90, 6499.00, 0),
(210000000009, 200000000003, '{"color":"白色","storage":"16GB+1TB"}', 50, 6999.00, 0),
-- 小米手环8 Pro SKU
(210000000010, 200000000004, '{"color":"黑色"}', 1000, 299.00, 0),
(210000000011, 200000000004, '{"color":"白色"}', 800, 299.00, 0),
(210000000012, 200000000004, '{"color":"蓝色"}', 600, 299.00, 0),
-- Nike AJ1 SKU
(210000000013, 200000000007, '{"size":"US 9 / CN 43"}', 50, 1599.00, 0),
(210000000014, 200000000007, '{"size":"US 10 / CN 44"}', 30, 1599.00, 0),
(210000000015, 200000000007, '{"size":"US 11 / CN 45"}', 20, 1599.00, 0);

-- ======================== 购物车测试数据 ========================
INSERT INTO `cart_item` (`id`, `user_id`, `sku_id`, `product_id`, `quantity`, `selected`) VALUES
(220000000001, 180000000001, 210000000001, 200000000001, 1, 1),
(220000000002, 180000000001, 210000000005, 200000000002, 2, 1),
(220000000003, 180000000001, 210000000010, 200000000004, 1, 0),
(220000000004, 180000000002, 210000000007, 200000000003, 1, 1),
(220000000005, 180000000002, 210000000013, 200000000007, 1, 1),
(220000000006, 180000000003, 210000000003, 200000000001, 1, 1),
(220000000007, 180000000003, 210000000011, 200000000004, 3, 1);

-- ======================== 订单测试数据（覆盖多种状态） ========================
INSERT INTO `order` (`id`, `order_no`, `user_id`, `merchant_id`, `total_amount`, `pay_amount`, `status`, `receiver_name`, `receiver_phone`, `receiver_address`, `remark`, `expire_time`) VALUES
-- 待支付订单（模拟 RabbitMQ 延迟队列场景）
(230000000001, 'ORD202601180001', 180000000001, 190000000001, 8498.00, 8498.00, 'PENDING_PAY', '张三', '13800138001', '北京市朝阳区建国路88号SOHO现代城A座1201室', '尽快发货', DATE_ADD(NOW(), INTERVAL 30 MINUTE)),
-- 已支付待发货
(230000000002, 'ORD202601170001', 180000000002, 190000000002, 7598.00, 7598.00, 'PAID', '李四', '13800138002', '上海市浦东新区陆家嘴环路100号', '', NULL),
-- 已发货
(230000000003, 'ORD202601160001', 180000000001, 190000000001, 6999.00, 6799.00, 'SHIPPED', '张三', '13800138001', '北京市朝阳区建国路88号SOHO现代城A座1201室', '工作日配送', NULL),
-- 已完成
(230000000004, 'ORD202601150001', 180000000003, 190000000002, 896.00, 876.00, 'COMPLETED', '王五', '13800138003', '广州市天河区珠江新城华夏路10号', NULL, NULL),
-- 已取消（用户主动取消 / 超时自动取消）
(230000000005, 'ORD202601140001', 180000000001, 190000000002, 299.00, 0.00, 'CANCELED', '张三', '13800138001', '北京市朝阳区建国路88号SOHO现代城A座1201室', '不想要了', NULL),
-- 已关闭（退款完成后的终态）
(230000000006, 'ORD202601130001', 180000000002, 190000000006, 1488.00, 0.00, 'CLOSED', '李四', '13800138002', '上海市浦东新区陆家嘴环路100号', '质量有问题申请退款', NULL),
-- 退款中
(230000000007, 'ORD202601120001', 180000000003, 190000000001, 6999.00, 6999.00, 'REFUNDING', '王五', '13800138003', '广州市天河区珠江新城华夏路10号', '与描述不符', NULL),
-- 已退款
(230000000008, 'ORD202601110001', 180000000001, 190000000002, 5999.00, 0.00, 'REFUNDED', '张三', '13800138001', '北京市朝阳区建国路88号SOHO现代城A座1201室', NULL, NULL);

-- ======================== 订单明细测试数据 ========================
INSERT INTO `order_item` (`id`, `order_id`, `sku_id`, `product_id`, `product_title`, `product_image`, `sku_specs`, `quantity`, `price`) VALUES
-- 订单1: 张三待支付 - Mate70Pro + FreeBuds Pro3 x2
(240000000001, 230000000001, 210000000001, 200000000001, '华为 Mate 70 Pro 12GB+256GB 曜石黑', 'https://example.com/img/mate70pro.jpg', '{"color":"曜石黑","storage":"256GB"}', 1, 6999.00),
(240000000002, 230000000001, 210000000005, 200000000002, '华为 FreeBuds Pro 3 星闪连接耳机', 'https://example.com/img/freebuds_pro3.jpg', '{"color":"星河蓝"}', 2, 1499.00),
-- 订单2: 李四已支付 - 小米15Ultra + 手环
(240000000003, 230000000002, 210000000007, 200000000003, '小米15 Ultra 徕卡影像旗舰', 'https://example.com/img/mi15ultra.jpg', '{"color":"黑色","storage":"12GB+256GB"}', 1, 5999.00),
(240000000004, 230000000002, 210000000010, 200000000004, '小米手环8 Pro 全面屏智能手环', 'https://example.com/img/miband8pro.jpg', '{"color":"黑色"}', 1, 299.00),
-- 订单3: 张三已发货 - Mate70Pro (另一单)
(240000000005, 230000000003, 210000000003, 200000000001, '华为 Mate 70 Pro 12GB+256GB 曜石黑', 'https://example.com/img/mate70pro.jpg', '{"color":"皓月白","storage":"256GB"}', 1, 6999.00),
-- 订单4: 王五已完成 - 手环x3
(240000000006, 230000000004, 210000000011, 200000000004, '小米手环8 Pro 全面屏智能手环', 'https://example.com/img/miband8pro.jpg', '{"color":"白色"}', 3, 299.00),
-- 订单5: 张三已取消 - 手环
(240000000007, 230000000005, 210000000010, 200000000004, '小米手环8 Pro 全面屏智能手环', 'https://example.com/img/miband8pro.jpg', '{"color":"黑色"}', 1, 299.00),
-- 订单6: 李四已关闭 - 华为Watch GT4
(240000000008, 230000000006, 210000000006, 200000000006, '华为 WATCH GT 4 智能手表', 'https://example.com/img/watch_gt4.jpg', NULL, 1, 1488.00),
-- 订单7: 王五退款中 - Mate70Pro
(240000000009, 230000000007, 210000000001, 200000000001, '华为 Mate 70 Pro 12GB+256GB 曜石黑', 'https://example.com/img/mate70pro.jpg', '{"color":"曜石黑","storage":"256GB"}', 1, 6999.00),
-- 订单8: 张三已退款 - 小米15Ultra
(240000000010, 230000000008, 210000000007, 200000000003, '小米15 Ultra 徕卡影像旗舰', 'https://example.com/img/mi15ultra.jpg', '{"color":"黑色","storage":"12GB+256GB"}', 1, 5999.00);

-- ======================== 支付测试数据 ========================
INSERT INTO `payment` (`id`, `order_id`, `user_id`, `amount`, `pay_method`, `pay_status`, `transaction_id`) VALUES
-- 订单2: 李四支付成功
(250000000001, 230000000002, 180000000002, 7598.00, 'WECHAT', 'SUCCESS', 'WX20260117143052123456'),
-- 订单3: 张三支付成功（有优惠）
(250000000002, 230000000003, 180000000001, 6799.00, 'ALIPAY', 'SUCCESS', 'ALI20260116103011223344'),
-- 订单4: 王五支付成功（有优惠）
(250000000003, 230000000004, 180000000003, 876.00, 'WECHAT', 'SUCCESS', 'WX20260115201533445566'),
-- 订单6: 已退款
(250000000004, 230000000006, 180000000002, 1488.00, 'ALIPAY', 'REFUNDED', 'ALI20260113111555667788'),
-- 订单7: 退款中
(250000000005, 230000000007, 180000000003, 6999.00, 'WECHAT', 'REFUNDING', 'WX20260112180077889900'),
-- 订单8: 已退款
(250000000006, 230000000008, 180000000001, 5999.00, 'ALIPAY', 'REFUNDED', 'ALI20260111090011223344');

-- ======================== 秒杀商品测试数据 ========================
INSERT INTO `seckill_goods` (`id`, `sku_id`, `product_id`, `seckill_stock`, `seckill_price`, `start_time`, `end_time`) VALUES
-- 明天上午10点开始的秒杀活动
(260000000001, 210000000001, 200000000001, 50, 4999.00, DATE_ADD(CURDATE(), INTERVAL 1 DAY) + INTERVAL 10 HOUR, DATE_ADD(CURDATE(), INTERVAL 1 DAY) + INTERVAL 12 HOUR),
-- 正在进行的秒杀
(260000000002, 210000000010, 200000000004, 100, 99.00, DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 2 HOUR)),
-- 即将开始的秒杀
(260000000003, 210000000013, 200000000007, 20, 899.00, DATE_ADD(NOW(), INTERVAL 2 HOUR), DATE_ADD(NOW(), INTERVAL 6 HOUR));

-- ============================================================
-- 数据验证查询（可选执行，确认数据正确性）
-- ============================================================
SELECT '=== 用户统计 ===' AS info; SELECT role, COUNT(*) FROM `user` GROUP BY role;
SELECT '=== 商品统计 ===' AS info; SELECT merchant_id, COUNT(*) AS product_count FROM `product` GROUP BY merchant_id;
SELECT '=== 订单状态分布 ===' AS info; SELECT status, COUNT(*) FROM `order` GROUP BY status;
SELECT '=== 支付状态分布 ===' AS info; SELECT pay_status, COUNT(*) FROM `payment` GROUP BY pay_status;
SELECT '=== 库存概览 ===' AS info; SELECT p.title, s.specs, s.stock, s.price FROM sku s JOIN product p ON s.product_id = p.id WHERE s.stock < 100 ORDER BY s.stock ASC;

-- ============================================================
-- 10. AI 导购助手 - 会话与消息表（独立扩展，供 ai-assistant 服务使用）
-- ============================================================
CREATE TABLE `chat_session` (
  `id`             VARCHAR(64)  NOT NULL COMMENT '会话ID (UUID)',
  `user_id`        BIGINT       DEFAULT NULL COMMENT '关联 user.id',
  `title`          VARCHAR(255) DEFAULT NULL COMMENT '会话标题',
  `state_snapshot` JSON         DEFAULT NULL COMMENT 'LangGraph 状态快照',
  `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`        TINYINT(1)   NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 会话';

CREATE TABLE `chat_message` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `session_id` VARCHAR(64)  NOT NULL,
  `role`       VARCHAR(32)  NOT NULL COMMENT 'user/assistant/system',
  `content`    TEXT         NOT NULL,
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted`    TINYINT(1)   NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_session` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 消息';
