# Distributed Rate Limiter

A production-style rate limiting microservice built with Spring Boot and Redis — supporting multiple pluggable algorithms and designed to work correctly across multiple running instances.

**🔗 Live demo:** https://distributed-rate-limiter-7m7f.onrender.com/dashboard.html
*(Free-tier hosting — first request may take 30-60s to wake up)*

---

## What this is

Every major API — Stripe, GitHub, Twitter — has a rate limiter deciding whether a request gets through or gets a `429 Too Many Requests`. This project implements that same core piece of infrastructure, built to work correctly in a **distributed environment** where multiple copies of the service run at once.

The interesting engineering problem this solves: if you run your app as 2+ instances (which any real production system does), where do you store "how many requests has this user made"? Storing it in memory means each instance has its own separate, wrong count. This project stores state in **Redis** instead — one shared source of truth — with an **atomic Lua script** to eliminate race conditions when multiple requests hit at the exact same moment.

---

<img width="1356" height="918" alt="image" src="https://github.com/user-attachments/assets/beed0ead-bd80-4740-b924-de68ecddf047" />


Both instances read and write the same Redis keys, so the rate limit is enforced consistently no matter which instance handles a given request — this is what makes it *distributed* rather than just *replicated*.

---

## Key design decisions

| Decision | Reasoning |
|---|---|
| **Redis over in-memory storage** | In-memory state doesn't survive across multiple instances — each would enforce its own separate limit, defeating the purpose. Redis gives one consistent view. |
| **Lua script for Token Bucket** | Without it, two simultaneous requests could both read "1 token left" and both get approved (a race condition). The Lua script makes read-check-decrement one atomic, uninterruptible operation. |
| **Strategy Pattern for algorithms** | Adding a new rate-limiting algorithm (e.g. Fixed Window) requires one new class — zero changes to existing code. Follows the Open/Closed Principle. |
| **Redis Sorted Sets for Sliding Window** | Timestamps as scores let you efficiently query "how many requests in the last N seconds" and evict old entries in one operation. |

---

## Algorithms implemented

**Token Bucket** — Each user has a bucket of tokens that refill at a steady rate. Every request consumes one token; an empty bucket means the request is blocked. Good for allowing short bursts while enforcing a steady average rate.

**Sliding Window Counter** — Tracks exact request timestamps in a rolling time window (e.g. last 60 seconds) using a Redis Sorted Set. More precise than fixed windows, avoids the "burst at window boundary" problem.

Switch between them per-request via an `X-Algorithm` header — no code changes, no redeploy.

---

## Tech stack

- **Java 17 + Spring Boot** — REST API
- **Redis** (Redis Cloud in production, Docker locally) — shared state store
- **Lua scripting** — atomic token bucket operations
- **Docker + Docker Compose** — local multi-instance environment
- **Strategy Pattern (LLD)** — pluggable algorithm architecture

---

## API Endpoints

| Endpoint | Description |
|---|---|
| `GET /check?userId={id}` | Checks if a request is allowed. Header `X-Algorithm: tokenBucket` or `slidingWindow` selects the strategy (default: tokenBucket). Returns `200 OK` or `429 Too Many Requests`. |
| `GET /tokens?userId={id}` | Returns the current real token count for a user (Token Bucket state, read directly from Redis). |
| `GET /window-count?userId={id}` | Returns the current request count within the sliding window for a user. |
| `GET /dashboard.html` | Live visual dashboard — fires real requests and displays real-time Redis state. |

---

## Running locally

**Prerequisites:** Java 17, Maven, Docker

```bash
# Build the jar
mvn clean package -DskipTests

# Start Redis + 2 app instances with Docker Compose
docker-compose up --build
```

Then open:
```
http://localhost:8080/dashboard.html
```

Hit "Send 5" or "Spam 15" and watch requests split across both instances while sharing one Redis-backed token count — proving the distributed behavior live.

---

## What the dashboard proves

- **Shared state** — the "Requests per instance" panel shows real traffic split across ports 8080/8081, both reading/writing the same Redis keys
- **Real enforcement** — the "Blocked 429" counter and request log reflect actual HTTP responses from the live API, not simulated data
- **Live algorithm switching** — the token/window stat panel polls the real Redis value for whichever algorithm is selected, updating in real time

---

## Possible extensions

- Per-API-key configurable limits (free tier vs premium tier)
- Fixed Window algorithm as a third strategy
- Kubernetes deployment with horizontal pod autoscaling
- Prometheus + Grafana metrics for request/block rates over time

---

## Author notes

Built to explore how rate limiting — a common system design interview topic — actually works under the hood, with a focus on the distributed-systems edge cases (race conditions, shared state, atomic operations) that a naive single-instance implementation would miss.
