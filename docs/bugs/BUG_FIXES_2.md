# Bug Fix Log 2

## 2026-07-29 — AuthController 注册时未设置 usedSpace

**错误：**
```
java.sql.SQLIntegrityConstraintViolationException: Column 'used_space' cannot be null
```

**根因：** `AuthController.register()` 创建 `User` 对象时设置了 `quota`（第 147 行）但遗漏 `usedSpace`。数据库 `t_user.used_space` 列为 `NOT NULL`，MyBatis INSERT 语句显式包含该列，传 `null` 导致报错。

**修复：** 注册时初始化 `usedSpace` 为 0：

| 文件 | 变更 |
|---|---|
| `backend/src/main/java/.../controller/AuthController.java:147` | 新增 `user.setUsedSpace(0L)` |

---

## 2026-07-29 — 兜底异常未写入日志

**问题：** 注册时 `quota` 未设置导致的 `SQLIntegrityConstraintViolationException` 没有出现在任何日志文件中，前端收到 500 但后端排查无从下手。

**根因：** `GlobalExceptionHandler.handleException(Exception e)`（第 49-52 行）只调用 `Result.fail()` 返回给客户端，没有打印日志。异常信息和堆栈完全丢失。

```java
// before
@ExceptionHandler(Exception.class)
public Result<Void> handleException(Exception e) {
    return Result.fail(ErrorCode.INTERNAL_ERROR, e.getMessage());
}

// after
@ExceptionHandler(Exception.class)
public Result<Void> handleException(Exception e) {
    log.error("Unhandled exception", e);                     // ← 新增
    return Result.fail(ErrorCode.INTERNAL_ERROR, e.getMessage());
}
```

| 文件 | 变更 |
|---|---|
| `backend/src/main/java/.../exception/GlobalExceptionHandler.java:50` | 新增 `log.error("Unhandled exception", e)` |

---

## 2026-07-29 — Brevo SMTP 发件配置全流程

涉及多个子问题，整个对话都在排查同一个功能——邮箱验证码发送。

### 1. Port 8080 已被占用

**现象：** `APPLICATION FAILED TO START — Port 8080 was already in use.`

**根因：** 旧后端进程未关闭，新进程端口冲突。

**修复：** 启动前 `kill $(lsof -ti:8080)`。

---

### 2. SMTP Authentication failed（首次）

**现象：** 点击发送验证码，前端提示 "Authentication failed" / "Request failed with status code 500"。

**根因：** 本地开发默认 profile 为 `local`，`application-local.yml` 中 `MAIL_USERNAME` 和 `MAIL_PASSWORD` 的兜底值为 `placeholder@local.dev` / `placeholder`，使用这些凭据连接 Brevo SMTP 必然认证失败。

**修复：** 在根目录 `.env` 中配置真实 SMTP 凭据，启动前 `source .env`。

---

### 3. shell 变量未 export 到子进程

**现象：** `source .env` 后 `echo $MAIL_HOST` 有值，但 Java 进程拿不到，仍用 placeholder。

**根因：** `.env` 格式为 `KEY=VALUE` 而非 `export KEY=VALUE`，`source` 后变量只在当前 shell 可见，不会传给子进程（mvnw → Java）。

**修复：** 启动前用 `set -a && source .env && set +a` 批量 export，或将 `.env` 改为 `export KEY=VALUE` 格式。

---

### 4. Brevo 需要 STARTTLS

**现象：** 凭据正确后仍报 "525 5.7.1 Unauthorized" / "Authentication failed"。

**根因：** Brevo SMTP 587 端口要求 STARTTLS 握手，YAML 中未配置。

**修复：** 在 `spring.mail.properties` 下追加：

```yaml
spring:
  mail:
    properties:
      mail:
        smtp:
          starttls:
            enable: true
            required: true
```

---

### 5. Brevo IP 白名单

**现象：** 配置 STARTTLS 后报 "525 5.7.1 Unauthorized IP address"。

**根因：** Brevo 安全策略阻挡未授权 IP 的 SMTP 请求。

**修复：** Brevo 后台 → Settings → Security → Authorized IPs，添加出口 IP。或直接关闭 SMTP IP 阻断。

---

### 6. Spring Boot 宽松绑定覆盖 YAML 占位符

**现象：** Brevo 日志中发件人（From）始终为 `b3a868001@smtp-brevo.com`，即使 YAML 用了 `${MAIL_FROM}`。

**根因：** Spring Boot 将 `MAIL_USERNAME` 环境变量通过宽松绑定（relaxed binding）自动注入到 `mail.username`（`@ConfigurationProperties`），直接覆盖了 YAML 中 `${MAIL_FROM}` 占位符，导致 From 地址始终等于 SMTP 登录名。

**修复：** 将环境变量前缀从 `MAIL_` 改为 `SMTP_`，避免与 `mail.*` 配置属性的宽松绑定冲突：

```env
SMTP_HOST=smtp-relay.brevo.com
SMTP_PORT=587
SMTP_USERNAME=b3a868001@smtp-brevo.com
SMTP_PASSWORD=xsmtpsib-...
SMTP_FROM=已验证的发件人邮箱
```

YAML 中所有 `${MAIL_*}` 对应改为 `${SMTP_*}`。

---

### 7. Brevo 发件人未验证

**现象：** 邮件在 Brevo 后台显示 "Sent" 但收件方始终收不到，Brevo 日志提示 `sender is not valid`。

**根因：** `b3a868001@smtp-brevo.com` 是 Brevo 的 SMTP 登录标识，不是真实邮箱，无法作为发件人。

**修复：** Brevo 后台 → Settings → Senders → Add a sender，验证真实邮箱（如 `Konhtt55@126.com`）。`MAIL_FROM` / `SMTP_FROM` 应设为已验证地址。
