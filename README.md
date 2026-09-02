# Cloud Storage

A full-stack private cloud storage platform built with Spring Boot and Vue3, featuring chunked upload, file sharing, RBAC authorization and object storage.

## Features

| Module | Description |
|--------|-------------|
| File Management | Directory tree, list/icon view, search, rename, move, copy, delete |
| Upload & Download | Chunked upload, resumable upload, instant upload, batch ZIP download |
| File Preview | Image/video/audio/PDF/text online preview |
| Share System | Link sharing + extraction code + expiry + anti-hotlink |
| Team Space | Multi-user collaboration, team file isolation, role management |
| Recycle Bin | Soft delete + 30-day auto cleanup |
| Admin Dashboard | Statistics, user management, system config, operation logs |
| Friend System | Add friends, share files between friends |

## Tech Stack

| Layer | Technology |
|-------|------------|
| Backend | Java 21 / Spring Boot 4.0.7 / Spring Security / MyBatis / Redis / MinIO / JWT |
| Frontend | Vue 3 / TypeScript / Pinia / Element Plus |
| Deployment | Docker Compose / Nginx / MySQL 8.4 / Redis 7.2 / MinIO |

## Quick Start

### Prerequisites

- JDK 21+
- Node.js 18+
- Docker & Docker Compose

### Development

```bash
# Clone repository
git clone https://github.com/shinamimi/per-cloud.git
cd per-cloud

# Start infrastructure services (MySQL, Redis, MinIO)
docker compose up -d

# Start backend
cd backend && ./mvnw spring-boot:run

# Start frontend
cd frontend && npm install && npm run dev
```

Visit http://localhost:5173

### Production

```bash
# Build backend
cd backend && ./mvnw clean package -DskipTests

# Build frontend
cd frontend && npm run build

# Deploy
docker compose -f docker-compose.prod.yml up -d
```

## Performance

| Scenario | QPS | Latency |
|----------|-----|---------|
| Login | 950+ | < 50ms |
| File List | 400+ | < 100ms |
| Directory Tree | 120+ | < 100ms |

Optimized for 2C2G cloud servers with G1GC tuning, composite indexes, and connection pool optimization.

## Project Structure

```
cloud-storage/
├── backend/                # Spring Boot backend
│   └── src/main/java/com/cloud/backend/
│       ├── controller/     # REST API
│       ├── service/        # Business logic
│       ├── entity/         # Database entities
│       ├── dto/            # Request/Response DTOs
│       ├── mapper/         # MyBatis Mappers
│       ├── config/         # Configuration classes
│       └── security/       # JWT authentication
├── frontend/               # Vue 3 frontend
│   └── src/
│       ├── views/          # Page components
│       ├── components/     # Shared components
│       ├── stores/         # Pinia state management
│       ├── api/            # API requests
│       └── router/         # Route configuration
├── sql/                    # Database scripts
├── docker/                 # Docker configuration
└── docs/public/            # Public technical documentation
```

## Documentation

- [Architecture](docs/public/ARCHITECTURE.md)
- [Performance Optimization](docs/public/PERFORMANCE.md)
- [Security Design](docs/public/SECURITY.md)
- [Upload Mechanism](docs/public/UPLOAD-MECHANISM.md)
- [Deployment Guide](docs/public/DEPLOYMENT.md)

## License

[MIT](LICENSE)
