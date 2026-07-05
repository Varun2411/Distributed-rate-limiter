-- KEYS[1] = tokens key, KEYS[2] = timestamp key
-- ARGV[1] = capacity, ARGV[2] = refill_rate, ARGV[3] = now (ms)

local tokens_key = KEYS[1]
local timestamp_key = KEYS[2]

local capacity = tonumber(ARGV[1])
local refill_rate = tonumber(ARGV[2])
local now = tonumber(ARGV[3])

local last_tokens = tonumber(redis.call("get", tokens_key))
if last_tokens == nil then
    last_tokens = capacity
end

local last_refreshed = tonumber(redis.call("get", timestamp_key))
if last_refreshed == nil then
    last_refreshed = now
end

local delta = math.max(0, now - last_refreshed)
local filled_tokens = math.min(capacity, last_tokens + (delta * refill_rate / 1000))

local allowed = filled_tokens >= 1
local new_tokens = filled_tokens
if allowed then
    new_tokens = filled_tokens - 1
end

redis.call("set", tokens_key, new_tokens)
redis.call("set", timestamp_key, now)

if allowed then
    return 1
else
    return 0
end