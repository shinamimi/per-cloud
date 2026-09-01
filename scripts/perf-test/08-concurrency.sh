#!/bin/bash
# 并发一致性测试
set -e

BACKEND="http://127.0.0.1:8081"
TOKEN="${TOKEN:-}"
RESULTS_DIR="scripts/perf-test/data"

if [ -z "$TOKEN" ]; then
    echo "错误: 请设置 TOKEN 环境变量"
    exit 1
fi

mkdir -p "$RESULTS_DIR"

echo "=== 并发一致性测试 ==="

# 8.1 引用计数一致性
echo ""
echo "--- 8.1 引用计数一致性测试 ---"
echo "测试场景：100 线程同时删除同一文件"
echo ""

read -p "请输入要测试的文件 ID: " FILE_ID
read -p "请输入文件 Hash: " FILE_HASH

if [ -z "$FILE_ID" ] || [ -z "$FILE_HASH" ]; then
    echo "错误: 请提供文件 ID 和 Hash"
    exit 1
fi

echo "文件 ID: $FILE_ID"
echo "文件 Hash: $FILE_HASH"

# 检查初始 ref_count
echo "初始 ref_count："
docker exec cloud-mysql mysql -uroot -proot cloud -e "
SELECT ref_count FROM t_file_hash WHERE file_hash = '$FILE_HASH';
" 2>/dev/null

# 100 线程同时删除
echo "启动 100 线程同时删除..."
for i in $(seq 1 100); do
    (
        curl -s -X DELETE "${BACKEND}/api/files/${FILE_ID}" \
          -H "Authorization: Bearer $TOKEN" > /dev/null 2>&1
    ) &
done
wait

echo "删除完成"

# 检查最终 ref_count
echo "最终 ref_count："
docker exec cloud-mysql mysql -uroot -proot cloud -e "
SELECT ref_count FROM t_file_hash WHERE file_hash = '$FILE_HASH';
" 2>/dev/null

echo "验证：ref_count 应为 0，记录应被清理"

# 8.2 秒传并发
echo ""
echo "--- 8.2 秒传并发测试 ---"
echo "测试场景：10 用户同时上传相同文件"
echo ""

read -p "请输入测试文件路径: " TEST_FILE
read -p "请输入目录 ID: " DIR_ID

if [ ! -f "$TEST_FILE" ]; then
    echo "错误: 文件不存在"
    exit 1
fi

# 计算 Hash
FILE_HASH=$(sha256sum "$TEST_FILE" | awk '{print $1}')
FILE_SIZE=$(stat -f%z "$TEST_FILE" 2>/dev/null || stat -c%s "$TEST_FILE")

echo "文件 Hash: $FILE_HASH"
echo "文件大小: $FILE_SIZE bytes"

# 10 用户同时上传
echo "启动 10 用户同时上传..."
for user in $(seq 1 10); do
    (
        # 获取 Token（使用不同用户）
        USER_TOKEN=$(curl -s ${BACKEND}/api/auth/login -X POST \
          -H "Content-Type: application/json" \
          -d "{\"username\":\"user${user}\",\"password\":\"123456\"}" | jq -r '.data.token')
        
        if [ -n "$USER_TOKEN" ] && [ "$USER_TOKEN" != "null" ]; then
            # 尝试秒传
            curl -s -X POST "${BACKEND}/api/files/upload/sec" \
              -H "Authorization: Bearer $USER_TOKEN" \
              -H "Content-Type: application/json" \
              -d "{\"fileName\":\"test.bin\",\"fileSize\":$FILE_SIZE,\"fileHash\":\"$FILE_HASH\",\"parentId\":$DIR_ID}" > /dev/null 2>&1
        fi
    ) &
done
wait

echo "上传完成"

# 检查结果
echo "t_file_hash 记录数："
docker exec cloud-mysql mysql -uroot -proot cloud -e "
SELECT COUNT(*) as count, ref_count FROM t_file_hash WHERE file_hash = '$FILE_HASH' GROUP BY ref_count;
" 2>/dev/null

echo "验证：应只有 1 条记录，ref_count 应为上传成功的用户数"

# 8.3 权限攻击
echo ""
echo "--- 8.3 权限攻击测试 ---"
echo "测试场景：用户 A 访问用户 B 的文件"
echo ""

read -p "请输入用户 A 的 Token: " TOKEN_A
read -p "请输入用户 B 的文件 ID: " FILE_ID_B

if [ -z "$TOKEN_A" ] || [ -z "$FILE_ID_B" ]; then
    echo "错误: 请提供用户 A 的 Token 和用户 B 的文件 ID"
    exit 1
fi

echo "尝试越权访问..."
RESP=$(curl -s -o /dev/null -w "%{http_code}" \
  "${BACKEND}/api/files/${FILE_ID_B}" \
  -H "Authorization: Bearer $TOKEN_A")

echo "响应状态码: $RESP"
if [ "$RESP" = "403" ] || [ "$RESP" = "404" ]; then
    echo "验证通过: 正确拒绝越权访问"
else
    echo "验证失败: 可能存在越权漏洞"
fi

echo ""
echo "=== 并发一致性测试完成 ==="
