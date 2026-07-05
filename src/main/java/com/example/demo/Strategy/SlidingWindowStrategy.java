package com.example.demo.Strategy;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component("slidingWindow")
public class SlidingWindowStrategy implements RateLimiterStrategy {

    private final StringRedisTemplate redisTemplate;

    private static final int MAX_REQUESTS = 10;
    private static final int WINDOW_SECONDS = 60;

    public SlidingWindowStrategy(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public long getCurrentCount(String userId) {
        String key = "sliding:" + userId;
        long now = System.currentTimeMillis();
        long windowStart = now - (WINDOW_SECONDS * 1000L);
        redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);
        Long count = redisTemplate.opsForZSet().zCard(key);
        return count == null ? 0 : count;
    }


    @Override
    public boolean allowRequest(String userId) {
        String key = "sliding:" + userId;
        long now = System.currentTimeMillis();
        long windowStart = now - (WINDOW_SECONDS * 1000L);

        // Remove requests outside the window
        redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);

        // Count requests in current window
        Long count = redisTemplate.opsForZSet().zCard(key);

        if (count == null || count < MAX_REQUESTS) {
            // Add current request timestamp
            redisTemplate.opsForZSet().add(key, String.valueOf(now), now);
            redisTemplate.expire(key, WINDOW_SECONDS, TimeUnit.SECONDS);
            return true;
        }

        return false;
    }
}