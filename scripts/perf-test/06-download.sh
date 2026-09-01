#!/bin/bash
# 下载性能测试
set -e

BACKEND="http://127.0.0.1:8081"
TOKEN="${TOKEN:-}"
RESULTS_DIR="scripts/perf-test/data"

if [ -z "$TOKEN" ]; then
    echo "错误: 请设置 TOKEN 环境变量"
    exit 1
fi

mkdir -p "$RESULTS_DIR"

echo "=== 下载性能测试 ==="

# 测试场景
declare -A SCENARIOS=(
    ["1MB"]="1048576"
    ["100MB"]="104857600"
    ["1GB"]="1073741824"
)

test_download() {
    local FILE_SIZE=$1
    local CONCURRENCY=$2
    
    echo ""
    echo "--- 下载测试: $FILE_SIZE, 并发 $CONCURRENCY ---"
    
    # 获取文件 ID
    read -p "请输入 $FILE_SIZE 文件的 ID: " FILE_ID
    
    if [ -z "$FILE_ID" ]; then
        echo "跳过（未提供文件 ID）"
        return 1
    fi
    
    echo "文件 ID: $FILE_ID"
    echo "开始测试..."
    
    # 运行 wrk 测试
    wrk -t2 -c${CONCURRENCY} -d60s --latency \
      -H "Authorization: Bearer $TOKEN" \
      "${BACKEND}/api/files/${FILE_ID}/download" \
      2>&1 | tee "$RESULTS_DIR/download_${FILE_SIZE}_${CONCURRENCY}c.txt"
    
    echo "结果已保存到: $RESULTS_DIR/download_${FILE_SIZE}_${CONCURRENCY}c.txt"
    
    # 冷却 5 分钟
    echo "冷却 5 分钟..."
    sleep 300
}

# 场景 6.1: 小文件下载
echo ""
echo "=== 场景 6.1: 小文件下载 (1MB, 50并发) ==="
test_download "1MB" 50

# 场景 6.2: 中文件下载
echo ""
echo "=== 场景 6.2: 中文件下载 (100MB, 20并发) ==="
test_download "100MB" 20

# 场景 6.3: 大文件下载
echo ""
echo "=== 场景 6.3: 大文件下载 (1GB, 10并发) ==="
test_download "1GB" 10

echo ""
echo "=== 下载测试完成 ==="
