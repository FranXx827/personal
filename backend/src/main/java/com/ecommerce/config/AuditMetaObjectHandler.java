package com.ecommerce.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 审计字段自动填充处理器
 *
 * <p>统一为所有标记了 {@code @TableField(fill = ...)} 的字段赋值，
 * 避免在各 Service 中重复手写 {@code setCreatedAt/setUpdatedAt}，
 * 也保证数据库 NOT NULL 的审计列（created_at / updated_at）在插入/更新时一定有值。</p>
 *
 * <p>使用 {@link #setFieldValByName(String, Object, MetaObject)} 而非严格填充方法：
 * 该方法在字段不存在时静默跳过（内部判断 {@code hasSetter}），因此对仅声明了
 * {@code createdAt} 而没有 {@code updatedAt} 的实体（如 order_item、seckill_goods）也安全，
 * 且不依赖具体 MyBatis-Plus 版本才提供的 {@code openStrict()} 方法。</p>
 */
@Slf4j
@Component
public class AuditMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        this.setFieldValByName("createdAt", now, metaObject);
        this.setFieldValByName("updatedAt", now, metaObject);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.setFieldValByName("updatedAt", LocalDateTime.now(), metaObject);
    }
}
