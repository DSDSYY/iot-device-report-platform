# IoT 设备数据上报平台（后端）

> 校招简历项目 · 面向物联网设备的高并发数据接入与查询平台
> 技术栈：Spring Boot 3.3.5 / MyBatis-Plus / MySQL 8 / Redis / RabbitMQ / Java 17

## 一、项目简介

模拟海量物联网设备（温湿度传感器等）持续上报数据的场景，解决三个核心问题：

1. **高并发接入**：设备批量上报先经 Redis 频控 + 幂等去重，再投递 RabbitMQ 异步落库，实现**削峰填谷**，接口只负责"快速受理"，不阻塞写库；
2. **数据不重不漏**：`(device_no, msg_id)` 唯一索引 + Redis 前置去重，双层幂等；MQ 发送失败自动回滚去重 Key，防止数据丢失；
3. **查询性能**：最新状态走 Redis Cache-Aside；历史轨迹走 `(device_no, report_time)` 联合索引分页；上报量按时/日聚合。

## 二、核心流程

```
设备批量上报
  ├─ ① 鉴权（device_no + api_key，查 device 表）
  ├─ ② 单设备令牌桶限流（Redis Lua，原子执行）
  ├─ ③ Redis SETNX 幂等去重（msgId 已存在则丢弃）
  ├─ ④ 投递 RabbitMQ（失败 → 回滚去重 Key，防漏数据）
  └─ 消费端异步落库
       ├─ 批量 INSERT IGNORE（唯一索引兜底幂等）
       ├─ 更新 device.last_report_time（IF 条件推进，防乱序回退）
       └─ 刷新 Redis 最新状态缓存（只允许时间前进）
```

查询链路：
```
最新状态：Redis Cache-Aside（未命中回源 DB 并回填）
历史轨迹：DB 分页（(device_no, report_time) 索引）
上报统计：DB GROUP BY 按时/日聚合
```

## 三、目录结构

```
iot-device-report-platform
├── pom.xml
├── sql/iot_platform_schema.sql      # 建库建表 + 演示设备 + 专用账号(需 root 执行一次)
├── scripts/
│   ├── start-redis.cmd              # 启动本地 Redis
│   ├── start-rabbitmq.cmd           # 启动本地 RabbitMQ
│   ├── start-backend.cmd            # 启动后端(spring-boot:run)
│   └── demo-test.ps1                # 一键演示脚本(注册/上报/查询/去重验证)
└── src/main/
    ├── java/com/example/iot/
    │   ├── controller/DeviceController.java   # 对外 REST 接口
    │   ├── service/                            # 设备/上报受理/查询
    │   ├── mq/                                 # RabbitMQ 生产者 + 落库消费者
    │   ├── mapper/                             # MyBatis-Plus Mapper
    │   └── config/                             # RabbitMQ 交换机队列 / Lua 脚本
    └── resources/
        ├── application.yml
        ├── lua/iot_rate_limit.lua              # 单设备令牌桶限流
        └── mapper/*.xml                        # 批量 INSERT IGNORE / 分页 / 聚合 SQL
```

## 四、快速开始

前置：JDK 17、Maven 3.9+、MySQL 8、Redis 5+、RabbitMQ 3.x（本机均已具备，见 scripts）。

```powershell
# 1) 初始化数据库（只需一次；需 root 密码，自动建 iot_platform 库 + iot/iot123 账号 + 3 台演示设备）
mysql -uroot -p < sql\iot_platform_schema.sql
# 或: mysql -uroot -p -e "SOURCE D:/CodexProjects/iot-device-report-platform/sql/iot_platform_schema.sql"

# 2) 启动中间件
scripts\start-redis.cmd
scripts\start-rabbitmq.cmd

# 3) 启动后端（默认 8090）
scripts\start-backend.cmd
# 看到 "Tomcat started on port 8090" 即成功

# 4) 一键演示（注册新设备 → 批量上报30条 → 查询最新/历史/统计 → 重复上报验证幂等）
powershell -ExecutionPolicy Bypass -File scripts\demo-test.ps1
```

> 演示设备已预置：`DEVICE-0001 / demo-key-0001`，`DEVICE-0002 / demo-key-0002`，`DEVICE-0003 / demo-key-0003`。

## 五、接口文档

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | /api/device/register | 设备注册，返回 apiKey（重复注册幂等） |
| POST | /api/device/report | 批量上报，Header: `X-Device-No` + `X-Api-Key` |
| GET | /api/device/{deviceNo}/latest | 设备最新状态（缓存优先，`source=cache/db`） |
| GET | /api/device/{deviceNo}/reports | 历史轨迹分页（start/end 为 epoch 毫秒） |
| GET | /api/device/{deviceNo}/stats | 上报量统计（granularity=hour/day） |

示例：

```bash
# 批量上报
curl -X POST http://127.0.0.1:8090/api/device/report \
  -H "X-Device-No: DEVICE-0001" -H "X-Api-Key: demo-key-0001" \
  -H "Content-Type: application/json" \
  -d '{"reports":[{"msgId":"demo-001","reportTime":1700000000000,"metrics":{"temperature":25.5,"humidity":60}}]}'
# 返回: {"code":200,"data":{"accepted":1,"duplicated":0}}
```

## 六、数据库设计要点（面试必讲）

| 设计 | 理由 |
|---|---|
| device 表冗余 `last_report_time`、`status` | "查最新/在线列表"不用扫上报明细大表 |
| device_report 只存明细，指标用 JSON | 兼容不同设备类型字段差异，避免一设备一表 |
| `UNIQUE(device_no, msg_id)` | 数据库层幂等兜底，配合 Redis 前置去重 |
| `KEY(device_no, report_time)` | 支撑"设备历史轨迹"范围查询 |
| 生产演进 | 数据量大后按 report_time 做 RANGE 分区/分表 + 归档；实时看板可用 ClickHouse/TDengine 等时序库 |

## 七、并发与一致性设计（面试必讲）

1. **削峰**：写路径三步全部"轻"——鉴权读单行、Redis 频控+去重、MQ 投递；真正的批量 INSERT 在消费端异步做，接口吞吐不受 DB 写放大影响。
2. **防超速**：单设备令牌桶（Redis Lua 原子执行），容忍短时突发、长期限速，比固定窗口公平。
3. **防重复**：Redis SETNX（TTL 24h）前置拦截 + DB 唯一索引兜底；**MQ 发送失败必须回滚去重 Key**，否则客户端重试会被误判重复而丢数据（本项目已处理）。
4. **防回退**：异步批次可能乱序到达，`last_report_time` 和"最新状态缓存"都**只允许时间前进**（DB 用 `IF` 条件更新，缓存先比较再覆盖）。
5. **失败可追踪**：消费失败自动重试 3 次，仍失败进死信队列，供人工/对账处理。

## 八、本地实测数据（诚实标注：开发机 + 模拟数据）

| 指标 | 数值 | 说明 |
|---|---|---|
| 批量上报接口 | 30 条/请求全受理 | 单次响应 <50ms |
| 幂等 | 同批重发 accepted=0, duplicated=30 | DB 行数不增 |
| 并发吞吐（36 并发） | ≈412 req/s、≈1.2 万条/s 受理 | 接口受理层（削峰后异步落库） |
| 数据一致性 | 受理 1110 条 → DB 1110 行 | 无丢失、无重复 |
| 时间防回退 | 旧批次不覆盖新批次 | 回归用例通过 |

> 说明：数值为本地开发环境实测，非生产压测；如需简历量化建议用 JMeter 在更高配置机器上重新压测后填写。

## 九、已知边界（面试主动说 = 加分）

- 设备鉴权用 apiKey 明文比对，生产应存加盐哈希、支持轮换与吊销；
- MQ 投递为"尽力投递"，生产应开启 publisher-confirm + 本地消息表对账；
- Redis 去重 TTL 内极端并发下可能先放行后由 DB 唯一索引兜底（已保证不重复）；
- 单库单表演示，未做分库分表与冷热分离（已在演进方案中说明）。