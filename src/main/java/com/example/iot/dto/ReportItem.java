package com.example.iot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * 单条上报数据
 *
 * <p>msgId 为设备侧生成的幂等号（如同一采集周期内的序号），
 * 服务端用 (deviceNo, msgId) 做去重，保证"断点续传/重试"不产生脏数据。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "msgId 不能为空")
    @Size(max = 64, message = "msgId 过长")
    private String msgId;

    /** 设备采集时间（epoch 毫秒） */
    @NotNull(message = "reportTime 不能为空")
    private Long reportTime;

    /** 指标集合：不同设备类型上报不同字段，如 temperature/humidity/voltage */
    private Map<String, Object> metrics;
}