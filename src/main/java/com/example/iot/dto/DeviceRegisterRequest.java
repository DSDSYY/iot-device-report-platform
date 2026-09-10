package com.example.iot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 设备注册/激活请求
 */
public record DeviceRegisterRequest(
        @NotBlank(message = "设备号不能为空")
        @Size(max = 64, message = "设备号过长")
        String deviceNo,

        @Size(max = 100, message = "设备名称过长")
        String deviceName
) {
}