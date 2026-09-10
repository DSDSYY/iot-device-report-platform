package com.example.iot.service.impl;

import com.example.iot.common.api.ResultCode;
import com.example.iot.common.exception.BusinessException;
import com.example.iot.common.util.RedisKey;
import com.example.iot.entity.DeviceReport;
import com.example.iot.mapper.DeviceReportMapper;
import com.example.iot.service.DeviceService;
import com.example.iot.service.QueryService;
import com.example.iot.vo.LatestReport;
import com.example.iot.vo.PageResult;
import com.example.iot.vo.ReportView;
import com.example.iot.vo.StatPoint;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 查询链路（读路径）
 * <ul>
 *   <li>最新状态：Redis Cache-Aside，未命中回源 DB 并回填缓存；</li>
 *   <li>历史轨迹：走 (device_no, report_time) 联合索引分页；</li>
 *   <li>上报量统计：DB 侧按小时/天 GROUP BY 聚合。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueryServiceImpl implements QueryService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final DeviceService deviceService;
    private final DeviceReportMapper reportMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${iot.latest-cache.ttl-seconds:60}")
    private long latestCacheTtlSeconds;

    @Override
    public LatestReport latest(String deviceNo) {
        deviceService.getByDeviceNo(deviceNo);

        // ① 读缓存
        String key = RedisKey.latest(deviceNo);
        String cached = redisTemplate.opsForValue().get(key);
        if (StringUtils.hasText(cached)) {
            try {
                LatestReport report = objectMapper.readValue(cached, LatestReport.class);
                report.setSource("cache");
                return report;
            } catch (Exception e) {
                // 缓存 JSON 损坏：忽略，走回源
                log.warn("[最新状态] 缓存解析失败，回源 DB deviceNo={}", deviceNo, e);
                redisTemplate.delete(key);
            }
        }

        // ② 回源 DB（取业务时间最新一条）
        DeviceReport last = reportMapper.selectLatestOne(deviceNo);
        LatestReport report = last == null
                ? new LatestReport(deviceNo, null, Collections.emptyMap(), "db")
                : new LatestReport(deviceNo, last.getReportTime(), parseMetrics(last.getMetricsJson()), "db");

        // ③ 回填缓存（Cache-Aside）
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(report),
                    Duration.ofSeconds(latestCacheTtlSeconds));
        } catch (Exception e) {
            log.warn("[最新状态] 缓存回填失败 deviceNo={}", deviceNo, e);
        }
        return report;
    }

    @Override
    public PageResult<ReportView> history(String deviceNo, Long startMs, Long endMs, int page, int size) {
        deviceService.getByDeviceNo(deviceNo);
        int p = Math.max(1, page);
        int s = Math.min(200, Math.max(1, size));
        LocalDateTime now = LocalDateTime.now(ZONE);
        LocalDateTime start = startMs == null ? now.minusDays(1) : toLocal(startMs);
        LocalDateTime end = endMs == null ? now : toLocal(endMs);

        long total = reportMapper.countByRange(deviceNo, start, end);
        List<DeviceReport> rows = reportMapper.selectPageByRange(deviceNo, start, end, (p - 1) * s, s);
        List<ReportView> records = rows.stream()
                .map(r -> new ReportView(r.getMsgId(), r.getReportTime(), parseMetrics(r.getMetricsJson())))
                .collect(Collectors.toList());
        return new PageResult<>(total, p, s, records);
    }

    @Override
    public List<StatPoint> stats(String deviceNo, String granularity, Long startMs, Long endMs) {
        deviceService.getByDeviceNo(deviceNo);
        LocalDateTime now = LocalDateTime.now(ZONE);
        LocalDateTime end = endMs == null ? now : toLocal(endMs);
        boolean byHour = !"day".equalsIgnoreCase(granularity);
        LocalDateTime start = startMs == null
                ? (byHour ? now.minusDays(1) : now.minusDays(7))
                : toLocal(startMs);
        return byHour
                ? reportMapper.countGroupByHour(deviceNo, start, end)
                : reportMapper.countGroupByDay(deviceNo, start, end);
    }

    private LocalDateTime toLocal(long epochMs) {
        return Instant.ofEpochMilli(epochMs).atZone(ZONE).toLocalDateTime();
    }

    private Map<String, Object> parseMetrics(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {
            });
        } catch (Exception e) {
            log.warn("[指标解析] JSON 解析失败: {}", json, e);
            return Collections.emptyMap();
        }
    }
}