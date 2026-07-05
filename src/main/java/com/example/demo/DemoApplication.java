package com.example.demo;

import com.example.demo.Strategy.RateLimiterStrategy;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.Map;

@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	@Bean
	public Map<String, RateLimiterStrategy> strategies(
			RateLimiterStrategy tokenBucket,
			RateLimiterStrategy slidingWindow
	) {
		return Map.of(
				"tokenBucket", tokenBucket,
				"slidingWindow", slidingWindow
		);
	}
}