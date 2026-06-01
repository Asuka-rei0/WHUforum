# WHUforum

WHUforum 是基于 OpenIsle 演进的校园论坛系统，面向武汉大学校园社区场景，提供帖子发布、评论互动、搜索、通知、校园身份认证、内容审核、二手市场等能力。项目采用前后端分离和多服务架构，适合本地开发、二次开发以及容器化部署。

## 项目组成

| 目录 | 说明 |
| --- | --- |
| `backend/` | Spring Boot 主后端，提供 REST API、JWT 鉴权、MySQL/JPA、Redis、RabbitMQ、OpenSearch、OpenAPI 等能力 |
| `frontend_nuxt/` | Nuxt 3 前端，负责页面渲染、用户交互、OAuth/CAS 回调、WebSocket 连接 |
| `websocket_service/` | 独立 WebSocket/STOMP 服务，消费 RabbitMQ 通知并向前端实时推送 |
| `mcp/` | Python MCP Server，将搜索、发帖、回复、通知等能力封装为 MCP 工具 |
| `docs/` | Fumadocs 文档站与 OpenAPI 文档生成逻辑 |
| `docker/` | Docker Compose、本地依赖服务与容器化启动配置 |
| `deploy/` | 生产与预发部署脚本 |
| `bots/` | 定时 Bot 脚本 |

## 技术栈

- 后端：Java 17、Spring Boot 3、Spring Security、Spring Data JPA、Maven
- 前端：Node.js 20+、Nuxt 3、Vue 3、STOMP/WebSocket
- 基础设施：MySQL 8、Redis 7、RabbitMQ、OpenSearch（可选）
- 工具与扩展：Python 3.11+、MCP、Fumadocs、Docker Compose

## 环境准备

本地启动前请先安装：

- Docker 与 Docker Compose
- JDK 17
- Maven 3.9+
- Node.js 20+ 与 npm
- Python 3.11+（仅运行 MCP 服务时需要）

复制环境变量示例文件：

```bash
cp .env.example .env
```

开发环境可以直接使用 `.env.example` 中的默认本地配置。涉及 JWT、CAS、COS、OAuth、WebPush、OpenAI 等外部服务时，请在 `.env` 中替换为自己的配置，避免提交真实密钥。

## 启动方式一：Docker Compose 一键启动

推荐第一次运行或需要完整联调时使用。该方式会启动 MySQL、Redis、RabbitMQ、后端、WebSocket 服务、前端开发服务和 MCP 服务。

```bash
docker compose --env-file .env -f docker/docker-compose.yaml --profile dev up --build
```

启动完成后访问：

- 前端页面：<http://localhost:3000>
- 后端 API：<http://localhost:8080>
- WebSocket 服务：<http://localhost:8082>
- MCP 服务：<http://localhost:8085>
- RabbitMQ 管理台：<http://localhost:15672>

停止服务：

```bash
docker compose --env-file .env -f docker/docker-compose.yaml --profile dev down
```

如需保留数据库、Redis、RabbitMQ 等数据卷，只执行上面的 `down` 即可；如需清空本地容器数据，请额外确认后再删除 Docker volume。

## 启动方式二：本地源码启动

适合需要调试 Java 或前端源码的开发场景。可以先用 Docker 启动基础依赖，再分别运行后端、WebSocket 和前端。

### 1. 启动基础依赖

```bash
docker compose --env-file .env -f docker/docker-compose.yaml --profile dev_local_backend up mysql redis rabbitmq websocket-service
```

### 2. 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端默认运行在 <http://localhost:8080>。

### 3. 启动前端

```bash
cd frontend_nuxt
npm install
npm run dev
```

前端默认运行在 <http://localhost:3000>。

### 4. 启动 WebSocket 服务（可选）

如果没有通过 Docker 启动 `websocket-service`，也可以本地运行：

```bash
cd websocket_service
mvn spring-boot:run
```

WebSocket 服务默认运行在 <http://localhost:8082>。

### 5. 启动 MCP 服务（可选）

```bash
cd mcp
python -m pip install -e .
openisle-mcp
```

MCP 服务默认连接后端 API，可通过 `.env` 中的 `OPENISLE_MCP_*` 配置调整监听地址、端口和请求超时。

## 常用命令

```bash
# 后端测试
cd backend
mvn test

# 前端构建
cd frontend_nuxt
npm run build

# WebSocket 服务测试
cd websocket_service
mvn test

# 文档生成与构建
cd docs
bun run generate
bun run build
```

## 默认端口

| 服务 | 默认端口 |
| --- | --- |
| 前端 Nuxt | `3000` |
| 后端 Spring Boot | `8080` |
| WebSocket 服务 | `8082` |
| MCP 服务 | `8085` |
| MySQL | `3306` |
| Redis | `6379` |
| RabbitMQ | `5672` |
| RabbitMQ 管理台 | `15672` |
| OpenSearch（可选） | `9200` |

## 开发约定

- 环境变量以根目录 `.env.example` 为基线，新增或改名时需同步各服务消费端。
- 后端、WebSocket、前端涉及鉴权或消息结构的改动，应同步确认 API、JWT、RabbitMQ 分片和 WebSocket 推送契约。
- 不提交 `.env`、密钥、令牌或生产凭证。
- 提交前按改动范围执行最小验证命令。

## 许可证

本项目沿用 MIT License，详见 [LICENSE](LICENSE)。
