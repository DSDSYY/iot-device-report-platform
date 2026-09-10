-- ============================================================================
-- iot_rate_limit.lua  单设备令牌桶限流（Redis 原子执行）
-- 调用约定：
--   KEYS[1]  iot:rate:{deviceNo}      限流 Key
--   ARGV[1]  桶容量 capacity（突发上限）
--   ARGV[2]  每秒补充令牌数 refillRate（稳态速率）
--   ARGV[3]  当前毫秒时间戳 now（应用传入，避免多节点时钟不一致）
-- value 存储格式："剩余令牌数:上次补充时间戳"，例 "3.500:1725600000123"
-- 返回值：1 放行；0 限流
-- 为什么用令牌桶：
--   1) 允许设备短时突发批量上报，长期速率被 refillRate 平滑限制；
--   2) Lua 整体在 Redis 单线程事件循环内原子执行，并发安全、无竞态；
--   3) 相比固定窗口，边界更公平，不会出现整点瞬间打满。
-- ============================================================================

local key      = KEYS[1]
local capacity = tonumber(ARGV[1])
local rate     = tonumber(ARGV[2])
local now      = tonumber(ARGV[3])

if not capacity or not rate or not now or capacity <= 0 or rate <= 0 then
    return 0
end

local tokens = capacity
local last   = now

local val = redis.call('GET', key)
if val then
    local savedTokens, savedLast = string.match(val, "^(%-?%d+%.?%d*):(%d+)$")
    if savedTokens and savedLast then
        tokens = tonumber(savedTokens)
        last   = tonumber(savedLast)
        tokens = tokens + (now - last) / 1000 * rate
        if tokens > capacity then
            tokens = capacity
        end
    end
end

if tokens >= 1 then
    tokens = tokens - 1
    redis.call('SET', key, string.format('%.3f', tokens) .. ':' .. now, 'PX', 60000)
    return 1
end

redis.call('SET', key, string.format('%.3f', tokens) .. ':' .. last, 'PX', 60000)
return 0