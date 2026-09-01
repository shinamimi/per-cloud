#!/bin/bash
# 文件列表查询测试
set -e

BACKEND="http://127.0.0.1:8081"
TOKEN="${TOKEN:-}"
RESULTS_DIR="scripts/perf-test/data"

if [ -z "$TOKEN" ]; then
    echo "错误: 请设置 TOKEN 环境变量"
    exit 1
fi

mkdir -p "$RESULTS_DIR"

echo "=== 文件列表查询测试 ==="

# 测试场景
declare -A SCENARIOS=(
    ["100_files"]="100"
    ["1000_files"]="1000"
    ["10000_files"]="10000"
)

for scenario in "100_files" "1000_files" "10000_files"; do
    echo ""
    echo "--- 场景: $scenario ---"
    
    # 获取目录 ID（需要预先创建）
    read -p "请输入 $scenario 目录的 ID: " DIR_ID
    
    if [ -z "$DIR_ID" ]; then
        echo "跳过 $scenario（未提供目录 ID）"
        continue
    fi
    
    echo "测试目录 ID: $DIR_ID"
    echo "开始测试..."
    
    # 运行 wrk 测试
    wrk -t2 -c50 -d60s --latency \
      -H "Authorization: Bearer $TOKEN" \
      "${BACKEND}/api/files?parentId=${DIR_ID}&size=20" \
      2>&1 | tee "$RESULTS_DIR/filelist_${scenario}.txt"
    
    echo "结果已保存到: $RESULTS_DIR/filelist_${scenario}.txt"
    
    # 冷却 5 分钟
    echo "冷却 5 分钟..."
    sleep 300
done

echo ""
echo "=== 文件列表测试完成 ==="
