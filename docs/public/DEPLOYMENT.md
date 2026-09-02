# Deployment Guide

## Overview

Per-Cloud supports two deployment modes: Development (local) and Production (Docker Compose).

## Development Environment

### Prerequisites

- JDK 21+
- Node.js 18+
- Docker & Docker Compose
- Git

### Step 1: Clone Repository

```bash
git clone https://github.com/shinamimi/per-cloud.git
cd per-cloud
```

### Step 2: Start Infrastructure Services

```bash
docker compose up -d
```

This starts:
- **MySQL 8.4**: Port 3306
- **Redis 7.2**: Port 6379
- **MinIO**: Port 9000 (API), 9001 (Console)

### Step 3: Start Backend

```bash
cd backend
./mvnw spring-boot:run
```

Backend runs on `http://localhost:8080`

### Step 4: Start Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend runs on `http://localhost:5173`

### Step 5: Access Application

- **Frontend**: http://localhost:5173
- **Backend API**: http://localhost:8080
- **MinIO Console**: http://localhost:9001
- **MySQL**: localhost:3306

### Default Account

First startup creates super admin. Check `docker/prod.env` for credentials:
- `SUPER_ADMIN_USERNAME`
- `SUPER_ADMIN_PASSWORD`

## Production Environment

### Prerequisites

- Server with Docker installed
- Minimum 2 CPU cores, 2GB RAM
- Domain name (optional)
- SSL certificate (optional)

### Step 1: Clone Repository

```bash
ssh ubuntu@your-server-ip
git clone https://github.com/shinamimi/per-cloud.git
cd per-cloud
```

### Step 2: Configure Environment

```bash
# Create production environment file
cp docker/prod.env.example docker/prod.env

# Edit with your settings
nano docker/prod.env
```

### Step 3: Build and Deploy

```bash
# Build backend
cd backend
./mvnw clean package -DskipTests

# Build frontend
cd ../frontend
npm install
npm run build

# Go back to root
cd ..

# Start production environment
docker compose -f docker-compose.prod.yml up -d --build
```

### Step 4: Verify Deployment

```bash
# Check container status
docker compose -f docker-compose.prod.yml ps

# Check logs
docker compose -f docker-compose.prod.yml logs -f backend
```

## Environment Variables

### Backend (docker/prod.env)

```bash
# MySQL
MYSQL_DATABASE=cloud
MYSQL_USER=cloud
MYSQL_PASSWORD=your-strong-password
MYSQL_ROOT_PASSWORD=your-root-password

# MinIO
MINIO_ROOT_USER=minioadmin
MINIO_ROOT_PASSWORD=your-minio-password
MINIO_ENDPOINT=http://minio:9000
MINIO_BUCKET=cloud-storage
MINIO_PUBLIC_URL=https://your-domain.com/files

# JWT
JWT_SECRET=your-256-bit-secret

# Redis
REDIS_HOST=redis
REDIS_PORT=6379

# Application
SPRING_PROFILES_ACTIVE=prod
```

### Frontend (Nginx)

The frontend is served by Nginx in production. Configuration is in `docker/nginx.conf`.

## Docker Compose Production

### Resource Limits

```yaml
services:
  mysql:
    cpus: 1.0
    mem_limit: 800m
    
  redis:
    cpus: 0.2
    mem_limit: 200m
    
  minio:
    cpus: 0.8
    mem_limit: 512m
    
  backend:
    cpus: 2.0
    mem_limit: 512m
    
  frontend:
    cpus: 0.2
    mem_limit: 128m
```

### Network Configuration

```yaml
networks:
  cloud-network:
    driver: bridge
```

### Volume Persistence

```yaml
volumes:
  mysql-data:
  redis-data:
  minio-data:
```

## SSL/TLS Configuration

### Using Let's Encrypt

```bash
# Install certbot
sudo apt install certbot

# Get certificate
sudo certbot certonly --standalone -d your-domain.com

# Copy certificates
sudo cp /etc/letsencrypt/live/your-domain.com/fullchain.pem docker/ssl/
sudo cp /etc/letsencrypt/live/your-domain.com/privkey.pem docker/ssl/
```

### Nginx SSL Configuration

```nginx
server {
    listen 443 ssl http2;
    server_name your-domain.com;
    
    ssl_certificate /etc/nginx/ssl/fullchain.pem;
    ssl_certificate_key /etc/nginx/ssl/privkey.pem;
    
    # ... rest of configuration
}
```

## Monitoring

### Health Checks

```bash
# Backend health
curl http://localhost:8080/actuator/health

# MySQL health
docker exec cloud-mysql mysqladmin ping -h localhost

# Redis health
docker exec cloud-redis redis-cli ping

# MinIO health
curl http://localhost:9000/minio/health/live
```

### Logs

```bash
# Backend logs
docker compose -f docker-compose.prod.yml logs -f backend

# MySQL logs
docker compose -f docker-compose.prod.yml logs -f mysql

# All services
docker compose -f docker-compose.prod.yml logs -f
```

### Metrics

- **Spring Boot Actuator**: http://localhost:8080/actuator
- **Prometheus**: http://localhost:9090 (if configured)
- **Grafana**: http://localhost:3000 (if configured)

## Backup and Restore

### Database Backup

```bash
# Backup MySQL
docker exec cloud-mysql mysqldump -uroot -p$MYSQL_ROOT_PASSWORD cloud > backup.sql

# Restore MySQL
docker exec -i cloud-mysql mysql -uroot -p$MYSQL_ROOT_PASSWORD cloud < backup.sql
```

### MinIO Backup

```bash
# Backup MinIO data
tar -czf minio-backup.tar.gz /var/lib/docker/volumes/per-cloud_minio-data

# Restore MinIO data
tar -xzf minio-backup.tar.gz -C /
```

### Full Backup

```bash
# Backup everything
docker compose -f docker-compose.prod.yml down
tar -czf full-backup.tar.gz /path/to/per-cloud
docker compose -f docker-compose.prod.yml up -d
```

## Troubleshooting

### Common Issues

#### 1. Port Already in Use

```bash
# Check what's using the port
lsof -i :8080

# Stop the process or change port
```

#### 2. Insufficient Memory

```bash
# Check container memory usage
docker stats

# Reduce memory limits if needed
```

#### 3. Database Connection Failed

```bash
# Check MySQL status
docker compose -f docker-compose.prod.yml logs mysql

# Verify credentials in prod.env
```

#### 4. MinIO Bucket Not Created

```bash
# Manually create bucket
mc alias set myminio http://localhost:9000 minioadmin your-password
mc mb myminio/cloud-storage
```

#### 5. Backend Won't Start

```bash
# Check logs
docker compose -f docker-compose.prod.yml logs backend

# Common issues:
# - Missing environment variables
# - Database not ready
# - MinIO not accessible
```

### Performance Issues

#### 1. Slow Queries

```bash
# Enable slow query log
docker exec cloud-mysql mysql -uroot -p$MYSQL_ROOT_PASSWORD -e "SET GLOBAL slow_query_log = ON; SET GLOBAL long_query_time = 1;"
```

#### 2. High Memory Usage

```bash
# Check JVM heap usage
docker exec cloud-backend jstat -gcutil 1 1000

# Adjust JVM settings in Dockerfile
```

#### 3. Connection Pool Exhausted

```bash
# Check HikariCP metrics
curl http://localhost:8080/actuator/metrics/hikaricp.connections.active
```

## Scaling

### Horizontal Scaling

```yaml
# Add more backend instances
services:
  backend:
    deploy:
      replicas: 3
      
  nginx:
    # Configure upstream
    upstream backend {
        server backend-1:8081;
        server backend-2:8082;
        server backend-3:8083;
    }
```

### Vertical Scaling

```yaml
# Increase resource limits
services:
  backend:
    cpus: 4.0
    mem_limit: 1g
```

### Database Scaling

```yaml
# MySQL read replica
services:
  mysql-replica:
    image: mysql:8.4
    # ... replica configuration
```

## Security Hardening

### 1. Change Default Passwords

```bash
# Generate strong passwords
openssl rand -base64 32

# Update docker/prod.env
```

### 2. Restrict Network Access

```yaml
# Only expose necessary ports
ports:
  - "127.0.0.1:8080:8080"  # Backend only from localhost
```

### 3. Enable Firewall

```bash
# UFW
sudo ufw allow 22/tcp
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable
```

### 4. Regular Updates

```bash
# Update Docker images
docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d
```

## Maintenance

### Log Rotation

```yaml
# Add to docker-compose.prod.yml
services:
  backend:
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"
```

### Cleanup

```bash
# Remove unused images
docker image prune -a

# Remove unused volumes
docker volume prune

# Remove unused networks
docker network prune
```

### Updates

```bash
# Pull latest changes
git pull origin release

# Rebuild and deploy
docker compose -f docker-compose.prod.yml up -d --build
```
