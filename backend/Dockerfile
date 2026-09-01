# =============================================================================
# Cloud 云盘 — 后端生产镜像（runtime 层）
# 构建前提：先执行 ./mvnw package 生成 target/cloud-backend-0.0.1-SNAPSHOT.jar
# 构建上下文：backend/（由 docker-compose.prod.yml 指定）
#
# 设计思路：
# 1. 采用多阶段之外的单层 runtime 镜像：镜像内仅含 JRE + jar，减少体积
# 2. JVM 堆按 2C2G 轻量机预热：-Xmx512m + G1GC 低延迟
# 3. prod profile 所有配置来自环境变量（application-prod.yml），镜像不内嵌密钥
# =============================================================================
FROM eclipse-temurin:21-jre

WORKDIR /app

RUN addgroup --system --gid 1001 appgroup && \
    adduser --system --uid 1001 --ingroup appgroup --shell /bin/sh appuser && \
    mkdir -p /app/logs && chown -R appuser:appgroup /app

COPY target/cloud-backend-0.0.1-SNAPSHOT.jar app.jar

ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:MaxMetaspaceSize=128m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/heapdump.hprof -Xlog:gc*:file=/app/logs/gc.log:time,uptime,level,tags:filecount=5,filesize=10m"

EXPOSE 8081

USER appuser

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar --spring.profiles.active=prod"]