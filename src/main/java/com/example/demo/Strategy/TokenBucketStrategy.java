package com.example.demo.Strategy;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component("tokenBucket")
public class TokenBucketStrategy implements RateLimiterStrategy {

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> script;

    private static final long CAPACITY = 10;
    private static final long REFILL_RATE = 1;

    public TokenBucketStrategy(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("token_bucket.lua"));
        script.setResultType(Long.class);
    }

    @Override
    public boolean allowRequest(String userId) {
        List<String> keys = Arrays.asList("tokens:" + userId, "lastRefill:" + userId);
        Long result = redisTemplate.execute(
                script, keys,
                String.valueOf(CAPACITY),
                String.valueOf(REFILL_RATE),
                String.valueOf(System.currentTimeMillis())
        );
        return result != null && result == 1;
    }

    public double getCurrentTokens(String userId) {
        String tokensKey = "tokens:" + userId;
        String value = redisTemplate.opsForValue().get(tokensKey);
        if (value == null) {
            return CAPACITY; // no key yet means bucket is full
        }
        return Double.parseDouble(value);
    }
}