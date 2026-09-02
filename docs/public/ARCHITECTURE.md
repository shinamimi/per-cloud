# Architecture

## System Overview

Per-Cloud is a full-stack private cloud storage system built with a classic three-tier architecture:

```
┌─────────────────────────────────────────────────────────────┐
│                      Client (Browser)                       │
│                     Vue 3 + TypeScript                      │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    Nginx (Reverse Proxy)                    │
│                  Static files + API proxy                   │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                  Backend (Spring Boot 4)                    │
│         Spring Security + MyBatis + Redis + MinIO           │
└─────────────────────────────────────────────────────────────┘
                              │
            ┌─────────────────┼─────────────────┐
            ▼                 ▼                 ▼
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│    MySQL 8.4    │ │    Redis 7.2    │ │      MinIO      │
│   (Metadata)    │ │ (Cache/Session) │ │   (Objects)     │
└─────────────────┘ └─────────────────┘ └─────────────────┘
```

## Technology Choices

### Backend: Spring Boot 4

- **Why Spring Boot**: Rapid development, auto-configuration, extensive ecosystem
- **Why Spring Security**: Industry-standard authentication/authorization framework
- **Why MyBatis**: Full control over SQL, easy optimization for complex queries
- **Why Redis**: High-performance cache, session store, rate limiting
- **Why MinIO**: S3-compatible, self-hosted, easy deployment

### Frontend: Vue 3

- **Why Vue 3**: Composition API, TypeScript support, smaller bundle
- **Why Element Plus**: Rich UI components, good documentation
- **Why Pinia**: Type-safe state management, Vue DevTools support

### Database: MySQL 8.4

- **InnoDB**: ACID transactions, row-level locking
- **UTF8MB4**: Full Unicode support (including emojis)
- **JSON support**: Flexible metadata storage

## Data Model

### Core Tables

```sql
-- User table
t_user (
  id, username, password, email, role, quota, used_space, status
)

-- File table (unified model for files and directories)
t_file (
  id, user_id, team_id, parent_id, name, path, size, 
  file_hash, type, category, object_name, status
)

-- File hash table (for instant upload)
t_file_hash (
  id, file_hash, object_name, size, ref_count
)

-- Share table
t_share (
  id, user_id, file_id, share_token, password, 
  expire_time, max_download, allow_download, allow_save
)

-- Team table
t_team (id, name, owner_id, description)

-- Team member table
t_team_member (id, team_id, user_id, role)
```

### Key Design Decisions

1. **Unified File Table**: Files and directories share the same table (`type` field distinguishes them)
2. **Soft Delete**: Files are marked as deleted (`status=0`) but not physically removed
3. **Reference Counting**: `t_file_hash.ref_count` tracks shared objects for instant upload
4. **Path Denormalization**: `t_file.path` stores full path for fast queries

## API Design

### RESTful Principles

- **Resources**: `/api/files`, `/api/teams`, `/api/shares`
- **HTTP Methods**: GET (read), POST (create), PUT (update), DELETE (delete)
- **Status Codes**: 200 (success), 400 (bad request), 401 (unauthorized), 403 (forbidden), 404 (not found)

### Authentication Flow

```
1. Client → POST /api/auth/login {username, password}
2. Server → Validate credentials, generate JWT
3. Client → Store JWT in memory
4. Client → Request with Authorization: Bearer <token>
5. Server → Validate JWT, check permissions
```

### File Upload Flow

```
1. Client → Calculate SHA-256 hash
2. Client → POST /api/files/sec (instant upload check)
3. Server → Check t_file_hash for matching hash
4. If match → Return existing file reference (instant upload)
5. If no match → POST /api/files/upload/init
6. Server → Create upload session in Redis
7. Client → Upload chunks concurrently
8. Server → Merge chunks, verify hash
9. Server → Create file record in database
```

## Deployment Architecture

### Development Environment

```bash
docker compose up -d
# Starts: MySQL, Redis, MinIO
# Backend: ./mvnw spring-boot:run
# Frontend: npm run dev
```

### Production Environment

```bash
docker compose -f docker-compose.prod.yml up -d
# All services containerized
# Resource limits for 2C2G server
# MySQL/Redis only accessible from localhost
```

### Resource Allocation (2C2G)

| Service | CPU | Memory | Purpose |
|---------|-----|--------|---------|
| MySQL | 1.0 | 800MB | Metadata storage |
| Redis | 0.2 | 200MB | Cache/session |
| MinIO | 0.8 | 512MB | Object storage |
| Backend | 2.0 | 512MB | API service |
| Frontend | 0.2 | 128MB | Static files |

## Design Patterns

### 1. Repository Pattern (MyBatis Mappers)

- Separates data access from business logic
- Easy to test and mock
- SQL optimization in XML files

### 2. Service Layer Pattern

- Business logic in service implementations
- Transaction management at service level
- Clear separation of concerns

### 3. DTO Pattern

- Request/Response DTOs isolate API from domain model
- Validation at DTO level
- Prevents over-fetching

### 4. State Machine (File Status)

```
正常 (status=1) → 已删除 (status=0) → 彻底删除
     ↑                                    ↓
     └────────── 恢复 (status=1) ←────────┘
```

### 5. Rate Limiting

- **Per-user**: 5 failed login attempts → 15-minute lockout
- **Per-IP**: 10 requests/minute for login
- **Implementation**: Redis + Lua atomic operations

## Scalability Considerations

### Current Limitations (2C2G)

- Single-server deployment
- No horizontal scaling
- Limited concurrent users (~100)

### Future Improvements

1. **Microservices**: Split into auth, file, share services
2. **Load Balancer**: Nginx upstream for multiple backend instances
3. **Database Replication**: MySQL master-slave for read scaling
4. **CDN**: MinIO + CDN for global file access
5. **Kubernetes**: Container orchestration for auto-scaling

## Security Architecture

### Authentication

- JWT tokens with 24-hour expiry
- BCrypt password hashing (10 rounds)
- Token blacklisting via Redis

### Authorization

- RBAC with 4 roles: USER, OPERATOR, ADMIN, SUPER_ADMIN
- URL-level permission matrix in SecurityConfig
- Method-level security with `@PreAuthorize`

### File Security

- User isolation: Users can only access their own files
- Signed URLs: Temporary MinIO presigned URLs for downloads
- Anti-hotlinking: Referer validation for file access

### Data Protection

- SQL injection prevention: MyBatis parameterized queries
- XSS protection: Input sanitization
- CSRF protection: Stateless JWT (no session)
