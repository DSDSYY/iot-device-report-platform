package com.example.iot.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置：上报落库队列 + 死信队列
 *
 * <p><b>为什么用 MQ 而不是同步写库：</b>
 * <ul>
 *   <li>设备上报存在明显"潮汐"特征（整点/批量上线时突发），MQ 把高峰请求先缓冲，
 *       消费端按可控速率批量落库，实现削峰填谷，避免写库压力直接打到 MySQL；
 *   <li>消息持久化 + 消费失败重试（3 次）后进死信队列，失败可追踪、可补偿；
 *   <li>消费端可水平扩容（增加 @RabbitListener 并发/实例），写入能力随机器线性扩展。
 * </ul>
 *
 * <p><b>消息流转：</b>
 * <pre>
 *  上报接口(批量, Redis去重后)
 *    └─► iot.report.save.queue ──(消费成功)──► 批量 INSERT IGNORE + 更新设备最近上报时间
 *            └─ 失败重试 3 次仍失败 → 死信(DLX) → iot.report.fail.queue（人工/对账处理）
 * </pre>
 */
@Configuration
@EnableRabbit
public class RabbitConfig {

    public static final String REPORT_EXCHANGE = "iot.report.exchange";
    public static final String DLX_EXCHANGE = "iot.report.dlx.exchange";
    public static final String SAVE_QUEUE = "iot.report.save.queue";
    public static final String FAIL_QUEUE = "iot.report.fail.queue";
    public static final String ROUTING_SAVE = "iot.report.save";
    public static final String ROUTING_FAIL = "iot.report.fail";

    @Bean
    public DirectExchange reportExchange() {
        return new DirectExchange(REPORT_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange reportDlxExchange() {
        return new DirectExchange(DLX_EXCHANGE, true, false);
    }

    /** 上报落库队列：消费失败/重试耗尽 -> 死信到 fail 队列 */
    @Bean
    public Queue saveQueue() {
        return QueueBuilder.durable(SAVE_QUEUE)
                .deadLetterExchange(DLX_EXCHANGE)
                .deadLetterRoutingKey(ROUTING_FAIL)
                .build();
    }

    /** 死信队列：处理失败消息汇聚于此，供人工/对账处理 */
    @Bean
    public Queue failQueue() {
        return QueueBuilder.durable(FAIL_QUEUE).build();
    }

    @Bean
    public Binding saveBinding() {
        return BindingBuilder.bind(saveQueue()).to(reportExchange()).with(ROUTING_SAVE);
    }

    @Bean
    public Binding failBinding() {
        return BindingBuilder.bind(failQueue()).to(reportDlxExchange()).with(ROUTING_FAIL);
    }
}