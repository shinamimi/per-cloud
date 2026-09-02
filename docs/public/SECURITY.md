# Security Design

## Overview

Per-Cloud implements a comprehensive security system covering authentication, authorization, file protection, and data integrity.

## Authentication

### JWT Token System

```
┌─────────────────────────────────────────────────────────────┐
│                    Authentication Flow                       │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│  1. User submits credentials (username/password)            │
│  2. Server validates against BCrypt hash                    │
│  3. Server generates JWT token (24h expiry)                 │
│  4. Client stores token in memory                           │
│  5. Client sends token in Authorization header              │
│  6. Server validates token on each request                  │
└─────────────────────────────────────────────────────────────┘
```

### JWT Configuration

```yaml
jwt:
  secret: ${JWT_SECRET}         # 256-bit secret key
  expiration: 86400000          # 24 hours
  header: Authorization
  prefix: "Bearer "
  issuer: cloud
```

### Token Blacklisting

- Revoked tokens stored in Redis
- Logout adds token to blacklist
- Blacklisted tokens rejected on validation

### Password Security

- **Hashing**: BCrypt with 10 rounds
- **Storage**: Only hashed passwords in database
- **Validation**: Timing-safe comparison

## Authorization

### RBAC Model

```
┌─────────────────────────────────────────────────────────────┐
│                      Role Hierarchy                         │
├─────────────────────────────────────────────────────────────┤
│  SUPER_ADMIN (100) → Full system access                     │
│      ↓                                                      │
│  ADMIN (20) → User management, system config                │
│      ↓                                                      │
│  OPERATOR (10) → File management, share management          │
│      ↓                                                      │
│  USER (0) → Basic file operations                           │
└─────────────────────────────────────────────────────────────┘
```

### Permission Matrix

| Endpoint | USER | OPERATOR | ADMIN | SUPER_ADMIN |
|----------|------|----------|-------|-------------|
| GET /api/files/** | ✅ | ✅ | ✅ | ✅ |
| POST /api/files/upload | ✅ | ✅ | ✅ | ✅ |
| DELETE /api/files/{id} | ✅ | ✅ | ✅ | ✅ |
| GET /api/admin/users | ❌ | ❌ | ✅ | ✅ |
| PUT /api/admin/settings | ❌ | ❌ | ❌ | ✅ |
| POST /api/admin/reset-password | ❌ | ❌ | ✅ | ✅ |

### Implementation

```java
// URL-level security in SecurityConfig
http.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/auth/login", "/api/auth/register").permitAll()
    .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
    .anyRequest().authenticated()
);
```

## File Security

### User Isolation

- Users can only access their own files
- Team files isolated by team_id
- Ownership verified on every operation

```java
// Example: File access check
File file = fileMapper.findById(fileId);
if (!file.getUserId().equals(currentUserId)) {
    throw new AccessDeniedException("Access denied");
}
```

### Presigned URLs

MinIO presigned URLs for secure file access:

```java
// Generate presigned URL (expires in 1 hour)
String url = minioClient.getPresignedObjectUrl(
    GetPresignedObjectUrlArgs.builder()
        .bucket(bucket)
        .object(objectName)
        .method(Method.GET)
        .expiry(1, TimeUnit.HOURS)
        .build()
);
```

**Security features**:
- Time-limited (1 hour default)
- No credentials in URL
- IP restriction (optional)
- Method restriction (GET only)

### Anti-Hotlinking

- Referer validation for file access
- Only requests from allowed domains accepted
- Blocks direct URL access from unauthorized sites

## Upload Security

### File Type Validation

```java
// Allowed file types
private static final Set<String> ALLOWED_TYPES = Set.of(
    "image/jpeg", "image/png", "image/gif", "image/webp",
    "application/pdf", "text/plain", "application/zip",
    "video/mp4", "audio/mpeg"
);

// Validate file type
if (!ALLOWED_TYPES.contains(file.getContentType())) {
    throw new BusinessException("File type not allowed");
}
```

### File Size Limits

- **Normal user**: 512MB max
- **VIP user**: 2GB max
- **Configurable via admin settings**

### Upload Quota

- **Default**: 10GB per user
- **VIP**: 100GB per user
- **Admin bonus**: Additional quota from admin
- **Atomic deduction**: Qu扣减 in single transaction

### Chunk State Management

Redis tracks upload session:

```redis
HSET upload:{uploadId}
  fileName "report.pdf"
  fileHash "abc123..."
  totalChunks 10
  uploadedChunks "1,2,3,4,5"
  createdAt "2026-09-01T10:00:00"
EXPIRE upload:{uploadId} 86400  # 24 hours
```

### Rate Limiting

- **Login attempts**: 5 per user, then 15-minute lockout
- **IP rate limit**: 10 requests/minute for login
- **Upload rate limit**: Configurable per user/VIP

```java
@RateLimit(key = "login", maxAttempts = 5, lockoutMinutes = 15)
public User login(String username, String password) {
    // ...
}
```

## Data Integrity

### Transaction Control

```java
@Transactional
public FileNodeResponse merge(Long userId, String uploadId) {
    // 1. Verify upload session exists
    // 2. Merge chunks into single object
    // 3. Verify file hash with DigestInputStream
    // 4. Create file record in database
    // 5. Deduct user quota
    // All in single transaction - rollback on failure
}
```

### Hash Verification

Two-stage hash verification:

1. **Client-side**: SHA-256 before upload
2. **Server-side**: DigestInputStream during merge

```java
// Server-side verification
InputStream merged = new SequenceInputStream(enumeration);
MessageDigest digest = MessageDigest.getInstance("SHA-256");
InputStream digestStream = new DigestInputStream(merged, digest);

// Write to MinIO
storageService.putObject(objectName, digestStream, size);

// Verify hash
String computedHash = HexFormat.of().formatHex(digest.digest());
if (!computedHash.equals(expectedHash)) {
    throw new BusinessException("Hash mismatch - file may be corrupted");
}
```

### State Machine

```
Upload Session States:
  CREATED → UPLOADING → MERGING → COMPLETED
                         ↓
                      FAILED

File States:
  NORMAL (1) → DELETED (0) → PHYSICALLY_DELETED
       ↑                         ↓
       └───── RESTORED ──────────┘
```

### Reference Counting

```sql
-- Instant upload: Increment ref count
UPDATE t_file_hash SET ref_count = ref_count + 1 WHERE file_hash = ?;

-- Delete file: Decrement ref count
UPDATE t_file_hash SET ref_count = ref_count - 1 WHERE file_hash = ?;

-- Cleanup: Delete objects with ref_count = 0
DELETE FROM t_file_hash WHERE ref_count = 0;
```

## Security Checklist

### Authentication
- [x] JWT tokens with expiry
- [x] BCrypt password hashing
- [x] Token blacklisting
- [x] Login rate limiting
- [x] Account lockout

### Authorization
- [x] RBAC implementation
- [x] URL-level permissions
- [x] User isolation
- [x] Team isolation

### File Security
- [x] User isolation
- [x] Presigned URLs
- [x] Anti-hotlinking
- [x] File type validation
- [x] File size limits

### Upload Security
- [x] Upload quota
- [x] Chunk state management
- [x] Rate limiting
- [x] Hash verification

### Data Integrity
- [x] Transaction control
- [x] Hash verification
- [x] State machine
- [x] Reference counting

## Security Best Practices

### 1. Never Trust User Input

- Validate all inputs at API boundary
- Sanitize file names
- Reject unexpected file types

### 2. Principle of Least Privilege

- Users only access their own files
- Admin operations require ADMIN role
- Default deny, explicit allow

### 3. Defense in Depth

- Multiple validation layers
- Client-side + server-side validation
- Database constraints

### 4. Secure by Default

- Strong passwords required
- Sessions expire after 24 hours
- Sensitive data not logged

### 5. Monitor and Log

- Operation logging for audit
- Failed login attempts
- Unusual access patterns
