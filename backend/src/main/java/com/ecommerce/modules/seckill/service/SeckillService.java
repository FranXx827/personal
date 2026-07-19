package com.ecommerce.modules.seckill.service;

import com.ecommerce.modules.seckill.dto.SeckillResult;

public interface SeckillService {

    SeckillResult seckill(Long skuId, Long userId);

    void initStockToRedis(Long seckillGoodsId);

    void asyncCreateOrder(String message);
}
