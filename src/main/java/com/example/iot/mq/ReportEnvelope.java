package com.example.iot.mq;

import com.example.iot.dto.ReportItem;

import java.util.List;

/**
 * 上报消息信封：一批消息整体投递，消费端批量落库
 *
 * @param deviceNo  设备号
 * @param reports   本批新增上报（已通过 Redis 去重）
 * @param receivedAt 服务端受理时间（epoch ms）
 */
public record ReportEnvelope(String deviceNo, List<ReportItem> reports, long receivedAt) {
}