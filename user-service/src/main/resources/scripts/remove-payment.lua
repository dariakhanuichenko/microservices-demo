-- KEYS[1] processed:refund:<paymentId>
-- KEYS[2] customer:<customerId>
-- ARGV[1] сума в копійках (додатна)
-- ARGV[2] TTL маркера в секундах
-- return  1 = враховано, 0 = дубль, -1 = аномалія (нічого не змінено)

if redis.call('EXISTS', KEYS[1]) == 1 then
    return 0
end

local cents = tonumber(ARGV[1])
local count = tonumber(redis.call('HGET', KEYS[2], 'ordersCount') or 0)
local spent = tonumber(redis.call('HGET', KEYS[2], 'totalSpentCents') or 0)

if count < 1 or spent < cents then
    return -1
end

redis.call('SET', KEYS[1], '1', 'EX', ARGV[2])
redis.call('HINCRBY', KEYS[2], 'ordersCount', -1)
redis.call('HINCRBY', KEYS[2], 'totalSpentCents', -cents)
return 1