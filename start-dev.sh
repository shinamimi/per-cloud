#!/bin/bash
# 一键启动 Cloud 云盘本地开发环境
#
# 启动顺序:
#   1. Docker 基础设施 (MySQL + Redis + MinIO)
#   2. 后端 Spring Boot (source .env && ./mvnw spring-boot:run)
#   3. 前端 Vite 开发服务器 (npm run dev -- --open)
#
# 停止: Ctrl+C 即可停止所有服务

set -e

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
BACKEND_READY_URL="http://localhost:8081/api/auth/login"

stop_all() {
  echo ""
  echo "正在停止所有服务..."
  [ -n "$BACKEND_PID" ] && kill "$BACKEND_PID" 2>/dev/null && echo "  [停止] 后端"
  [ -n "$FRONTEND_PID" ] && kill "$FRONTEND_PID" 2>/dev/null && echo "  [停止] 前端"
  echo "  [停止] Docker 基础设施 (保留容器)"
  exit 0
}

trap stop_all SIGINT SIGTERM

echo "=========================================="
echo "  Cloud 云盘 — 开发环境一键启动"
echo "=========================================="

# 1. 启动基础设施
echo "[1/3] 启动 Docker 基础设施 (MySQL + Redis + MinIO)..."
docker compose -f "$ROOT_DIR/docker-compose.yml" up -d
echo "       MySQL:   localhost:3306"
echo "       Redis:   localhost:6379"
echo "       MinIO:   localhost:9000 (API) / localhost:9001 (Console)"

# 2. 启动后端（后台运行，等待就绪）
echo "[2/3] 启动后端 (Spring Boot :8081)..."
cd "$ROOT_DIR/backend"
# 加载根目录 .env（SMTP 等敏感配置），随后启动 Spring Boot
set -a; source "$ROOT_DIR/.env" 2>/dev/null; set +a
./mvnw spring-boot:run &
BACKEND_PID=$!
cd "$ROOT_DIR"

echo -n "       等待后端就绪"
while true; do
  response=$(curl -s -o /dev/null -w "%{http_code}" --max-time 3 "$BACKEND_READY_URL" 2>/dev/null || echo "000")
  if [ "$response" != "000" ]; then
    echo ""
    echo "       后端就绪 (HTTP $response)"
    break
  fi
  echo -n "."
  sleep 3
done

# 3. 启动前端
echo "[3/3] 启动前端 (Vite :5173)..."
cd "$ROOT_DIR/frontend"
npm run dev -- --open &
FRONTEND_PID=$!
cd "$ROOT_DIR"

echo ""
echo "=========================================="
echo "  所有服务已启动"
echo "  前端:  http://localhost:5173"
echo "  后端:  http://localhost:8081"
echo "  MinIO: http://localhost:9001"
echo "=========================================="
echo "按 Ctrl+C 停止所有服务"
echo ""

wait
