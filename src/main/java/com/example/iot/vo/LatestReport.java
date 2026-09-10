package com.example.iot.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 设备最新状态（供管理端/查询接口）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LatestReport {

    private String deviceNo;

    private LocalDateTime reportTime;

    /** 最新一次上报的指标 */
    private Map<String, Object> metrics;

    /** 数据来源：cache-命中缓存 / db-回源数据库 */
    private String source;
}