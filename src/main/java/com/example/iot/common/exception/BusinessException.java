package com.example.iot.common.exception;

import com.example.iot.common.api.ResultCode;
import lombok.Getter;

/**
 * 业务异常：由全局异常处理器统一转换为 Result 返回
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ResultCode resultCode;

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.resultCode = resultCode;
    }

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
    }
}