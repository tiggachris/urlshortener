# AWS Deployment Guide: High-Performance URL Shortener

This document details two battle-tested deployment paths on Amazon Web Services (AWS) for this project.

---

## Strategy A: Single-Instance EC2 with Docker Compose (Most Cost-Effective & Simple)
> **AWS Free Tier Eligible**: Uses an `EC2 t3.micro / t2.micro` instance running Docker and Docker Compose.

### Step 1: Launch EC2 Instance
1. In the AWS Management Console, navigate to **EC2** $\rightarrow$ **Launch Instance**.
2. **Name**: `urlshortener-prod`
3. **AMI**: Ubuntu 24.04 LTS (HVM) or Amazon Linux 2023.
4. **Instance Type**: `t3.micro` or `t3.small` (1 vCPU, 2GB RAM is ideal for Spring Boot + Postgres + Redis).
5. **Key Pair**: Create or choose an existing `.pem` key pair for SSH access.
6. **Network & Security Group**:
   - Inbound Rule 1: SSH (Port 22) from your IP.
   - Inbound Rule 2: HTTP (Port 80) from Anywhere (`0.0.0.0/0`).
   - Inbound Rule 3: HTTPS (Port 443) from Anywhere (`0.0.0.0/0`).
   - Inbound Rule 4 (Dev only): Custom TCP (Port 8080) and (Port 5173).

### Step 2: Connect and Install Docker
SSH into your instance:
```bash
ssh -i your-key.pem ubuntu@<EC2-PUBLIC-IP>
```

Update system packages and install Docker:
```bash
sudo apt-get update -y
sudo apt-get install -y docker.io docker-compose git
sudo systemctl enable --now docker
sudo usermod -aG docker ubuntu
newgrp docker
```

### Step 3: Clone Code and Start Containers
```bash
git clone https://github.com/your-username/urlshortener.git
cd urlshortener

# Launch PostgreSQL, Redis, and Spring Boot Backend
docker-compose up -d --build
```

### Step 4: Verify Deployment
```bash
# Check container health
docker ps

# Test the backend health endpoint
curl http://localhost:8080/actuator/health
# Response: {"status":"UP"}
```

---

## Strategy B: Enterprise Managed Architecture (High Availability)
> **Production Grade**: Decoupled managed database, managed cache, and autoscaling application tier.

```text
[ Route 53 (DNS) ]
        │
[ Application Load Balancer (ALB) - HTTPS :443 ]
        │
[ AWS Elastic Beanstalk / ECS Fargate (Spring Boot App) ]
   ├── Writes & Reads ──► [ Amazon RDS (PostgreSQL Multi-AZ) ]
   └── Sub-ms Caching ──► [ Amazon ElastiCache (Redis Cluster) ]
```

### 1. Amazon RDS (PostgreSQL)
1. Navigate to **RDS** $\rightarrow$ **Create Database**.
2. Choose **PostgreSQL 16**.
3. Template: Free Tier (`db.t3.micro` or `db.t4g.micro`).
4. DB Name: `urlshortener`, Username: `postgres`, Password: `[SecurePassword]`.
5. Attach to the same VPC and security group allowing inbound port `5432` from Elastic Beanstalk.

### 2. Amazon ElastiCache (Redis)
1. Navigate to **ElastiCache** $\rightarrow$ **Redis clusters** $\rightarrow$ **Create**.
2. Node type: `cache.t3.micro` (or `cache.t4g.micro`).
3. Set cluster mode to enabled or single-node depending on budget.
4. Security Group: Allow port `6379` inbound from application security group.

### 3. AWS Elastic Beanstalk
1. Navigate to **Elastic Beanstalk** $\rightarrow$ **Create Application**.
2. Platform: **Java** (Corretto 21).
3. Configuration $\rightarrow$ **Software** $\rightarrow$ Environment properties:
   - `DB_HOST`: `<RDS_ENDPOINT>`
   - `DB_PORT`: `5432`
   - `DB_NAME`: `urlshortener`
   - `DB_USER`: `postgres`
   - `DB_PASSWORD`: `[SecurePassword]`
   - `REDIS_HOST`: `<ELASTICACHE_ENDPOINT>`
   - `REDIS_PORT`: `6379`
   - `APP_BASE_URL`: `https://yourdomain.com`
4. Upload `backend/target/urlshortener-0.0.1-SNAPSHOT.jar` and deploy!

---

## Summary of Resume Metrics to Highlight
* **Deployment**: Configured automated multi-stage Docker build deployed on AWS EC2 with continuous zero-downtime container updates.
* **Resilience**: Configured PostgreSQL persistent volume storage and Redis append-only persistence (AOF) with automated healthcheck failover.
