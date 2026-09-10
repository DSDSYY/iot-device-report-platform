package com.example.iot.mq;

import com.example.iot.config.RabbitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 上报消息生产者（RabbitMQ）
 *
 * <p>说明：调用方拿到 false 后必须"回滚 Redis 去重 Key"，
 * 否则客户端重试同一批 msgId 会被误判为重复而丢数据。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReportMessageProducer {

    private final RabbitTemplate rabbitTemplate;

    public boolean send(String json) {
        try {
            rabbitTemplate.convertAndSend(RabbitConfig.REPORT_EXCHANGE, RabbitConfig.ROUTING_SAVE, json);
            return true;
        } catch (Exception e) {
            log.error("[MQ] 上报消息发送失败", e);
            return false;
        }
    }
}