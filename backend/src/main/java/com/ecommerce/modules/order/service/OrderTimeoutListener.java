package com.ecommerce.modules.order.service;

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
public class OrderTimeoutListener {

    private final OrderService orderService;

    @RabbitListener(queues = RabbitMQConfig.ORDER_DEAD_QUEUE)
    public void handleTimeout(org.springframework.amqp.core.Message message, Channel channel,
                              @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        try {
            String orderNo = new String(message.getBody());
            log.info("接收订单超时消息: orderNo={}", orderNo);
            orderService.cancelByTimeout(orderNo);
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("处理订单超时消息失败", e);
            channel.basicNack(tag, false, true);
        }
    }
}
