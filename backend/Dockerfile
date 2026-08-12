# =============================================================================
# Cloud 云盘 — 后端生产镜像（runtime 层）
# 构建前提：先执行 ./mvnw package 生成 target/cloud-backend-0.0.1-SNAPSHOT.jar
# 构建上下文：backend/（由 docker-compose.prod.yml 指定）
#
# 设计思路：
# 1. 采用多阶段之外的单层 runtime 镜像：镜像内仅含 JRE + jar，减少体积
# 2. JVM 堆按 2C2G 轻量机预热：-Xmx512m + Serial GC 低内存占用
# 3. prod profile 所有配置来自环境变量（application-prod.yml），镜像不内嵌密钥
# =============================================================================
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY target/cloud-backend-0.0.1-SNAPSHOT.jar app.jar

ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseSerialGC"

EXPOSE 8081

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar --spring.profiles.active=prod"]