# =====================================================================
# 水务监测分析系统 - 前端镜像 (Vue3 构建 + Nginx 托管)
# 多阶段构建：Node 构建 dist -> Nginx 托管并反向代理后端
# 注意：该 Dockerfile 的构建上下文必须为项目根目录（shuiwu/），
#       以便 COPY 到 deploy/docker/nginx.conf。请在 docker-compose.yml
#       中设置 context: ..  `dockerfile: frontend/Dockerfile`。
# =====================================================================
FROM node:20-alpine AS build
WORKDIR /app

COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --no-audit --no-fund

COPY frontend/ .
RUN npm run build

# ---------- 运行阶段 ----------
FROM nginx:1.25-alpine

# 覆盖默认配置（反向代理 /api、/ws 到 backend）
COPY deploy/docker/nginx.conf /etc/nginx/conf.d/default.conf

# 拷贝构建产物到 Nginx html 目录
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]