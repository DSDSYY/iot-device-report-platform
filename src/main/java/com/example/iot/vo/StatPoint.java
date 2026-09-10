package com.example.iot.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 上报量统计点（按小时/按天聚合）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatPoint {

    private String bucket;

    private Long count;
}