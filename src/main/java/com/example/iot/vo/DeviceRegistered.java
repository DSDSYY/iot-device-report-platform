package com.example.iot.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 设备注册结果：返回 deviceNo + apiKey（设备侧保存，调用上报接口时使用）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceRegistered {

    private String deviceNo;

    private String apiKey;
}