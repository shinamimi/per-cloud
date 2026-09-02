# Performance Optimization

## Overview

Per-Cloud is optimized for low-cost 2-core 2GB (2C2G) cloud servers. This document details the tuning strategies and results.

## JVM Tuning

### G1GC Configuration

```bash
-XX:+UseG1GC                          # Use Garbage-First GC
-XX:MaxGCPauseMillis=200              # Target max pause time
-XX:MaxMetaspaceSize=128m             # Limit metaspace
-Xms256m -Xmx512m                    # Heap size (256MB initial, 512MB max)
-XX:+HeapDumpOnOutOfMemoryError       # Enable heap dump on OOM
-XX:HeapDumpPath=/tmp/heapdump.hprof  # Heap dump location
```

### GC Monitoring

```bash
-Xlog:gc*:file=/app/logs/gc.log:time,uptime,level,tags:filecount=5,filesize=10m
```

### Results

| Metric | Value |
|--------|-------|
| Young GC Pause | 3-15ms |
| Full GC Frequency | Rare (< 1/hour) |
| Heap Usage | 40-60% under load |
| OOM Incidents | 0 (after optimization) |

## Database Optimization

### HikariCP Connection Pool

```yaml
hikari:
  maximum-pool-size: 30        # Max connections
  minimum-idle: 5              # Min idle connections
  connection-timeout: 5000     # 5s timeout
  idle-timeout: 600000         # 10min idle timeout
  max-lifetime: 1800000        # 30min max lifetime
  leak-detection-threshold: 60000  # 60s leak detection
```

### Index Optimization

#### Problem: Directory Tree Query (100万 files)

Original query scanned 1M rows:
```sql
SELECT * FROM t_file WHERE user_id = 3 AND team_id = 0 AND status != 0 AND type = 1
```

**EXPLAIN output**: `rows=494534` (full scan)

#### Solution: Composite Index

```sql
CREATE INDEX idx_user_team_type_status ON t_file(user_id, team_id, type, status)
```

**EXPLAIN output**: `rows=282` (index scan)

#### Result

| Metric | Before | After | Improvement |
|--------|--------|-------|-------------|
| Query Time | 10s | 0.02s | 500x |
| Rows Scanned | 494,534 | 282 | 1,753x |

### CTE Query Optimization

#### Problem: Subtree Delete Query

Recursive CTE for subtree collection was slow with large datasets.

#### Solution: Optimized CTE

```sql
-- Before: Full file info
WITH RECURSIVE subtree AS (
  SELECT id, user_id, team_id, parent_id, name, path, type, status, 
         created_at, updated_at, size, file_hash, object_name, category
  FROM t_file WHERE id = ?
  UNION ALL
  SELECT f.* FROM t_file f JOIN subtree s ON f.parent_id = s.id
)

-- After: Minimal columns
WITH RECURSIVE subtree AS (
  SELECT id, parent_id, name, type, file_hash, object_name, size
  FROM t_file WHERE id = ?
  UNION ALL
  SELECT f.id, f.parent_id, f.name, f.type, f.file_hash, f.object_name, f.size
  FROM t_file f INNER JOIN subtree s ON f.parent_id = s.id
)
```

**Result**: Delete time reduced from 40s+ to 1s

### Query Optimization

#### Problem: findByUserId OOM

Loading all files for a user caused OutOfMemoryError:
```java
// Before: Loads ALL files (1M+ rows)
List<File> files = fileMapper.findByUserId(userId);
```

#### Solution: Load Only Directories

```java
// After: Load only directories (281 rows)
List<File> dirs = fileMapper.findDirsByUserId(userId);
```

**Result**: Zero OOM incidents

## Redis Optimization

### Lettuce Connection Pool

```yaml
spring.data.redis.lettuce.pool:
  max-active: 32     # Max connections
  max-idle: 16       # Max idle connections
  min-idle: 4        # Min idle connections
  max-wait: 3000     # 3s max wait
```

### Cache Strategy

1. **Upload Session**: Redis hash for chunk tracking (TTL: 24h)
2. **Rate Limiting**: Redis sorted sets for sliding window
3. **JWT Blacklist**: Redis set for revoked tokens
4. **File Progress**: Redis hash for upload progress

## Tomcat Tuning

```yaml
server.tomcat.threads:
  max: 50              # Max worker threads
  min-spare: 5         # Min spare threads
server.tomcat.max-connections: 100
server.tomcat.accept-count: 100
server.tomcat.connection-timeout: 10000
```

## Performance Results

### Benchmark Environment

- **Server**: Tencent Cloud 2C2G (2 vCPU, 2GB RAM)
- **OS**: Ubuntu 22.04
- **Docker**: 24.0
- **Database**: MySQL 8.4 (1 CPU, 800MB)
- **Cache**: Redis 7.2 (0.2 CPU, 200MB)

### API Performance

| Endpoint | QPS | P99 Latency | Notes |
|----------|-----|-------------|-------|
| POST /api/auth/login | 950+ | < 50ms | With bcrypt verification |
| GET /api/files?parentId=0 | 400+ | < 100ms | Root directory listing |
| GET /api/files/tree | 120+ | < 100ms | 281 directories (100万 files) |
| POST /api/files/upload/chunk | 200+ | < 200ms | 5MB chunk upload |
| DELETE /api/files/{id} | 50+ | < 2s | Subtree delete (CTE) |

### Concurrency Test

- **100 concurrent users**: Stable
- **Duration**: 10 minutes
- **Error rate**: 0%
- **Memory usage**: 60-70% (backend)
- **GC pauses**: < 15ms

### Stress Test (100万 Files)

| Scenario | Result |
|----------|--------|
| Directory tree query | 0.02s (was 10s) |
| Single directory listing (10K files) | 0.03s |
| Root directory listing (282 dirs) | 0.02s |
| File delete (subtree) | 1s (was 40s+) |
| JVM OOM | 0 (was 2 incidents) |

## Optimization Checklist

### JVM
- [x] G1GC enabled
- [x] Heap size tuned (512MB max)
- [x] GC logging enabled
- [x] Heap dump on OOM

### Database
- [x] HikariCP pool configured
- [x] Composite indexes added
- [x] CTE queries optimized
- [x] Slow query logging enabled

### Redis
- [x] Lettuce pool configured
- [x] Connection timeout set
- [x] Memory limit configured

### Application
- [x] Batch queries (avoid N+1)
- [x] Lazy loading for large datasets
- [x] Connection pool monitoring
- [x] Rate limiting implemented

## Monitoring Recommendations

### Key Metrics to Monitor

1. **JVM**: Heap usage, GC frequency, GC pause time
2. **Database**: Connection pool usage, query latency, slow queries
3. **Redis**: Memory usage, connection count, hit rate
4. **Application**: Request latency, error rate, throughput

### Tools

- **Prometheus**: Metrics collection
- **Grafana**: Visualization
- **Spring Boot Actuator**: Health checks
- **MySQL Performance Schema**: Query analysis
