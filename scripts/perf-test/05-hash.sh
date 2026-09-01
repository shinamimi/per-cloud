#!/bin/bash
# Hash 计算性能测试
set -e

RESULTS_DIR="scripts/perf-test/data"

mkdir -p "$RESULTS_DIR"

echo "=== Hash 计算性能测试 ==="

# 测试文件
declare -a FILE_SIZES=("10MB" "100MB" "1GB" "5GB")
declare -A FILE_SIZES_BYTES=(
    ["10MB"]="10485760"
    ["100MB"]="104857600"
    ["1GB"]="1073741824"
    ["5GB"]="5368709120"
)

# 生成测试文件
echo "1. 生成测试文件..."
for size in "${FILE_SIZES[@]}"; do
    FILE="/tmp/test_${size}.bin"
    if [ ! -f "$FILE" ]; then
        echo "  生成 $FILE..."
        dd if=/dev/urandom of="$FILE" bs=1M count=$(( ${FILE_SIZES_BYTES[$size]} / 1048576 )) 2>/dev/null
    else
        echo "  $FILE 已存在"
    fi
done

# 测试函数
test_hash() {
    local FILE_SIZE=$1
    local FILE="/tmp/test_${FILE_SIZE}.bin"
    
    if [ ! -f "$FILE" ]; then
        echo "  文件不存在: $FILE"
        return 1
    fi
    
    echo ""
    echo "--- Hash 测试: $FILE_SIZE ---"
    
    # 清除文件系统缓存
    echo "  清除缓存..."
    sync
    echo 3 > /proc/sys/vm/drop_caches 2>/dev/null || true
    
    # 测试 SHA-256
    echo "  测试 SHA-256..."
    local START=$(date +%s%N)
    local HASH=$(sha256sum "$FILE" | awk '{print $1}')
    local END=$(date +%s%N)
    local DURATION=$(( (END - START) / 1000000 ))
    local DURATION_S=$(echo "scale=2; $DURATION / 1000" | bc)
    
    echo "  Hash: $HASH"
    echo "  耗时: ${DURATION_S}秒 (${DURATION}ms)"
    
    # 计算速度
    local FILE_SIZE_BYTES=$(stat -f%z "$FILE" 2>/dev/null || stat -c%s "$FILE")
    local SPEED_MBPS=$(echo "scale=2; $FILE_SIZE_BYTES / 1048576 / ($DURATION / 1000)" | bc)
    echo "  速度: ${SPEED_MBPS} MB/s"
    
    # 记录结果
    echo "${FILE_SIZE},${FILE_SIZE_BYTES},${DURATION},${SPEED_MBPS},${HASH}" \
      >> "$RESULTS_DIR/hash_results.csv"
}

# 初始化结果文件
echo "文件大小,文件大小(bytes),耗时(ms),速度(MB/s),Hash" \
  > "$RESULTS_DIR/hash_results.csv"

# 执行测试
for size in "10MB" "100MB" "1GB"; do
    test_hash "$size"
done

# 大文件测试（可选）
read -p "是否测试 5GB 文件？(y/n): " TEST_5GB
if [ "$TEST_5GB" = "y" ]; then
    test_hash "5GB"
fi

echo ""
echo "=== Hash 测试完成 ==="
echo "结果保存在: $RESULTS_DIR/hash_results.csv"
