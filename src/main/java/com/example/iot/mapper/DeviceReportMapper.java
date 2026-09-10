package com.example.iot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.iot.entity.DeviceReport;
import com.example.iot.vo.StatPoint;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface DeviceReportMapper extends BaseMapper<DeviceReport> {

    /**
     * 批量 INSERT IGNORE：数据库唯一索引(device_no,msg_id)兜底幂等，
     * 重复消息被忽略不报错（返回受影响行数，被忽略的不计数）
     */
    int insertIgnoreBatch(@Param("list") List<DeviceReport> list);

    /** 历史轨迹分页查询（走 idx_device_time 联合索引） */
    List<DeviceReport> selectPageByRange(@Param("deviceNo") String deviceNo,
                                         @Param("start") LocalDateTime start,
                                         @Param("end") LocalDateTime end,
                                         @Param("offset") int offset,
                                         @Param("size") int size);

    long countByRange(@Param("deviceNo") String deviceNo,
                      @Param("start") LocalDateTime start,
                      @Param("end") LocalDateTime end);

    /** 按小时统计上报量 */
    List<StatPoint> countGroupByHour(@Param("deviceNo") String deviceNo,
                                     @Param("start") LocalDateTime start,
                                     @Param("end") LocalDateTime end);

    /** 按天统计上报量 */
    List<StatPoint> countGroupByDay(@Param("deviceNo") String deviceNo,
                                    @Param("start") LocalDateTime start,
                                    @Param("end") LocalDateTime end);

    /** 取设备最新一条上报（Cache-Aside 回源时用） */
    DeviceReport selectLatestOne(@Param("deviceNo") String deviceNo);
}