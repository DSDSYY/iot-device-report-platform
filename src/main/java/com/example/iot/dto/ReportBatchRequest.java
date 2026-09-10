package com.example.iot.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 批量上报请求体
 */
@Data
public class ReportBatchRequest {

    @Valid
    @NotEmpty(message = "上报数据不能为空")
    @Size(max = 200, message = "单次批量上报不能超过 200 条")
    private List<ReportItem> reports;
}