-- KEYS[1] processed:payment:<paymentId>
-- KEYS[2] customer:<customerId>
-- ARGV[1] total amount in cents
-- ARGV[2] TTL marker in seconds
-- return  1 = processed, 0 = duplicate
if redis.call('SET', KEYS[1], '1', 'NX', 'EX', ARGV[2]) then
    redis.call('HINCRBY', KEYS[2], 'ordersCount', 1)
    redis.call('HINCRBY', KEYS[2], 'totalSpentCents', ARGV[1])
    return 1
end
return 0