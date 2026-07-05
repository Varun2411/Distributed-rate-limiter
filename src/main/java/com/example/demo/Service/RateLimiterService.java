package com.example.demo.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class RateLimiterService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private final DefaultRedisScript<Long> script;

    private static final long CAPACITY = 10;       // max tokens
    private static final long REFILL_RATE = 0;      // tokens added per second

    public RateLimiterService() {
        script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("token_bucket.lua"));
        script.setResultType(Long.class);
    }

    public boolean allowRequest(String userId) {
        String tokensKey = "tokens:" + userId;
        String timestampKey = "lastRefill:" + userId;

        List<String> keys = Arrays.asList(tokensKey, timestampKey);
        long now = System.currentTimeMillis();

        Long result = redisTemplate.execute(
                script,
                keys,
                String.valueOf(CAPACITY),
                String.valueOf(REFILL_RATE),
                String.valueOf(now)
        );

        return result != null && result == 1;
    }
}