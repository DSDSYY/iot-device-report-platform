package com.example.iot.service;

import com.example.iot.dto.DeviceRegisterRequest;
import com.example.iot.entity.Device;
import com.example.iot.vo.DeviceRegistered;

public interface DeviceService {

    /**
     * 设备注册/激活：不存在则创建并下发 apiKey，已存在则幂等返回
     */
    DeviceRegistered register(DeviceRegisterRequest request);

    /**
     * 按设备号查询（不存在抛 DEVICE_NOT_FOUND）
     */
    Device getByDeviceNo(String deviceNo);

    /**
     * 上报鉴权：校验设备号 + apiKey
     */
    void authenticate(String deviceNo, String apiKey);
}