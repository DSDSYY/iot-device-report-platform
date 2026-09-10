package com.example.iot.service;

import com.example.iot.dto.ReportBatchRequest;
import com.example.iot.vo.ReportBatchResult;

public interface ReportService {

    /**
     * 受理设备批量上报：频控 -> Redis 幂等去重 -> 投递 MQ 异步落库
     */
    ReportBatchResult reportBatch(String deviceNo, String apiKey, ReportBatchRequest request);
}