# Bug Fix Log 9

## 2026-09-02 — 本地启动报 Access denied，根因是 Mac 本地 MySQL 占用 3306 端口（端口冲突）

**现象：** 后端 `./mvnw spring-boot:run` 启动失败，报 `Access denied for user 'cloud'@'localhost' (using password: YES)`。Docker 容器内 `docker exec` 用相同密码连接正常，JDBC 却连不上。

**根因：** Mac 上有 Oracle MySQL（`/usr/local/mysql/bin/mysqld`）通过 launchd 守护进程自启动，占用主机 3306 端口。Docker 端口映射 `0.0.0.0:3306->3306/tcp` 被本地 MySQL 挡住，JDBC 的 `localhost:3306` 连到了本地 MySQL（无 `cloud` 用户），而非 Docker MySQL。

**排查过程（走了弯路）：**
1. 误以为是密码不匹配，多次 `ALTER USER` 重置 MySQL 密码——实际上 Docker MySQL 密码一直正确
2. 误以为是 `caching_sha2_password` 认证方式问题，尝试创建 `mysql_native_password` 用户——MySQL 8.4 已移除该插件
3. 误以为是 `allowPublicKeyRetrieval` 配置缺失——实际上该参数与问题无关
4. 最终通过 `ps aux | grep mysql` 发现 `/usr/local/mysql/bin/mysqld` 在运行，确认端口冲突

**修复：**
```bash
# 1. 停止本地 MySQL
sudo /usr/local/mysql/support-files/mysql.server stop

# 2. 禁止开机自启动（需 sudo）
sudo launchctl bootout system /Library/LaunchDaemons/com.oracle.oss.mysql.mysqld.plist
sudo mv /Library/LaunchDaemons/com.oracle.oss.mysql.mysqld.plist \
        /Library/LaunchDaemons/com.oracle.oss.mysql.mysqld.plist.disabled
```

**顺带修复：** `application-local.yml` 的 JDBC URL 加上 `allowPublicKeyRetrieval=true`（防止 `caching_sha2_password` 公钥检索报错），密码默认值改为匹配 Docker 容器实际密码。

**经验沉淀：**
- **Docker 容器端口映射 ≠ 端口一定可用**。主机上已有进程占用目标端口时，Docker 端口映射静默失败（`docker ps` 仍显示映射正常，但 `nc -z localhost 3306` 连的是本地进程）。
- 排查 Docker 网络问题第一步：`ps aux | grep <端口对应进程>` + `nc -z localhost <端口>`，确认是谁在监听。
- 不要在排查过程中随意修改数据库用户密码/插件——可能引入新问题，且掩盖真正的根因。

---

## 2026-09-02 — release 分支 merge 时 .gitignore 处理不当（操作失误）

**现象：** 执行 `git merge main` 后 `.gitignore` 产生冲突。解决冲突时，手动将 main 和 release 两边的 `.gitignore` 内容拼合在一起，导致 release 分支的排除规则被污染。

**根因：** release 分支的 `.gitignore` 有公开版排除规则（`docs/`、`scripts/`、`.agents/` 等），main 分支新增了 `BOOT-INF/`。merge 冲突时，正确的做法是保留 release 的排除规则、仅追加 `BOOT-INF/`，但我错误地将两边内容都保留了。

**错误操作：**
```bash
# 错误：手动编辑合并两边 .gitignore
# 保留了 release 的排除规则 + 添加了 BOOT-INF/，但过程中可能引入不一致
```

**正确操作：**
```bash
git checkout release
git merge main
# 冲突发生时：
git checkout release -- .gitignore    # 先恢复 release 版本
echo "BOOT-INF/" >> .gitignore       # 再手动加新规则
git add .gitignore
```

**经验沉淀：**
- **merge 冲突时，先 `git checkout <当前分支> -- <冲突文件>` 恢复当前分支版本**，再有针对性地添加/修改，而不是手动"两边都要"。
- `.gitignore` 是分支级配置，不同分支的排除规则可能完全不同（如 release 排除内部文档），merge 时必须尊重目标分支的规则。
- 合并前应先确认两个分支的 `.gitignore` 差异：`git diff main..release -- .gitignore`。
