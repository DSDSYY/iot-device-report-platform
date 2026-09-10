package com.example.iot.service.impl;

import com.example.iot.common.api.ResultCode;
import com.example.iot.common.exception.BusinessException;
import com.example.iot.common.util.RedisKey;
import com.example.iot.dto.ReportBatchRequest;
import com.example.iot.dto.ReportItem;
import com.example.iot.mq.ReportEnvelope;
import com.example.iot.mq.ReportMessageProducer;
import com.example.iot.service.DeviceService;
import com.example.iot.service.ReportService;
import com.example.iot.vo.ReportBatchResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 上报受理（写链路热路径）
 *
 * <pre>
 *  ① 设备鉴权（DB 查设备 + apiKey 比对）
 *  ② 单设备令牌桶限流（Redis Lua，拦截超速设备，保护下游）
 *  ③ Redis SETNX 幂等去重：(deviceNo, msgId) 已存在则丢弃，不重复进队
 *  ④ 组装批量消息投递 RabbitMQ（削峰），失败则回滚去重 Key（防止漏数据）
 *  ⑤ 真正的落库在 ReportSaveListener 消费端异步完成
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final DeviceService deviceService;
    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> rateLimitScript;
    private final ReportMessageProducer messageProducer;
    private final ObjectMapper objectMapper;

    @Value("${iot.report.batch-max:200}")
    private int batchMax;

    @Value("${iot.report.dedup-ttl-seconds:86400}")
    private long dedupTtlSeconds;

    @Value("${iot.rate-limit.capacity:100}")
    private long capacity;

    @Value("${iot.rate-limit.refill-per-second:100}")
    private long refillPerSecond;

    @Override
    public ReportBatchResult reportBatch(String deviceNo, String apiKey, ReportBatchRequest request) {
        // ① 鉴权
        deviceService.authenticate(deviceNo, apiKey);

        // ② 频控（单设备令牌桶）
        Long allowed = redisTemplate.execute(rateLimitScript,
                Collections.singletonList(RedisKey.rate(deviceNo)),
                String.valueOf(capacity),
                String.valueOf(refillPerSecond),
                String.valueOf(System.currentTimeMillis()));
        if (allowed == null || allowed != 1L) {
            throw new BusinessException(ResultCode.TOO_MANY_REQUESTS);
        }

        List<ReportItem> items = request.getReports();
        if (items.size() > batchMax) {
            throw new BusinessException(ResultCode.BATCH_TOO_LARGE);
        }

        // ③ Redis 幂等去重：SETNX 成功 = 首次上报
        List<ReportItem> fresh = new ArrayList<>();
        int duplicated = 0;
        for (ReportItem item : items) {
            Boolean first = redisTemplate.opsForValue().setIfAbsent(
                    RedisKey.dedup(deviceNo, item.getMsgId()),
                    "1",
                    Duration.ofSeconds(dedupTtlSeconds));
            if (Boolean.TRUE.equals(first)) {
                fresh.add(item);
            } else {
                duplicated++;
            }
        }
        if (fresh.isEmpty()) {
            return new ReportBatchResult(0, duplicated);
        }

        // ④ 投递 MQ 异步落库（削峰）
        try {
            String json = objectMapper.writeValueAsString(
                    new ReportEnvelope(deviceNo, fresh, System.currentTimeMillis()));
            if (!messageProducer.send(json)) {
                rollbackDedupKeys(deviceNo, fresh);
                throw new BusinessException(ResultCode.QUEUE_BUSY);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            rollbackDedupKeys(deviceNo, fresh);
            log.error("[上报] 消息序列化失败，已回滚去重 Key deviceNo={}", deviceNo, e);
            throw new BusinessException(ResultCode.QUEUE_BUSY);
        }

        log.info("[上报受理] deviceNo={}, accepted={}, duplicated={}", deviceNo, fresh.size(), duplicated);
        return new ReportBatchResult(fresh.size(), duplicated);
    }

    /**
     * 关键：MQ 发送失败时必须删除已写入的去重 Key，
     * 否则客户端重试同批 msgId 会被误判为"重复"而丢弃 -> 数据凭空丢失。
     */
    private void rollbackDedupKeys(String deviceNo, List<ReportItem> items) {
        for (ReportItem item : items) {
            redisTemplate.delete(RedisKey.dedup(deviceNo, item.getMsgId()));
        }
    }
}