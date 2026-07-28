# Bug Fix Log

## 2026-07-28 — 后端启动失败修复

### 1. JWT Secret 不是合法 Base64

**错误：**
```
io.jsonwebtoken.io.DecodingException: Illegal base64 character: '-'
        at com.cloud.backend.utils.JwtTokenUtil.<init>(JwtTokenUtil.java:34)
```

**根因：** `application-local.yml` 和 `application-dev.yml` 中 `jwt.secret` 的默认值为 `my-secret-key-change-in-production`，其中包含 `-` 字符，不是合法 Base64。JJWT 的 `Decoders.BASE64.decode()` 做严格校验，直接抛出解码异常。

**修复：** 生成 32 字节随机 Base64 密钥替换默认值：

| 文件 | 变更 |
|---|---|
| `backend/src/main/resources/application-local.yml:29` | `secret` 值改为 Base64 密钥 |
| `backend/src/main/resources/application-dev.yml:27` | `secret` 默认值改为 Base64 密钥 |

---

### 2. MyBatisTypeHandlerConfig 注入 Configuration 时机太早

**错误：**
```
Parameter 0 of constructor in com.cloud.backend.config.MyBatisTypeHandlerConfig
required a bean of type 'org.apache.ibatis.session.Configuration' that could not be found.
```

**根因：** `MyBatisTypeHandlerConfig` 使用 `@Configuration` + 构造器注入 `org.apache.ibatis.session.Configuration`。但 `@Configuration` 类会在 Spring 容器初始化早期被处理，此时 MyBatis 的 `Configuration` bean 尚未创建。

**修复：** 改为实现 `ConfigurationCustomizer` 接口，由 MyBatis-Spring-Boot 在正确的生命周期回调：

```java
// before
@Configuration
public class MyBatisTypeHandlerConfig {
    public MyBatisTypeHandlerConfig(org.apache.ibatis.session.Configuration configuration) {
        ...
    }
}

// after
@Component
public class MyBatisTypeHandlerConfig implements ConfigurationCustomizer {
    @Override
    public void customize(Configuration configuration) {
        ...
    }
}
```

| 文件 | 变更 |
|---|---|
| `backend/src/main/java/.../config/MyBatisTypeHandlerConfig.java` | 改为 `ConfigurationCustomizer` 实现类 |

---

### 3. 缺少 spring.mail.host 配置，JavaMailSender 未创建

**错误：**
```
Parameter 0 of constructor in com.cloud.backend.service.EmailService
required a bean of type 'org.springframework.mail.javamail.JavaMailSender' that could not be found.
```

**根因：** `application-local.yml` 只配置了自定义命名空间 `mail.*`（对应 `MailProperties`），缺少 `spring.mail.*`。Spring Boot 的 `JavaMailSender` 自动配置不会触发，导致 `EmailService` 注入失败。

**修复：** 在 `application-local.yml` 中补全 `spring.mail.host`、`spring.mail.port`、`spring.mail.username`、`spring.mail.password`。本地环境使用占位值：

| 文件 | 变更 |
|---|---|
| `backend/src/main/resources/application-local.yml` | 新增 `spring.mail.*` 配置段 |

---

### 4. SuperAdminInitializer 未设置 quota 和 used_space

**错误：**
```
java.sql.SQLIntegrityConstraintViolationException: Column 'quota' cannot be null
java.sql.SQLIntegrityConstraintViolationException: Column 'used_space' cannot be null
```

**根因：** `SuperAdminInitializer` 创建 `User` 对象时只设置了用户名/密码/邮箱/昵称/角色/状态，没有设置 `quota` 和 `usedSpace`。但 `t_user` 表的这两列是 `NOT NULL`，MyBatis 的 INSERT 语句显式列出了所有列，传 null 导致数据库报错。

**修复：** 初始化时补充空间配额和已用空间：

| 文件 | 变更 |
|---|---|
| `backend/src/main/java/.../config/SuperAdminInitializer.java` | 添加 `newAdmin.setQuota(FileConstants.DEFAULT_QUOTA)` 和 `newAdmin.setUsedSpace(0L)` |
