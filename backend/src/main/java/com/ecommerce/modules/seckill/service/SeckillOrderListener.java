package com.ecommerce.modules.seckill.service;

import com.ecommerce.config.RabbitMQConfig;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillOrderListener {

    private final SeckillService seckillService;

    @RabbitListener(queues = RabbitMQConfig.SECKILL_ORDER_QUEUE)
    public void handleSeckillOrder(org.springframework.amqp.core.Message message, Channel channel,
                                   @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        try {
            String msg = new String(message.getBody());
            seckillService.asyncCreateOrder(msg);
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("处理秒杀订单消息失败", e);
            channel.basicNack(tag, false, true);
        }
    }
}
