package com.example.demo.Controller;

import com.example.demo.Strategy.RateLimiterStrategy;
import com.example.demo.Strategy.SlidingWindowStrategy;
import com.example.demo.Strategy.TokenBucketStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
public class RateLimiterController {

    @Autowired
    private Map<String, RateLimiterStrategy> strategies;

    @Autowired
    private TokenBucketStrategy tokenBucketStrategy;

    @GetMapping("/tokens")
    public ResponseEntity<Double> getTokens(@RequestParam String userId) {
        double tokens = tokenBucketStrategy.getCurrentTokens(userId);
        return ResponseEntity.ok(tokens);
    }

    @Autowired
    private SlidingWindowStrategy slidingWindowStrategy;

    @GetMapping("/window-count")
    public ResponseEntity<Long> getWindowCount(@RequestParam String userId) {
        return ResponseEntity.ok(slidingWindowStrategy.getCurrentCount(userId));
    }

    @GetMapping("/check")
    public ResponseEntity<String> check(
            @RequestParam String userId,
            @RequestHeader(value = "X-Algorithm", defaultValue = "tokenBucket") String algorithm
    ) {
        RateLimiterStrategy strategy = strategies.getOrDefault(algorithm, strategies.get("tokenBucket"));
        boolean allowed = strategy.allowRequest(userId);

        if (allowed) {
            return ResponseEntity.ok("✅ Allowed | Algorithm: " + algorithm);
        } else {
            return ResponseEntity
                    .status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("❌ Rate limit exceeded | Algorithm: " + algorithm);
        }
    }
}