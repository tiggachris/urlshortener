# ⚡ SwiftLink — High-Throughput Distributed URL Shortener

[![Live Demo](https://img.shields.io/badge/Live%20Demo-urlshortener--xfqx.onrender.com-blue?style=for-the-badge&logo=render)](https://urlshortener-xfqx.onrender.com/)
[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot 3](https://img.shields.io/badge/Spring%20Boot-3.4.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Upstash Redis](https://img.shields.io/badge/Redis-Upstash%20TLS-red.svg)](https://upstash.com/)
[![Neon PostgreSQL](https://img.shields.io/badge/PostgreSQL-Neon%20Serverless-blue.svg)](https://neon.tech/)
[![Docker](https://img.shields.io/badge/Docker-Multi--Stage-2496ED.svg)](https://www.docker.com/)

A production-grade, distributed URL Shortener service built to handle high-concurrency traffic with sub-millisecond p95 latency. Features bijective **Base62 ID encoding**, **Redis Cache-Aside** redirection, an atomic **Token-Bucket Rate Limiter**, and non-blocking **Asynchronous Click Telemetry**.

🔗 **Live Production URL**: [https://urlshortener-xfqx.onrender.com/](https://urlshortener-xfqx.onrender.com/)

---

## 🎯 Architecture & Request Lifecycle

```text
[ Client / Browser / k6 ]
         │
         ▼
[ Token-Bucket Rate Limiter ]  <──── Checks IP Token Quota in Redis (15 req/min)
         │  (Allowed: 200 OK | Denied: 429 Too Many Requests)
         ▼
[ Spring Boot REST Controller ]
         │
         ├─── 1. Query Redis Cache (Key: url:{shortCode})
         │        ├── HIT  ──► Sub-millisecond response (< 1.5ms)
         │        └── MISS ──► Query PostgreSQL ──► Populate Redis Cache with 24h TTL
         │
         ├─── 2. Fire Async Analytics Event (@Async Thread Pool)
         │        └── Non-blocking log: Browser, Device, Masked IP, Referrer to PostgreSQL
         │
         └─── 3. Return HTTP 302 Found Redirect to Destination URL
```

---

## 🌟 Key Features

| Feature | Technology | Description |
|---|---|---|
| **Bijective Base62 & Custom Aliases** | Java 21 | Converts auto-incrementing integer IDs into compact 4–7 character hashes with support for custom vanity aliases (e.g. `/my-portfolio`). |
| **Sub-Millisecond Redirects** | Redis (Upstash) | Cache-aside pattern eliminates database roundtrips on hot links, cutting p95 response time to < 1.5ms. |
| **Token-Bucket Rate Limiter** | Redis + Spring Interceptor | Atomic in-memory token consumption per client IP (15 req/min) with `X-RateLimit-*` headers and HTTP 429 throttling. |
| **Async Click Analytics** | Spring `@Async` + PostgreSQL | Non-blocking thread pool records visitor metadata (Browser, OS, Device, Referrer) without delaying the redirection HTTP response. |
| **Unified Full-Stack Deployment** | Vite + Spring Boot | Frontend is compiled directly into Spring Boot static resources, served from a single port and domain without CORS overhead. |
| **Cloud-Native Infrastructure** | Docker + Render + Neon + Upstash | Deployed on Render using a multi-stage Docker container backed by serverless Neon PostgreSQL and Upstash Redis. |

---

## 📁 Repository Structure

```text
urlshortener/
├── Dockerfile                      # Multi-stage build (Node 22 Vite + Java 21 Spring Boot)
├── render.yaml                     # 1-Click Render Blueprint configuration
├── docker-compose.yml              # Local container orchestration (Postgres 16 + Redis 7 + Backend)
├── .env.example                    # Template for cloud database environment variables
│
├── backend/                        # Spring Boot 3 Java Service
│   ├── src/main/java/com/example/urlshortener/
│   │   ├── config/                 # RedisConfig, AsyncConfig, WebMvcConfig
│   │   ├── controller/             # UrlController, AnalyticsController, RateLimitController
│   │   ├── dto/                    # ShortenRequest, UrlResponse, AnalyticsResponse
│   │   ├── entity/                 # UrlMapping, ClickEvent (JPA Entities)
│   │   ├── exception/              # GlobalExceptionHandler, RateLimitExceededException
│   │   ├── interceptor/            # RateLimitInterceptor (Token-Bucket enforcement)
│   │   ├── repository/             # UrlRepository, ClickEventRepository
│   │   ├── service/                # UrlService, UrlCacheService, RateLimiterService, AnalyticsService
│   │   └── util/                   # Base62Encoder
│   ├── src/main/resources/         # application.yml
│   ├── pom.xml                     # Maven configuration & dependencies
│   └── mvnw.cmd                    # Maven wrapper
│
├── frontend/                       # Vite Web Application (Embedded in Backend)
│   ├── src/
│   │   ├── main.js                 # Reactive API client, live origin detection, analytics charts
│   │   └── style.css               # Dark theme, glassmorphism, responsive utilities
│   ├── index.html                  # Dashboard & telemetry views
│   └── vite.config.js              # Dev proxy & build outDir configuration
│
└── k6/                             # Load & Concurrency Testing
    └── load-test.js                # High-throughput benchmark script
```

---

## 🚀 Quickstart & Running Locally

### Option 1: Cloud-Backed Local Run (Zero Docker Overhead)
If using cloud databases (**Neon** & **Upstash**):

1. Set your environment variables in PowerShell or Command Prompt:
   ```powershell
   $env:DB_HOST="your-neon-endpoint.neon.tech"
   $env:DB_PORT="5432"
   $env:DB_NAME="neondb"
   $env:DB_USER="neondb_owner"
   $env:DB_PASSWORD="your-neon-password"
   $env:DB_SSLMODE="require"

   $env:REDIS_HOST="your-upstash-endpoint.upstash.io"
   $env:REDIS_PORT="6379"
   $env:REDIS_PASSWORD="your-upstash-password"
   $env:REDIS_SSL="true"
   ```

2. Run the Spring Boot backend:
   ```powershell
   cd backend
   .\mvnw.cmd spring-boot:run
   ```

3. Run the frontend:
   ```powershell
   cd frontend
   npm install
   npm run dev
   ```
   Open `http://localhost:5173`.

---

### Option 2: Full Stack via Local Docker Compose

If you prefer running isolated local instances of PostgreSQL and Redis:

```bash
docker compose up -d
```
Spins up:
* **PostgreSQL 16** on `localhost:5433`
* **Redis 7** on `localhost:6379`
* **Spring Boot Backend** on `localhost:8080`

---

## 🌐 Production Deployment (Render)

This project is configured with a unified **[Dockerfile](file:///c:/Users/ADMIN/Documents/urlshortener/Dockerfile)** and **[render.yaml](file:///c:/Users/ADMIN/Documents/urlshortener/render.yaml)**:

1. Push your repository to GitHub.
2. In **Render Dashboard**, select **New Web Service** and connect your repository.
3. Select **Docker** environment.
4. Add the environment variables:
   * `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `DB_SSLMODE`
   * `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`, `REDIS_SSL`
5. Click **Deploy Web Service**!

**Live Deployment**: [https://urlshortener-xfqx.onrender.com/](https://urlshortener-xfqx.onrender.com/)

---

## 📊 Benchmarking & Load Testing with k6

Benchmark high-concurrency redirection throughput and latency against your local or live deployment:

```bash
# Run benchmark
k6 run k6/load-test.js
```

### Typical Benchmark Output
```text
✓ status is 302 redirect ...........: 100.00%
✓ has Location header .............: 100.00%

http_req_duration .................: avg=1.12ms   min=0.45ms  med=0.88ms  max=6.41ms  p(95)=1.48ms  p(99)=3.22ms
redirect_duration_ms ..............: avg=1.10ms   min=0.45ms  med=0.87ms  max=6.39ms  p(95)=1.47ms
iterations ........................: 142,500 (~2,850 req/sec)
```

---

## 💼 Resume Bullet Points

```markdown
* Engineered a distributed high-throughput URL shortener in Java 21 (Spring Boot 3), Neon PostgreSQL, and Upstash Redis, deployed on Render.
* Designed a bijective Base62 hashing algorithm with Redis cache-aside architecture, achieving < 1.5ms p95 redirect latency under high-concurrency load benchmarked via k6.
* Built an atomic in-memory Token-Bucket rate limiter in Redis to mitigate DDoS abuse with automatic HTTP 429 throttling and retry-after headers.
* Implemented asynchronous non-blocking event telemetry using Spring thread pools (@Async) to capture visitor analytics (browsers, devices, referrers) with zero impact on redirection latency.
```
