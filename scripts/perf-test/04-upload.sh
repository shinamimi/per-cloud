#!/bin/bash
# 文件上传测试
set -e

BACKEND="http://127.0.0.1:8081"
TOKEN="${TOKEN:-}"
RESULTS_DIR="scripts/perf-test/data"

if [ -z "$TOKEN" ]; then
    echo "错误: 请设置 TOKEN 环境变量"
    exit 1
fi

mkdir -p "$RESULTS_DIR"

echo "=== 文件上传测试 ==="

# 测试文件大小
declare -a FILE_SIZES=("10MB" "100MB" "1GB")
declare -A FILE_SIZES_BYTES=(
    ["10MB"]="10485760"
    ["100MB"]="104857600"
    ["1GB"]="1073741824"
)

# 生成测试文件
echo "1. 生成测试文件..."
for size in "${FILE_SIZES[@]}"; do
    FILE="/tmp/test_${size}.bin"
    if [ ! -f "$FILE" ]; then
        echo "  生成 $FILE..."
        dd if=/dev/urandom of="$FILE" bs=1M count=$(( ${FILE_SIZES_BYTES[$size]} / 1048576 ))
    else
        echo "  $FILE 已存在"
    fi
done

# 测试函数
test_upload() {
    local FILE_SIZE=$1
    local CONCURRENCY=$2
    local CHUNK_SIZE=10485760  # 10MB
    
    echo ""
    echo "--- 上传测试: $FILE_SIZE, 并发 $CONCURRENCY ---"
    
    local FILE="/tmp/test_${FILE_SIZE}.bin"
    local TOTAL_SIZE=${FILE_SIZES_BYTES[$FILE_SIZE]}
    local CHUNK_COUNT=$(( (TOTAL_SIZE + CHUNK_SIZE - 1) / CHUNK_SIZE ))
    
    echo "  文件大小: $FILE_SIZE"
    echo "  分片数量: $CHUNK_COUNT"
    echo "  并发数: $CONCURRENCY"
    
    # 初始化上传
    echo "  初始化上传..."
    local INIT_RESP=$(curl -s ${BACKEND}/api/files/upload/init \
      -H "Authorization: Bearer $TOKEN" \
      -H "Content-Type: application/json" \
      -d "{\"fileName\":\"test_${FILE_SIZE}.bin\",\"fileSize\":$TOTAL_SIZE,\"parentId\":0}")
    
    local UPLOAD_ID=$(echo $INIT_RESP | jq -r '.data.uploadId')
    if [ -z "$UPLOAD_ID" ] || [ "$UPLOAD_ID" = "null" ]; then
        echo "  错误: 初始化上传失败"
        echo "  响应: $INIT_RESP"
        return 1
    fi
    echo "  Upload ID: $UPLOAD_ID"
    
    # 分片上传（并发）
    echo "  上传分片..."
    local START_TIME=$(date +%s%N)
    
    # 切割文件
    split -b $CHUNK_SIZE -d "$FILE" "/tmp/chunk_${UPLOAD_ID}_"
    
    # 并发上传
    for i in $(seq 0 $((CHUNK_COUNT - 1))); do
        local CHUNK_FILE=$(printf "/tmp/chunk_%s_%02d" "$UPLOAD_ID" $i)
        local SEQ=$((i + 1))
        
        (
            curl -s ${BACKEND}/api/files/upload/chunk \
              -H "Authorization: Bearer $TOKEN" \
              -F "file=@${CHUNK_FILE}" \
              -F "uploadId=$UPLOAD_ID" \
              -F "seq=$SEQ" > /dev/null
        ) &
        
        # 控制并发数
        if [ $((i % CONCURRENCY)) -eq $((CONCURRENCY - 1)) ]; then
            wait
        fi
    done
    wait
    
    local END_TIME=$(date +%s%N)
    local UPLOAD_DURATION=$(( (END_TIME - START_TIME) / 1000000 ))
    echo "  分片上传耗时: ${UPLOAD_DURATION}ms"
    
    # 计算 Hash
    echo "  计算 SHA-256 Hash..."
    local HASH_START=$(date +%s%N)
    local FILE_HASH=$(sha256sum "$FILE" | awk '{print $1}')
    local HASH_END=$(date +%s%N)
    local HASH_DURATION=$(( (HASH_END - HASH_START) / 1000000 ))
    echo "  Hash 计算耗时: ${HASH_DURATION}ms"
    echo "  Hash: $FILE_HASH"
    
    # 合并文件
    echo "  合并文件..."
    local MERGE_START=$(date +%s%N)
    local MERGE_RESP=$(curl -s ${BACKEND}/api/files/upload/merge \
      -H "Authorization: Bearer $TOKEN" \
      -H "Content-Type: application/json" \
      -d "{\"uploadId\":\"$UPLOAD_ID\",\"fileName\":\"test_${FILE_SIZE}.bin\",\"fileSize\":$TOTAL_SIZE,\"fileHash\":\"$FILE_HASH\"}")
    
    local MERGE_END=$(date +%s%N)
    local MERGE_DURATION=$(( (MERGE_END - MERGE_START) / 1000000 ))
    echo "  合并耗时: ${MERGE_DURATION}ms"
    echo "  合并响应: $MERGE_RESP"
    
    # 计算总速度
    local TOTAL_DURATION=$((UPLOAD_DURATION + HASH_DURATION + MERGE_DURATION))
    local SPEED_MBPS=$(echo "scale=2; $TOTAL_SIZE / 1048576 / ($TOTAL_DURATION / 1000)" | bc)
    echo "  总耗时: ${TOTAL_DURATION}ms"
    echo "  上传速度: ${SPEED_MBPS} MB/s"
    
    # 记录结果
    echo "${FILE_SIZE},${CONCURRENCY},${CHUNK_COUNT},${UPLOAD_DURATION},${HASH_DURATION},${MERGE_DURATION},${TOTAL_DURATION},${SPEED_MBPS}" \
      >> "$RESULTS_DIR/upload_results.csv"
    
    # 清理临时文件
    rm -f /tmp/chunk_${UPLOAD_ID}_*
    
    echo "  冷却 5 分钟..."
    sleep 300
}

# 初始化结果文件
echo "文件大小,并发数,分片数,上传耗时(ms),Hash耗时(ms),合并耗时(ms),总耗时(ms),速度(MB/s)" \
  > "$RESULTS_DIR/upload_results.csv"

# 测试场景
for SIZE in "10MB" "100MB"; do
    for CONC in 1 3 5; do
        test_upload "$SIZE" "$CONC"
    done
done

# 大文件测试（可选）
read -p "是否测试 1GB 文件？(y/n): " TEST_1GB
if [ "$TEST_1GB" = "y" ]; then
    for CONC in 1 5 10; do
        test_upload "1GB" "$CONC"
    done
fi

echo ""
echo "=== 上传测试完成 ==="
echo "结果保存在: $RESULTS_DIR/upload_results.csv"
