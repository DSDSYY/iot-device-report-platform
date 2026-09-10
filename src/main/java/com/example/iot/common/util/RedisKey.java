package com.example.iot.common.util;

/**
 * Redis Key 统一管理，避免散落魔法字符串
 */
public final class RedisKey {

    private RedisKey() {
    }

    /** 设备消息幂等去重 Key：SETNX 成功=首次上报 */
    public static String dedup(String deviceNo, String msgId) {
        return "iot:dedup:" + deviceNo + ":" + msgId;
    }

    /** 单设备限流 Key（令牌桶） */
    public static String rate(String deviceNo) {
        return "iot:rate:" + deviceNo;
    }

    /** 设备最新状态缓存 Key（Cache-Aside） */
    public static String latest(String deviceNo) {
        return "iot:latest:" + deviceNo;
    }
}