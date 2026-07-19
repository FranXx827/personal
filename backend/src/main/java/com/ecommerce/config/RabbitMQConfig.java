package com.ecommerce.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;

/**
 * RabbitMQ 配置：交换机、队列、绑定、Confirm/Return
 */
@Slf4j
@Configuration
public class RabbitMQConfig {

    // ==================== 订单超时取消（延迟队列） ====================
    public static final String ORDER_DELAY_EXCHANGE = "order.delay.exchange";
    public static final String ORDER_DELAY_QUEUE = "order.delay.queue";
    public static final String ORDER_DEAD_EXCHANGE = "order.dead.exchange";
    public static final String ORDER_DEAD_QUEUE = "order.dead.queue";
    public static final String ORDER_DELAY_ROUTING_KEY = "order.delay";
    public static final String ORDER_DEAD_ROUTING_KEY = "order.dead";

    // ==================== 秒杀异步写库 ====================
    public static final String SECKILL_ORDER_EXCHANGE = "seckill.order.exchange";
    public static final String SECKILL_ORDER_QUEUE = "seckill.order.queue";
    public static final String SECKILL_ORDER_ROUTING_KEY = "seckill.order.create";

    /** 死信交换机（实际消费） */
    @Bean
    public DirectExchange orderDeadExchange() {
        return ExchangeBuilder.directExchange(ORDER_DEAD_EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue orderDeadQueue() {
        return QueueBuilder.durable(ORDER_DEAD_QUEUE).build();
    }

    @Bean
    public Binding orderDeadBinding() {
        return BindingBuilder.bind(orderDeadQueue()).to(orderDeadExchange()).with(ORDER_DEAD_ROUTING_KEY);
    }

    /** 延迟队列（消息过期后进入死信交换机） */
    @Bean
    public DirectExchange orderDelayExchange() {
        return ExchangeBuilder.directExchange(ORDER_DELAY_EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue orderDelayQueue() {
        return QueueBuilder.durable(ORDER_DELAY_QUEUE)
                .deadLetterExchange(ORDER_DEAD_EXCHANGE)
                .deadLetterRoutingKey(ORDER_DEAD_ROUTING_KEY)
                .ttl(30 * 60 * 1000) // 30 分钟
                .build();
    }

    @Bean
    public Binding orderDelayBinding() {
        return BindingBuilder.bind(orderDelayQueue()).to(orderDelayExchange()).with(ORDER_DELAY_ROUTING_KEY);
    }

    // ==================== 秒杀 ====================

    @Bean
    public DirectExchange seckillOrderExchange() {
        return ExchangeBuilder.directExchange(SECKILL_ORDER_EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue seckillOrderQueue() {
        return QueueBuilder.durable(SECKILL_ORDER_QUEUE).build();
    }

    @Bean
    public Binding seckillOrderBinding() {
        return BindingBuilder.bind(seckillOrderQueue())
                .to(seckillOrderExchange()).with(SECKILL_ORDER_ROUTING_KEY);
    }

    // ==================== 通用 ====================

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 配置 Confirm 和 Return 回调
     */
    @Configuration
    @Slf4j
    public static class RabbitTemplateConfig {

        private final RabbitTemplate rabbitTemplate;

        public RabbitTemplateConfig(RabbitTemplate rabbitTemplate) {
            this.rabbitTemplate = rabbitTemplate;
        }

        @PostConstruct
        public void init() {
            rabbitTemplate.setConfirmCallback((CorrelationData cd, boolean ack, String cause) -> {
                if (!ack) {
                    log.error("消息发送到Exchange失败: cause={}, id={}", cause, cd != null ? cd.getId() : null);
                }
            });
            rabbitTemplate.setReturnsCallback(returned -> {
                log.error("消息路由到Queue失败: exchange={}, routingKey={}, replyCode={}, replyText={}",
                        returned.getExchange(), returned.getRoutingKey(),
                        returned.getReplyCode(), returned.getReplyText());
            });
        }
    }
}
