package com.example.iot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 设备上报明细实体（一条消息一行）
 */
@Data
@TableName("device_report")
public class DeviceReport implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String deviceNo;

    /** 设备消息幂等号 */
    private String msgId;

    /** 设备采集/上报时间（业务时间） */
    private LocalDateTime reportTime;

    /** 上报指标 JSON 字符串 */
    private String metricsJson;

    private LocalDateTime createTime;
}