package com.example.iot.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 批量上报处理结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportBatchResult {

    /** 本次新接收、进入异步队列的条数 */
    private int accepted;

    /** 命中幂等去重、被忽略的条数 */
    private int duplicated;
}