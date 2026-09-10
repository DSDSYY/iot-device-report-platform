package com.example.iot.common.api;

import lombok.Getter;

/**
 * 业务状态码
 */
@Getter
public enum ResultCode {

    SUCCESS(200, "成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "设备鉴权失败"),
    TOO_MANY_REQUESTS(429, "上报过于频繁，请稍后重试"),
    INTERNAL_ERROR(500, "系统繁忙，请稍后重试"),

    DEVICE_NOT_FOUND(2001, "设备不存在或已注销"),
    BATCH_TOO_LARGE(2002, "单次批量上报超过上限"),
    QUEUE_BUSY(2003, "消息队列繁忙，请稍后重试");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}