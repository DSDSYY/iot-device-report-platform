package com.example.iot.mq;

import com.example.iot.common.util.RedisKey;
import com.example.iot.config.RabbitConfig;
import com.example.iot.dto.ReportItem;
import com.example.iot.entity.DeviceReport;
import com.example.iot.mapper.DeviceMapper;
import com.example.iot.mapper.DeviceReportMapper;
import com.example.iot.vo.LatestReport;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * 上报落库消费者（异步写链路）
 *
 * <pre>
 *  ① 批量 INSERT IGNORE（数据库唯一索引兜底幂等，重复行静默忽略）
 *  ② 更新 device 冗余字段：最近上报时间 + 在线状态（避免"查最新"扫明细大表）
 *  ③ DB 提交后刷新 Redis 最新状态缓存（Cache-Aside 写路径）
 * 任一环节抛异常 -> Spring 重试 3 次 -> 仍失败进死信队列人工处理
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReportSaveListener {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final DeviceReportMapper reportMapper;
    private final DeviceMapper deviceMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${iot.latest-cache.ttl-seconds:60}")
    private long latestCacheTtlSeconds;

    @RabbitListener(queues = RabbitConfig.SAVE_QUEUE)
    public void onSave(String json) {
        try {
            ReportEnvelope envelope = objectMapper.readValue(json, ReportEnvelope.class);
            String deviceNo = envelope.deviceNo();

            // ① 组装明细并找出本批业务时间最新的一条
            LocalDateTime newestTime = null;
            ReportItem newestItem = null;
            List<DeviceReport> rows = new ArrayList<>(envelope.reports().size());
            for (ReportItem item : envelope.reports()) {
                LocalDateTime reportTime = Instant.ofEpochMilli(item.getReportTime())
                        .atZone(ZONE).toLocalDateTime();
                if (newestTime == null || reportTime.isAfter(newestTime)) {
                    newestTime = reportTime;
                    newestItem = item;
                }
                DeviceReport row = new DeviceReport();
                row.setDeviceNo(deviceNo);
                row.setMsgId(item.getMsgId());
                row.setReportTime(reportTime);
                row.setMetricsJson(item.getMetrics() == null ? null
                        : objectMapper.writeValueAsString(item.getMetrics()));
                rows.add(row);
            }

            // ② 批量落库（INSERT IGNORE 幂等兜底）
            int affected = reportMapper.insertIgnoreBatch(rows);

            // ③ 更新设备冗余字段
            if (newestTime != null) {
                deviceMapper.updateLastReport(deviceNo, newestTime);
            }

            // ④ 刷新最新状态缓存（同样只允许前进，防止乱序批次回退缓存）
            if (newestItem != null && newestTime != null) {
                if (isNewerThanCache(deviceNo, newestTime)) {
                    LatestReport latest = new LatestReport(deviceNo, newestTime, newestItem.getMetrics(), "db");
                    redisTemplate.opsForValue().set(RedisKey.latest(deviceNo),
                            objectMapper.writeValueAsString(latest),
                            Duration.ofSeconds(latestCacheTtlSeconds));
                }
            }

            log.info("[落库完成] deviceNo={}, 本批 {} 条, 实际写入 {} 行, 最新时间={}",
                    deviceNo, envelope.reports().size(), affected, newestTime);
        } catch (Exception e) {
            log.error("[落库失败] 消息将重试/进入死信: {}", json, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 判断该上报时间是否比缓存中的最新时间更新
     * （缓存缺失/解析失败视为更新，交给 DB 侧 IF 条件推进兜底）
     */
    private boolean isNewerThanCache(String deviceNo, LocalDateTime reportTime) {
        String cached = redisTemplate.opsForValue().get(RedisKey.latest(deviceNo));
        if (cached == null || cached.isBlank()) {
            return true;
        }
        try {
            LatestReport old = objectMapper.readValue(cached, LatestReport.class);
            return old.getReportTime() == null || reportTime.isAfter(old.getReportTime());
        } catch (Exception e) {
            log.warn("[最新缓存] 解析失败，按更新处理 deviceNo={}", deviceNo, e);
            return true;
        }
    }
}