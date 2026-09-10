package com.example.iot;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * IoT 设备数据上报平台 - 启动类
 *
 * <p>面向物联网设备的高并发数据接入：设备鉴权 -> 频控 -> Redis 幂等去重 ->
 * RabbitMQ 异步削峰 -> 批量落库 -> 最新状态缓存/历史查询/上报量统计。
 */
@SpringBootApplication
@MapperScan("com.example.iot.mapper")
public class IotApplication {

    public static void main(String[] args) {
        SpringApplication.run(IotApplication.class, args);
    }
}