-- =====================================================================
-- IoT 设备数据上报平台 数据库设计（MySQL 8.x / InnoDB / utf8mb4）
-- 说明：本脚本创建独立库 iot_platform + 专用账号 iot/iot123，需要 root 权限。
--       若只想临时借用已有库跑通演示（无 root），见 README「快速演示」章节。
-- =====================================================================

CREATE DATABASE IF NOT EXISTS `iot_platform`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_general_ci;

USE `iot_platform`;

-- ---------------------------------------------------------------------
-- 1. 设备表 device
--    设计要点：
--     ① device_no 是设备侧唯一业务号（出厂烧录/注册下发），对外接口都用它，不暴露自增主键；
--     ② api_key 是设备调用上报接口的凭证（演示用明文，生产应存加盐哈希并支持轮换）；
--     ③ status 冗余"在线/离线"状态位，供管理端列表过滤；
--     ④ last_report_time 冗余最近上报时间，避免"查最新"每次都扫明细大表。
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `device`;
CREATE TABLE `device` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '设备ID(主键)',
    `device_no`        VARCHAR(64)     NOT NULL                COMMENT '设备业务号(对外唯一)',
    `device_name`      VARCHAR(100)    NOT NULL DEFAULT ''     COMMENT '设备名称',
    `api_key`          VARCHAR(64)     NOT NULL                COMMENT '上报接口凭证(演示明文)',
    `status`           TINYINT         NOT NULL DEFAULT 0      COMMENT '状态: 0-离线 1-在线',
    `last_report_time` DATETIME        NULL                    COMMENT '最近一次上报时间(冗余)',
    `create_time`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`          TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除: 0-正常 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_device_no` (`device_no`),
    KEY `idx_status` (`status`),
    KEY `idx_last_report_time` (`last_report_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '设备表';

-- ---------------------------------------------------------------------
-- 2. 设备上报明细表 device_report（核心大表）
--    设计要点：
--     ① 只存"明细"，一条消息一行，指标以 JSON 形式存放（metrics_json），
--        兼容不同设备类型上报不同字段，避免为每种设备建表；
--     ② UNIQUE(device_no, msg_id)：msg_id 是设备侧自增/幂等号，
--        数据库唯一索引是"接口幂等"的最后一道防线（配合 Redis 前置去重）；
--     ③ KEY(device_no, report_time)：支撑"设备历史轨迹"范围查询；
--     ④ 生产建议：数据量上来后按 report_time 做 RANGE 分区（按月）或分表
--        device_report_YYYYMM，配合归档任务；本演示单表即可。
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `device_report`;
CREATE TABLE `device_report` (
    `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '记录ID(主键)',
    `device_no`    VARCHAR(64)     NOT NULL                COMMENT '设备业务号',
    `msg_id`       VARCHAR(64)     NOT NULL                COMMENT '设备消息幂等号',
    `report_time`  DATETIME        NOT NULL                COMMENT '设备采集/上报时间(业务时间)',
    `metrics_json` JSON            NULL                    COMMENT '上报指标(JSON,如温度/湿度/电压)',
    `create_time`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '落库时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_device_msg` (`device_no`, `msg_id`),
    KEY `idx_device_time` (`device_no`, `report_time`),
    KEY `idx_report_time` (`report_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '设备上报明细表';

-- ---------------- 演示设备（可重复执行） ----------------
INSERT IGNORE INTO `device` (`device_no`, `device_name`, `api_key`, `status`)
VALUES ('DEVICE-0001', '温湿度传感器-01', 'demo-key-0001', 0),
       ('DEVICE-0002', '温湿度传感器-02', 'demo-key-0002', 0),
       ('DEVICE-0003', '烟感探测器-01',   'demo-key-0003', 0);

-- ---------------- 专用账号 iot / iot123 ----------------
CREATE USER IF NOT EXISTS 'iot'@'localhost' IDENTIFIED BY 'iot123';
ALTER USER 'iot'@'localhost' IDENTIFIED BY 'iot123';
GRANT ALL PRIVILEGES ON `iot_platform`.* TO 'iot'@'localhost';
FLUSH PRIVILEGES;

SELECT 'iot_platform 初始化完成' AS msg;