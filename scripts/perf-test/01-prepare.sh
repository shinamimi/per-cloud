#!/bin/bash
# 环境准备脚本
set -e

SERVER="101.35.233.30"
BACKEND="http://127.0.0.1:8081"

echo "=== Cloud 2C2G 全面性能测试 - 环境准备 ==="

# 1. 检查 wrk
echo "1. 检查 wrk..."
if ! command -v wrk &> /dev/null; then
    echo "错误: wrk 未安装"
    exit 1
fi
wrk --version

# 2. 检查服务
echo "2. 检查服务状态..."
curl -s ${BACKEND}/actuator/health || echo "警告: 后端未就绪"

# 3. 登录获取 Token
echo "3. 登录获取 Token..."
TOKEN=$(curl -s ${BACKEND}/api/auth/login -X POST \
  -H "Content-Type: application/json" \
  -d '{"username":"bench","password":"bench123"}' | jq -r '.data.token')

if [ -z "$TOKEN" ] || [ "$TOKEN" = "null" ]; then
    echo "错误: 登录失败"
    exit 1
fi
echo "Token: ${TOKEN:0:20}..."

# 4. 预热 JVM（30 分钟）
echo "4. 预热 JVM（30 分钟）..."
for i in $(seq 1 100); do
  curl -s ${BACKEND}/api/files?parentId=0 \
    -H "Authorization: Bearer $TOKEN" > /dev/null
  if [ $((i % 10)) -eq 0 ]; then
    echo "  预热进度: $i/100"
  fi
done

# 5. 验证 Token
echo "5. 验证 Token..."
curl -s ${BACKEND}/api/files?parentId=0 \
  -H "Authorization: Bearer $TOKEN" | jq '.code' || echo "错误: Token 无效"

echo "=== 环境准备完成 ==="
echo "Token: $TOKEN"
