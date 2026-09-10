package com.example.iot.controller;

import com.example.iot.common.api.Result;
import com.example.iot.dto.DeviceRegisterRequest;
import com.example.iot.dto.ReportBatchRequest;
import com.example.iot.service.DeviceService;
import com.example.iot.service.QueryService;
import com.example.iot.service.ReportService;
import com.example.iot.vo.DeviceRegistered;
import com.example.iot.vo.LatestReport;
import com.example.iot.vo.PageResult;
import com.example.iot.vo.ReportBatchResult;
import com.example.iot.vo.ReportView;
import com.example.iot.vo.StatPoint;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 设备接入与查询接口
 *
 * <pre>
 *   POST /api/device/register                  设备注册，返回 apiKey
 *   POST /api/device/report                    批量上报（Header 带设备凭证）
 *   GET  /api/device/{deviceNo}/latest         最新状态（缓存优先）
 *   GET  /api/device/{deviceNo}/reports        历史轨迹（分页）
 *   GET  /api/device/{deviceNo}/stats          上报量统计（hour/day）
 * </pre>
 */
@RestController
@RequestMapping("/api/device")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;
    private final ReportService reportService;
    private final QueryService queryService;

    @PostMapping("/register")
    public Result<DeviceRegistered> register(@Valid @RequestBody DeviceRegisterRequest request) {
        return Result.ok(deviceService.register(request));
    }

    @PostMapping("/report")
    public Result<ReportBatchResult> report(
            @RequestHeader("X-Device-No") String deviceNo,
            @RequestHeader("X-Api-Key") String apiKey,
            @Valid @RequestBody ReportBatchRequest request) {
        return Result.ok("上报受理成功（异步落库）", reportService.reportBatch(deviceNo, apiKey, request));
    }

    @GetMapping("/{deviceNo}/latest")
    public Result<LatestReport> latest(@PathVariable String deviceNo) {
        return Result.ok(queryService.latest(deviceNo));
    }

    @GetMapping("/{deviceNo}/reports")
    public Result<PageResult<ReportView>> reports(
            @PathVariable String deviceNo,
            @RequestParam(required = false) Long start,
            @RequestParam(required = false) Long end,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(queryService.history(deviceNo, start, end, page, size));
    }

    @GetMapping("/{deviceNo}/stats")
    public Result<List<StatPoint>> stats(
            @PathVariable String deviceNo,
            @RequestParam(defaultValue = "hour") String granularity,
            @RequestParam(required = false) Long start,
            @RequestParam(required = false) Long end) {
        return Result.ok(queryService.stats(deviceNo, granularity, start, end));
    }
}