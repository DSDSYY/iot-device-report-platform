package com.example.iot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设备实体
 */
@Data
@TableName("device")
public class Device {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 设备业务号（对外唯一） */
    private String deviceNo;

    private String deviceName;

    /** 上报接口凭证 */
    private String apiKey;

    /** 0-离线 1-在线 */
    private Integer status;

    /** 最近一次上报时间（冗余字段，避免查最新扫明细表） */
    private LocalDateTime lastReportTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}