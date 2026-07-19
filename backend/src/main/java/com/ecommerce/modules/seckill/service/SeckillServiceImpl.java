package com.ecommerce.modules.seckill.service;

import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.config.RabbitMQConfig;
import com.ecommerce.modules.seckill.dto.SeckillResult;
import com.ecommerce.modules.seckill.entity.SeckillGoods;
import com.ecommerce.modules.seckill.mapper.SeckillGoodsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillServiceImpl implements SeckillService {

    private static final String SECKILL_STOCK_PREFIX = "seckill:stock:";

    private final SeckillGoodsMapper seckillGoodsMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RabbitTemplate rabbitTemplate;

    @Override
    public SeckillResult seckill(Long skuId, Long userId) {
        SeckillGoods goods = seckillGoodsMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SeckillGoods>()
                        .eq(SeckillGoods::getSkuId, skuId)
        );
        if (goods == null) {
            return new SeckillResult(false, "秒杀商品不存在", null);
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(goods.getStartTime())) {
            return new SeckillResult(false, "秒杀尚未开始", null);
        }
        if (now.isAfter(goods.getEndTime())) {
            return new SeckillResult(false, "秒杀已结束", null);
        }

        // Lua 脚本原子扣减库存
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setLocation(new ClassPathResource("lua/seckill.lua"));
        redisScript.setResultType(Long.class);

        String stockKey = SECKILL_STOCK_PREFIX + goods.getId();
        Long result = redisTemplate.execute(redisScript, Collections.singletonList(stockKey), 1);

        if (result == null) {
            return new SeckillResult(false, "系统繁忙，请重试", null);
        }

        if (result == 1) {
            // 扣减成功，发送 MQ 消息异步创建订单
            String msg = goods.getId() + ":" + userId;
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.SECKILL_ORDER_EXCHANGE,
                    RabbitMQConfig.SECKILL_ORDER_ROUTING_KEY,
                    msg
            );
            log.info("秒杀扣库存成功: skuId={}, userId={}", skuId, userId);
            return new SeckillResult(true, "排队中", null);
        } else if (result == -1) {
            return new SeckillResult(false, "库存不足", null);
        }

        return new SeckillResult(false, "系统繁忙，请重试", null);
    }

    @Override
    public void initStockToRedis(Long seckillGoodsId) {
        SeckillGoods goods = seckillGoodsMapper.selectById(seckillGoodsId);
        if (goods == null) {
            throw new ResourceNotFoundException("秒杀商品", seckillGoodsId);
        }
        String stockKey = SECKILL_STOCK_PREFIX + seckillGoodsId;
        redisTemplate.opsForValue().set(stockKey, goods.getSeckillStock());
        log.info("秒杀库存已加载到Redis: goodsId={}, stock={}", seckillGoodsId, goods.getSeckillStock());
    }

    @Override
    public void asyncCreateOrder(String message) {
        // message 格式: seckillGoodsId:userId
        String[] parts = message.split(":");
        if (parts.length < 2) {
            log.error("秒杀消息格式错误: {}", message);
            return;
        }
        Long seckillGoodsId = Long.parseLong(parts[0]);
        Long userId = Long.parseLong(parts[1]);

        SeckillGoods goods = seckillGoodsMapper.selectById(seckillGoodsId);
        if (goods == null) {
            log.error("秒杀商品不存在: goodsId={}", seckillGoodsId);
            return;
        }

        // TODO: 创建订单逻辑，调用 OrderService 或直接操作 DB
        log.info("秒杀异步创建订单: goodsId={}, userId={}, skuId={}, price={}",
                seckillGoodsId, userId, goods.getSkuId(), goods.getSeckillPrice());
    }
}
