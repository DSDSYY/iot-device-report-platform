package com.example.iot.service;

import com.example.iot.vo.LatestReport;
import com.example.iot.vo.PageResult;
import com.example.iot.vo.ReportView;
import com.example.iot.vo.StatPoint;

import java.util.List;

public interface QueryService {

    /** 设备最新状态（Redis Cache-Aside，未命中回源 DB） */
    LatestReport latest(String deviceNo);

    /** 设备历史上报轨迹（分页，时间范围过滤） */
    PageResult<ReportView> history(String deviceNo, Long startMs, Long endMs, int page, int size);

    /** 上报量统计（hour/day 粒度） */
    List<StatPoint> stats(String deviceNo, String granularity, Long startMs, Long endMs);
}