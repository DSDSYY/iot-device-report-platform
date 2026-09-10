package com.example.iot.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 单条上报记录视图（对外返回，隐藏内部字段）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportView {

    private String msgId;

    private LocalDateTime reportTime;

    private Map<String, Object> metrics;
}