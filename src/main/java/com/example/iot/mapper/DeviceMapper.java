package com.example.iot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.iot.entity.Device;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

public interface DeviceMapper extends BaseMapper<Device> {

    /**
     * 上报成功后更新设备最近上报时间并置为在线（冗余字段，避免查询扫明细大表）
     */
    int updateLastReport(@Param("deviceNo") String deviceNo, @Param("reportTime") LocalDateTime reportTime);
}