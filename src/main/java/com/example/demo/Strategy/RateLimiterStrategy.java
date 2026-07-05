package com.example.demo.Strategy;

public interface RateLimiterStrategy {
    boolean allowRequest(String userId);
}
