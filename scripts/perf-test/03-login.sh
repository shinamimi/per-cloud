#!/bin/bash
# 登录认证测试
set -e

BACKEND="http://127.0.0.1:8081"
RESULTS_DIR="scripts/perf-test/data"

mkdir -p "$RESULTS_DIR"

echo "=== 登录认证测试 ==="

# 场景 3.1: 单用户基线
echo ""
echo "--- 场景 3.1: 单用户基线 ---"
wrk -t1 -c1 -d30s --latency \
  -s scripts/perf-test/lua/login.lua \
  ${BACKEND}/api/auth/login \
  2>&1 | tee "$RESULTS_DIR/login_single.txt"

echo "冷却 5 分钟..."
sleep 300

# 场景 3.2: 50 并发
echo ""
echo "--- 场景 3.2: 50 并发 ---"
wrk -t2 -c50 -d60s --latency \
  -s scripts/perf-test/lua/login.lua \
  ${BACKEND}/api/auth/login \
  2>&1 | tee "$RESULTS_DIR/login_50c.txt"

echo "冷却 5 分钟..."
sleep 300

# 场景 3.3: 100 并发
echo ""
echo "--- 场景 3.3: 100 并发 ---"
wrk -t2 -c100 -d60s --latency \
  -s scripts/perf-test/lua/login.lua \
  ${BACKEND}/api/auth/login \
  2>&1 | tee "$RESULTS_DIR/login_100c.txt"

echo ""
echo "=== 登录测试完成 ==="
