# 水务监测分析系统 - 腾讯云服务器部署指南

本项目采用 **Docker Compose** 一键部署，包含 5 个服务：
`mysql(5.7)`、`redis(7)`、`ai-service(FastAPI)`、`backend(Spring Boot)`、`frontend(Nginx)`。

> 前置：一台腾讯云 CVM 服务器，推荐 **Ubuntu 22.04**；已在控制台弹出安全组并放行端口。

***

## 一、腾讯云控制台准备

### 1. 购买/选择云服务器

* 系统镜像选择 **Ubuntu 22.04 LTS**（或 20.04）。

* 按需选择带宽与配置（本项目单机演示 2C4G 即可；压测/大数据量建议 4C8G+）。

### 2. 配置安全组（放行端口）

在 **云服务器 → 安全组 → 入站规则** 中放行：

| 端口/协议      | 用途           | 建议          |
| ---------- | ------------ | ----------- |
| 22 / TCP   | SSH 登录       | 必须          |
| 80 / TCP   | 前端访问入口       | 必须          |
| 8080 / TCP | 后端 REST（调试用） | 建议          |
| 8000 / TCP | AI 微服务（调试用）  | 建议          |
| 3306 / TCP | MySQL        | **不建议公网开放** |
| 6379 / TCP | Redis        | **不建议公网开放** |

> MySQL/Redis 仅在容器内互访即可，未必要暴露公网。若确需远程连接数据库，请仅对 `来源=我的IP` 放行。

***

## 二、登录服务器并安装 Docker

1. 使用控制台网页登录，或本机终端 SSH：

   ```bash
   ssh root@<你的公网IP>
   ```

   首次登录按提示设置 root 密码或使用密钥。

2. 安装 Docker 与 Compose 插件（Ubuntu，root 用户）：

   ```bash
   apt-get update
   apt-get install -y ca-certificates curl gnupg
   install -m 0755 -d /etc/apt/keyrings
   curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
   chmod a+r /etc/apt/keyrings/docker.gpg

   echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo $VERSION_CODENAME) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null

   apt-get update
   apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
   # 验证
   docker --version
   docker compose version
   ```

3. 如需让非 root 用户免 sudo 使用 docker（可选）：

   ```bash
   usermod -aG docker $USER   # 重新登录生效
   ```

***

## 三、上传项目代码

任选一种方式将整个项目上传到服务器：

**方式 A：git（推荐，之后升级方便）**

```bash
cd /opt
git clone <你的仓库地址> shuiwu
```

**方式 B：scp / SFTP（无仓库时）**
在本机项目目录外执行：

```bash
# 把 shuiwu 目录整个传到服务器 /opt
scp -r ./shuiwu root@<你的公网IP>:/opt/
```

***

## 四、一键启动

```bash
cd /opt/shuiwu/deploy
bash start.sh
```

`start.sh` 会自动：构建镜像 → 初始化数据库（首次执行 `sql/shuiwu.sql`）→ 启动全部服务。

若需手动操作，等价命令：

```bash
docker compose up -d --build
docker compose ps          # 查看状态
docker compose logs -f backend
```

> 首次构建需联网拉取基础镜像与 Maven/NPM 依赖，耗时 5\~10 分钟，属正常现象。

***

## 五、访问与验证

| 服务    | 地址                        |
| ----- | ------------------------- |
| 管理后台  | `http://<公网IP>/`          |
| 可视化大屏 | `http://<公网IP>/bigscreen` |
| 后端接口  | `http://<公网IP>:8080/api`  |
| 默认账号  | `admin / 123456`          |

验证命令：

```bash
curl http://127.0.0.1/                      # 前端返回 HTML
curl -X POST http://127.0.0.1/api/auth/login -H "Content-Type: application/json" -d '{"username":"admin","password":"123456"}'
```

***

## 六、日常运维

```bash
cd /opt/shuiwu/deploy

bash stop.sh                     # 停止服务(保留数据)
docker compose down -v           # 停止并删除数据卷(谨慎)
docker compose restart backend   # 仅重启后端
docker compose logs -f frontend  # 查看某服务日志

# 有代码更新后重新发布
git pull                         # 若用 git 方式
docker compose up -d --build
```

数据持久化到命名卷：`mysql-data`、`redis-data`、`reports-data`。
执行 `docker compose down` 不会删除数据；`-v` 才会。

***

## 七、常见问题

* **访问 80 不通**：检查安全组是否放行 80，以及系统防火墙 `ufw status`。

* **后端反复重启**：`docker compose logs -f backend` 看是否数据库/Redis 未就绪——compose 已配置等待 mysql/redis 健康后再启动，等待即可。

* **大屏 WebSocket 断连**：确认 `/ws` 代理正常，nginx 配置已含 Upgrade 头。

* **改数据库密码**：需同时修改 `docker-compose.yml` 中 `MYSQL_ROOT_PASSWORD` 与后端环境变量 `SPRING_DATASOURCE_PASSWORD`，并重建（彻底清理注意 `-v`）。

