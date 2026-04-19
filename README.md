# NovaLeap

NovaLeap 是一个面向学习、内容沉淀与社区互动的全栈项目，当前版本由 3 个应用组成：

- `nova-frontend`：面向普通用户的主站
- `nova-admin`：面向运营 / 管理员的后台
- `nova-backend`：统一 API、鉴权、AI 能力、数据存储与业务编排

当前仓库已经支持使用 Docker Compose 进行一键构建与部署，适合直接部署到单台 Linux 服务器。

## 当前版本能力

### 用户端

- 账号体系：注册、登录、游客登录、邮箱验证码、重置密码
- 首页与导航：主工作台、模块入口、访问统计上报
- 题库：分类筛选、随机抽题、答案查看、浏览计数
- 笔记：列表、我的笔记、详情、创建、编辑、删除、点赞、评论
- 简历分析：提交简历文本并获取 AI 流式分析
- AI 陪练：话题式聊天、历史记录、新建会话、清空历史
- 愿望墙：发布愿望、点赞、评论、审核后展示
- 排行榜：答题完成榜、小游戏积分榜
- 个人中心：个人资料、连续签到信息
- 法务页面：用户协议、隐私政策

### 管理端

- 管理员登录
- 数据总览
- 系统监控
- 访客记录查询
- 用户管理
- 题目管理
- 题目分类管理
- 题库导入与审核
- 笔记管理与审核
- 愿望墙内容审核、死信重试

### 后端

- JWT 无状态认证
- Spring Security 鉴权
- MySQL 持久化
- Redis 缓存与排行榜支持
- Spring AI 接入 OpenAI 兼容接口
- Resend 邮件发送
- Docker 镜像构建与容器化部署

## 技术栈

### 前端主站 `nova-frontend`

- Vue 3
- Vite
- Pinia
- Vue Router
- Tailwind CSS
- 部分 3D / 动效依赖：`three`、`@react-three/*`、`meshline`

### 管理端 `nova-admin`

- Vue 3
- Vite
- Pinia
- Vue Router
- Tailwind CSS

### 后端 `nova-backend`

- Java 17
- Spring Boot 3.2.5
- Spring Security
- MyBatis-Plus 3.5.5
- MySQL 8
- Redis 7
- Spring AI
- Resend Java SDK

## 仓库结构

```text
novaleap/
├─ docker-compose.yml
├─ .env.example
├─ nova-frontend/
│  ├─ src/
│  ├─ public/
│  ├─ Dockerfile
│  └─ nginx.conf
├─ nova-admin/
│  ├─ src/
│  ├─ Dockerfile
│  └─ nginx.conf
└─ nova-backend/
   ├─ src/
   ├─ sql/
   ├─ docs/
   ├─ Dockerfile
   ├─ pom.xml
   └─ novaleap_full.sql
```

## 运行方式

当前推荐使用 Docker Compose 部署整套服务。

### 默认服务

- `nova-mysql`
- `nova-redis`
- `nova-backend`
- `nova-frontend`
- `nova-admin`

### 默认端口

- 用户端：`80`
- 管理端：`8081`
- 后端 API：`8080`
- MySQL：`3306`
- Redis：`6379`

说明：

- 生产环境建议将 `BACKEND_BIND_IP`、`DATA_BIND_IP` 设为 `127.0.0.1`
- 如果前后端都部署在同一台服务器上，建议将 `FRONTEND_PORT`、`ADMIN_PORT` 改为本机端口，例如 `3000`、`3001`，再由系统 Nginx 统一接入 `80/443`

## 快速开始

### 1. 准备环境

你至少需要以下其一：

- Docker + Docker Compose Plugin
- 或本地开发环境：JDK 17、Node.js 18+、MySQL 8、Redis 7

### 2. 复制环境变量模板

```bash
cp .env.example .env
```

### 3. 填写关键变量

至少需要填写：

- `MYSQL_ROOT_PASSWORD`
- `MYSQL_PASSWORD`
- `SPRING_DATASOURCE_PASSWORD`
- `SPRING_AI_OPENAI_API_KEY`
- `NOVA_JWT_SECRET`

如果要启用邮箱功能，再填写：

- `NOVA_RESEND_API_KEY`
- `NOVA_RESEND_FROM`

如果要启用 Turnstile，再填写：

- `NOVA_TURNSTILE_ENABLED=true`
- `NOVA_TURNSTILE_SECRET`

### 4. 启动整套服务

```bash
docker compose up -d --build
```

### 5. 检查服务状态

```bash
docker compose ps
docker compose logs -f nova-backend
```

如果要在服务启动后做一轮接口级烟测，可以直接运行仓库自带脚本：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\full_smoke_test.ps1
```

如果后端不是跑在默认的 `http://localhost:8080`，或者管理员账号不是默认值，也可以显式传参：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\full_smoke_test.ps1 `
  -BaseUrl "http://127.0.0.1:8088" `
  -AdminUsername "admin" `
  -AdminPassword "your-admin-password"
```

### 6. 访问地址

- 主站：`http://<server-ip>/`
- 管理端：`http://<server-ip>:8081/`

## 生产部署建议

当前版本最适合单机部署，典型方案如下：

- `novaleap.xyz` 指向主站
- `admin.novaleap.xyz` 指向管理端
- 主机系统层使用 Nginx 反向代理
- Docker 内部运行 5 个服务
- MySQL / Redis / Backend 仅监听本机

建议的安全组策略：

- 放行：`22`、`80`、`443`
- 不对公网开放：`3306`、`6379`、`8080`

推荐的上线流程：

1. 将仓库上传到 GitHub
2. 服务器 `git clone`
3. 填写 `.env`
4. 执行 `docker compose up -d --build`
5. 配置系统 Nginx 与 HTTPS

## 本地开发

### 后端

```bash
cd nova-backend
mvn spring-boot:run
```

### 主站

```bash
cd nova-frontend
npm install
npm run dev
```

### 管理端

```bash
cd nova-admin
npm install
npm run dev
```

说明：

- 前端开发环境通过 Vite 代理访问本地后端
- 生产环境由容器内 Nginx 通过 `/api` 转发至 `nova-backend`

## 关键环境变量说明

| 变量 | 说明 |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | Spring 运行环境，默认 `prod` |
| `MYSQL_ROOT_PASSWORD` | MySQL root 密码 |
| `MYSQL_PASSWORD` | 业务库用户密码 |
| `SPRING_DATASOURCE_PASSWORD` | 后端连接数据库使用的密码 |
| `SPRING_AI_OPENAI_API_KEY` | AI 接口密钥，当前代码必填 |
| `SPRING_AI_OPENAI_BASE_URL` | OpenAI 兼容网关地址 |
| `LONGCAT_MODEL` | 主模型名称 |
| `LONGCAT_MODEL_FALLBACK` | 回退模型名称 |
| `NOVA_JWT_SECRET` | JWT 签名密钥 |
| `NOVA_TURNSTILE_ENABLED` | 是否启用 Turnstile |
| `NOVA_TURNSTILE_SECRET` | Turnstile 服务端密钥 |
| `NOVA_RESEND_API_KEY` | Resend API Key |
| `NOVA_RESEND_FROM` | 发信人地址 |
| `JAVA_TOOL_OPTIONS` | JVM 内存与 GC 参数 |

## API 文档

当前版本接口概览见：

- [API_SPECIFICATION.md](./API_SPECIFICATION.md)

## 上传仓库前注意

不要提交以下内容：

- `.env`
- `mysql-data/`
- `redis-data/`
- `node_modules/`
- `dist/`
- `target/`
- 本地日志与临时文件

## 当前部署特征

当前仓库已经具备以下部署条件：

- 前端与管理端都能在容器内完成打包
- 后端 Docker 镜像可直接构建
- 前端容器 Nginx 已内置 `/api` 反向代理
- 主站与管理端支持分域部署

如果你现在的目标是“整套项目直接上同一台 ECS”，当前代码结构已经满足这个方向。
