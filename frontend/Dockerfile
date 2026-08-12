# =============================================================================
# Cloud 云盘 — 前端生产镜像（Nginx 静态托管 + API/WS 反代）
# 构建前提：先执行 npm run build 生成 dist/
# 构建上下文：frontend/（由 docker-compose.prod.yml 指定）
#
# 设计思路：
# 1. 单层 Nginx 镜像：dist 静态资源直接托管，无需 Node 运行时
# 2. 同域反代 /api 与 /ws/progress，前端无跨域、无需 VITE_API_BASE_URL/WS_URL
# 3. MinIO presigned URL 由后端生成公网地址，不走 Nginx 反代（见 nginx.conf 注释）
# =============================================================================
FROM nginx:1.27-alpine

COPY dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]