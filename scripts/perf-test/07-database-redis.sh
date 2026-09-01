#!/bin/bash
# 数据库和 Redis 性能测试
set -e

RESULTS_DIR="scripts/perf-test/data"

mkdir -p "$RESULTS_DIR"

echo "=== 数据库和 Redis 性能测试 ==="

# 7.1 MySQL EXPLAIN
echo ""
echo "--- 7.1 MySQL EXPLAIN ---"
echo "文件列表查询执行计划："
docker exec cloud-mysql mysql -uroot -proot cloud -e "
EXPLAIN SELECT * FROM t_file 
WHERE user_id = 1 AND team_id = 0 AND parent_id = 0 AND status != 0
ORDER BY type DESC, created_at DESC
LIMIT 0, 20;
" 2>/dev/null | tee "$RESULTS_DIR/mysql_explain.txt"

echo ""
echo "慢查询状态："
docker exec cloud-mysql mysql -uroot -proot -e "SHOW STATUS LIKE 'Slow_queries';" 2>/dev/null

echo ""
echo "表索引信息："
docker exec cloud-mysql mysql -uroot -proot cloud -e "SHOW INDEX FROM t_file;" 2>/dev/null

# 7.2 Redis 基准测试
echo ""
echo "--- 7.2 Redis 基准测试 ---"
echo "Redis GET/SET 基准测试："
docker exec cloud-redis redis-benchmark -n 10000 -c 50 -q 2>/dev/null | tee "$RESULTS_DIR/redis_benchmark.txt"

echo ""
echo "Redis 信息："
docker exec cloud-redis redis-cli INFO memory 2>/dev/null | grep -E "used_memory_human|maxmemory_human"

# 7.3 慢查询日志
echo ""
echo "--- 7.3 慢查询日志 ---"
echo "当前慢查询数量："
docker exec cloud-mysql mysql -uroot -proot -e "SHOW STATUS LIKE 'Slow_queries';" 2>/dev/null

echo ""
echo "=== 数据库和 Redis 测试完成 ==="
