#!/bin/bash
# 系统资源监控脚本
set -e

RESULTS_DIR="scripts/perf-test/data"
DURATION=${1:-300}  # 默认监控 5 分钟
INTERVAL=5  # 每 5 秒采集一次

mkdir -p "$RESULTS_DIR"

echo "=== 系统资源监控 ==="
echo "监控时长: ${DURATION}秒"
echo "采集间隔: ${INTERVAL}秒"
echo ""

# 初始化结果文件
echo "时间,CPU用户态,CPU系统态,IO等待,内存RSS(MB),内存Heap(MB),磁盘读(MB/s),磁盘写(MB/s),网络入(Mbps),网络出(Mbps)" \
  > "$RESULTS_DIR/system_monitor.csv"

# 监控函数
monitor() {
    local START=$(date +%s)
    local END=$((START + DURATION))
    
    while [ $(date +%s) -lt $END ]; do
        local TIMESTAMP=$(date "+%Y-%m-%d %H:%M:%S")
        
        # CPU 使用率
        CPU_STATS=$(top -bn1 | grep "Cpu(s)" | awk '{print $2, $4, $6}')
        CPU_USER=$(echo $CPU_STATS | awk '{print $1}')
        CPU_SYSTEM=$(echo $CPU_STATS | awk '{print $2}')
        CPU_IOWAIT=$(echo $CPU_STATS | awk '{print $3}')
        
        # 内存使用
        MEM_RSS=$(docker stats --no-stream --format "{{.MemUsage}}" cloud-backend | awk '{print $1}' | sed 's/MiB//')
        MEM_HEAP=$(docker exec cloud-backend jstat -gc 1 2>/dev/null | tail -1 | awk '{print $3}' | awk '{printf "%.1f", $1/1024}')
        
        # 磁盘 I/O
        DISK_IO=$(iostat -x 1 1 | grep "sda\|vda\|nvme" | awk '{print $3, $4}' | head -1)
        DISK_READ=$(echo $DISK_IO | awk '{print $1}')
        DISK_WRITE=$(echo $DISK_IO | awk '{print $2}')
        
        # 网络流量
        NET_IO=$(cat /proc/net/dev | grep -E "eth0|ens" | awk '{print $2, $10}' | head -1)
        NET_IN=$(echo $NET_IO | awk '{printf "%.2f", $1/1024/1024}')
        NET_OUT=$(echo $NET_IO | awk '{printf "%.2f", $10/1024/1024}')
        
        # 记录数据
        echo "${TIMESTAMP},${CPU_USER},${CPU_SYSTEM},${CPU_IOWAIT},${MEM_RSS},${MEM_HEAP},${DISK_READ},${DISK_WRITE},${NET_IN},${NET_OUT}" \
          >> "$RESULTS_DIR/system_monitor.csv"
        
        # 显示当前状态
        echo -ne "\r[${TIMESTAMP}] CPU: ${CPU_USER}% 用户, ${CPU_SYSTEM}% 系统, ${CPU_IOWAIT}% IO等待 | 内存: ${MEM_RSS}MB RSS, ${MEM_HEAP}MB Heap"
        
        sleep $INTERVAL
    done
    
    echo ""
}

# JVM 监控函数
monitor_jvm() {
    local START=$(date +%s)
    local END=$((START + DURATION))
    
    while [ $(date +%s) -lt $END ]; do
        local TIMESTAMP=$(date "+%Y-%m-%d %H:%M:%S")
        
        # 获取 JVM PID
        local JVM_PID=$(docker exec cloud-backend jps 2>/dev/null | grep app | awk '{print $1}')
        
        if [ -n "$JVM_PID" ]; then
            # GC 统计
            local GC_STATS=$(docker exec cloud-backend jstat -gc $JVM_PID 2>/dev/null | tail -1)
            
            if [ -n "$GC_STATS" ]; then
                local YOUNG_GC=$(echo $GC_STATS | awk '{print $7}')
                local FULL_GC=$(echo $GC_STATS | awk '{print $8}')
                local YOUNG_GC_TIME=$(echo $GC_STATS | awk '{print $9}')
                local FULL_GC_TIME=$(echo $GC_STATS | awk '{print $10}')
                
                # 记录到单独文件
                echo "${TIMESTAMP},${YOUNG_GC},${FULL_GC},${YOUNG_GC_TIME},${FULL_GC_TIME}" \
                  >> "$RESULTS_DIR/jvm_monitor.csv"
            fi
        fi
        
        sleep 30  # 每 30 秒采集一次 JVM
    done
}

# 初始化 JVM 监控文件
echo "时间,Young GC次数,Full GC次数,Young GC时间(ms),Full GC时间(ms)" \
  > "$RESULTS_DIR/jvm_monitor.csv"

# 后台启动 JVM 监控
monitor_jvm &
JVM_PID=$!

# 前台执行系统监控
monitor

# 停止 JVM 监控
kill $JVM_PID 2>/dev/null || true

echo ""
echo "=== 监控完成 ==="
echo "系统监控数据: $RESULTS_DIR/system_monitor.csv"
echo "JVM 监控数据: $RESULTS_DIR/jvm_monitor.csv"
