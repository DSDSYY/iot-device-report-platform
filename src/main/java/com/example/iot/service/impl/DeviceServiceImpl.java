package com.example.iot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.iot.common.api.ResultCode;
import com.example.iot.common.exception.BusinessException;
import com.example.iot.dto.DeviceRegisterRequest;
import com.example.iot.entity.Device;
import com.example.iot.mapper.DeviceMapper;
import com.example.iot.service.DeviceService;
import com.example.iot.vo.DeviceRegistered;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {

    private final DeviceMapper deviceMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public DeviceRegistered register(DeviceRegisterRequest request) {
        String deviceNo = request.deviceNo();
        Device existing = deviceMapper.selectOne(new LambdaQueryWrapper<Device>()
                .eq(Device::getDeviceNo, deviceNo));
        if (existing != null) {
            // 幂等：重复注册直接返回已有凭证
            return new DeviceRegistered(existing.getDeviceNo(), existing.getApiKey());
        }

        Device device = new Device();
        device.setDeviceNo(deviceNo);
        device.setDeviceName(StringUtils.hasText(request.deviceName()) ? request.deviceName() : deviceNo);
        device.setApiKey(generateApiKey());
        device.setStatus(0);
        deviceMapper.insert(device);
        log.info("[设备注册] deviceNo={}, apiKey={}", deviceNo, device.getApiKey());
        return new DeviceRegistered(device.getDeviceNo(), device.getApiKey());
    }

    @Override
    public Device getByDeviceNo(String deviceNo) {
        Device device = deviceMapper.selectOne(new LambdaQueryWrapper<Device>()
                .eq(Device::getDeviceNo, deviceNo));
        if (device == null) {
            throw new BusinessException(ResultCode.DEVICE_NOT_FOUND);
        }
        return device;
    }

    @Override
    public void authenticate(String deviceNo, String apiKey) {
        Device device = getByDeviceNo(deviceNo);
        if (!device.getApiKey().equals(apiKey)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "设备凭证错误");
        }
    }

    private String generateApiKey() {
        byte[] bytes = new byte[16];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}