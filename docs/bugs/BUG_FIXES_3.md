# Bug Fix Log 3

## 2026-07-30 — Enum 后缀导致 JSON 字段名泄露

**错误：** 前端拿不到预期字段。`AdminUserResponse` 返回的 JSON 中出现 `"roleEnum"` 而不是 `"role"`，`AdminLogResponse` 中出现 `"targetTypeEnum"` 而不是 `"targetType"`。

```json
// 预期
{ "role": "ADMIN", "targetType": "FILE" }

// 实际
{ "roleEnum": "ADMIN", "targetTypeEnum": "FILE" }
```

**根因：** 之前定下的命名规范要求枚举类加 `Enum` 后缀（如 `RoleEnum`）。Jackson 按 JavaBean 规范通过 **getter 方法名**推断 JSON 字段名：

```
类型名：RoleEnum
getter：getRoleEnum()
JavaBean 推导：去掉 "get" → 属性名 "roleEnum" → JSON key "roleEnum"
```

`Enum` 后缀通过 getter 名泄漏到 JSON 序列化结果中。这不是 Jackson 的配置问题，而是命名规范与 JavaBean 规范的冲突。

```java
// AdminUserResponse.java
private RoleEnum role;

// 手写 getter 时类型名被 IDE 带入方法名
public RoleEnum getRoleEnum() { return role; }  // ← 问题在这
```

**影响范围：**
- `dto/admin/AdminUserResponse.java` — `getRoleEnum()` → JSON `"roleEnum"`
- `dto/admin/AdminLogResponse.java` — `getTargetTypeEnum()` → JSON `"targetTypeEnum"`
- 未来所有 DTO 手写 getter 返回枚举类型时，都可能踩坑

**修复决策：** 移除枚举类的 `Enum` 后缀，从命名规范层面根治，而不是逐个加 `@JsonProperty` 修补。

**对比：**

| 方案 | 优点 | 缺点 |
|------|------|------|
| 加 `@JsonProperty` | 改动小 | 每个枚举 getter 都要加，漏写则编译通过但运行时前端拿不到字段 |
| 去掉 `Enum` 后缀 | 根治，JSON 字段名与类名/字段名一致 | 一次全项目批量重构（10 个枚举 + ~74 个引用文件） |

**执行：**

| 步骤 | 操作 |
|------|------|
| 1 | `mv` 重命名 10 个枚举文件（去掉 `Enum` 后缀） |
| 2 | `sed` 批量替换所有 `.java` 引用（`RoleEnum`→`Role` 等 10 组） |
| 3 | 更新 `CODING_STANDARDS.md` 枚举命名规则：`{领域}Enum` → `{领域}` |
| 4 | 更新 `admin-user-management.md` 中的枚举引用 |
| 5 | `mvn compile` 验证通过 |

**命名规范变更：**

| 变更前 | 变更后 |
|--------|--------|
| `enums/` 统一 `XxxEnum` | `enums/` 统一 `{领域}`（不加后缀） |
| 示例：`RoleEnum`、`UserStatusEnum` | 示例：`Role`、`UserStatus` |

**教训：** 命名规范要考虑与框架/生态规则的交互（JavaBean 规范、Jackson 序列化）。后缀 `Enum` 在类型层面无害，但会通过 getter 名进入 JSON 字段名。常规 Java/Spring 项目枚举不加大类后缀，`Role.ADMIN` 本身已足够清晰。

---

## 2026-07-30 — macOS sed 不支持 `\b` 词边界

**错误：** 批量替换枚举名时，第一次 `sed` 使用 `\bRoleEnum\b` 模式，替换完全没有生效（`mvn compile` 仍报 `枚举 RoleEnum 是公共的, 应在名为 RoleEnum.java 的文件中声明`）。

**根因：** `\b`（词边界）是 GNU sed 扩展，macOS 自带的 BSD sed 不支持，导致整个替换命令静默失败，不报任何错误。

**修复：** 去掉 `\b` 直接替换。因为 `RoleEnum` 等类名不会作为其他标识符的子串出现，无词边界替换是安全的：

```bash
# 错误（macOS 无效）
sed -i '' 's/\bRoleEnum\b/Role/g'

# 正确（macOS 有效）
sed -i '' 's/RoleEnum/Role/g'
```

**教训：** macOS 默认 `sed` 是 BSD 版本，与 Linux 的 GNU sed 有差异（`\b`、`-i` 语法等）。在 macOS 上批量替换优先用无 `\b` 的纯字符串模式，或用 `perl -pi -e`。
