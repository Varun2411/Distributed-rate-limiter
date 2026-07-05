package com.example.demo;

public class TokenBucket {
    private final long capacity;        // max tokens allowed
    private final long refillRate;      // tokens added per second
    private double currentTokens;
    private long lastRefillTime;

    public TokenBucket(long capacity, long refillRate) {
        this.capacity = capacity;
        this.refillRate = refillRate;
        this.currentTokens = capacity;
        this.lastRefillTime = System.currentTimeMillis();
    }

    private void refill() {
        long now = System.currentTimeMillis();
        double secondsElapsed = (now - lastRefillTime) / 1000.0;
        double tokensToAdd = secondsElapsed * refillRate;

        currentTokens = Math.min(capacity, currentTokens + tokensToAdd);
        lastRefillTime = now;
    }

    public synchronized boolean allowRequest() {
        refill();
        if (currentTokens >= 1) {
            currentTokens--;
            return true;   // request allowed
        }
        return false;      // request blocked
    }
}
