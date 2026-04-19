# NovaLeap API Specification

本文档基于当前仓库中的后端控制器与前端调用方式整理，描述的是当前版本可见接口分布，而不是旧版本草稿。

## 1. 基本约定

### 基础路径

- 用户端与管理端都通过同一后端服务提供 API
- 默认基础前缀：`/api`

### 返回格式

除 SSE 流式接口外，普通接口统一返回如下结构：

```json
{
  "code": 200,
  "msg": "成功",
  "data": {}
}
```

说明：

- `code = 200` 表示业务成功
- 非 `200` 时通常表示业务失败或鉴权失败
- `msg` 为错误提示或成功消息

### 鉴权方式

- 用户端登录成功后，通过 `Authorization: Bearer <token>` 访问受保护接口
- 管理端登录成功后，也通过 `Authorization: Bearer <token>` 访问后台接口
- 当前后端采用 JWT 无状态鉴权

### 权限等级

- `Public`：匿名可访问
- `User`：普通登录用户可访问
- `Admin`：后台管理员权限

## 2. SSE 流式接口

以下接口返回 `text/event-stream`：

| Method | Path | Auth | 说明 |
| --- | --- | --- | --- |
| `GET` | `/api/ai/question/{id}/explain` | Public | AI 讲解题目，流式返回 |
| `POST` | `/api/ai/coach/chat` | Public / User | AI 陪练聊天，流式返回 |
| `POST` | `/api/ai/resume/analyze` | Public / User | AI 简历分析，流式返回 |

## 3. 用户认证接口

基础前缀：`/api/auth`

| Method | Path | Auth | 说明 |
| --- | --- | --- | --- |
| `POST` | `/api/auth/login` | Public | 用户登录 |
| `POST` | `/api/auth/guest` | Public | 游客登录 |
| `POST` | `/api/auth/register` | Public | 用户注册 |
| `POST` | `/api/auth/logout` | User | 退出登录 |
| `POST` | `/api/auth/password/reset` | Public | 重置密码 |
| `POST` | `/api/auth/email/send-code` | Public | 发送邮箱验证码 |
| `GET` | `/api/auth/profile` | User | 获取个人资料 |
| `PUT` | `/api/auth/profile` | User | 更新个人资料 |
| `GET` | `/api/auth/streak` | User | 获取签到 / 连续活跃信息 |

### 典型请求体

登录：

```json
{
  "username": "demo",
  "password": "demo-password"
}
```

注册：

```json
{
  "username": "demo",
  "password": "demo-password",
  "email": "demo@example.com",
  "code": "123456"
}
```

说明：

- 实际字段以 DTO 校验规则为准
- 登录与注册成功后，返回的数据中通常包含 token 与用户信息

## 4. 题库接口

基础前缀：`/api/questions`

| Method | Path | Auth | 说明 |
| --- | --- | --- | --- |
| `GET` | `/api/questions` | Public | 分页查询题目列表 |
| `GET` | `/api/questions/categories` | Public | 查询题目分类选项 |
| `GET` | `/api/questions/{id}` | Public | 查询题目详情 |
| `POST` | `/api/questions/{id}/view` | Public | 记录 / 增加题目浏览量 |
| `GET` | `/api/questions/random` | Public | 随机抽题 |
| `GET` | `/api/questions/{id}/answer` | Public | 查看题目答案 |

### 常见查询参数

- `page`
- `size`
- `category`
- `difficulty`
- `keyword`
- `bankId`

## 5. 用户题库导入接口

基础前缀：`/api/question-banks`

| Method | Path | Auth | 说明 |
| --- | --- | --- | --- |
| `GET` | `/api/question-banks/mine` | User | 查看我的题库 |
| `POST` | `/api/question-banks/import` | User | 上传文件导入题库 |
| `PUT` | `/api/question-banks/{id}` | User | 编辑我的题库 |

说明：

- 导入接口使用 `multipart/form-data`

## 6. 笔记接口

基础前缀：`/api/notes`

| Method | Path | Auth | 说明 |
| --- | --- | --- | --- |
| `GET` | `/api/notes` | User | 分页查询公开 / 可见笔记 |
| `GET` | `/api/notes/mine` | User | 查询我的笔记 |
| `GET` | `/api/notes/{id}` | User | 查询笔记详情 |
| `POST` | `/api/notes/{id}/view` | User | 增加浏览量 |
| `POST` | `/api/notes` | User | 创建笔记 |
| `PUT` | `/api/notes/{id}` | User | 编辑笔记 |
| `POST` | `/api/notes/{id}/like` | User | 点赞 / 取消点赞 |
| `GET` | `/api/notes/{id}/comments` | User | 查询评论 |
| `POST` | `/api/notes/{id}/comments` | User | 新增评论 |
| `DELETE` | `/api/notes/{id}` | User | 删除笔记 |

### 常见查询参数

- `page`
- `size`
- `keyword`
- `category`
- `status`

## 7. 愿望墙接口

基础前缀：`/api/wishes`

| Method | Path | Auth | 说明 |
| --- | --- | --- | --- |
| `GET` | `/api/wishes` | Public | 查询已审核通过的愿望列表 |
| `POST` | `/api/wishes` | User | 提交愿望 |
| `POST` | `/api/wishes/{wishId}/like` | Public / User | 点赞或取消点赞 |
| `GET` | `/api/wishes/{wishId}/comments` | Public / User | 查询评论 |
| `POST` | `/api/wishes/{wishId}/comments` | User | 新增评论 |

说明：

- 游客账号不能发布愿望
- 游客账号不能评论
- 未登录访客的点赞通常依赖 `visitorId`

## 8. 排行榜接口

基础前缀：`/api/leaderboard`

| Method | Path | Auth | 说明 |
| --- | --- | --- | --- |
| `GET` | `/api/leaderboard` | Public | 查询综合排行榜 |
| `GET` | `/api/leaderboard/question-done` | Public | 查询答题完成榜 |
| `POST` | `/api/leaderboard/question-done` | User | 上报答题完成记录 |
| `POST` | `/api/leaderboard/game-score` | User | 上报小游戏积分 |

## 9. AI 接口

基础前缀：`/api/ai`

| Method | Path | Auth | 说明 |
| --- | --- | --- | --- |
| `GET` | `/api/ai/question/{id}/explain` | Public | 流式讲解题目 |
| `POST` | `/api/ai/coach/chat` | Public / User | 流式 AI 陪练 |
| `GET` | `/api/ai/coach/history` | User | 查询陪练历史 |
| `GET` | `/api/ai/quote/daily` | Public / User | 获取每日一句 |
| `POST` | `/api/ai/coach/session/new` | User | 新建陪练会话 |
| `DELETE` | `/api/ai/coach/history` | User | 清空陪练历史 |
| `POST` | `/api/ai/resume/analyze` | Public / User | 流式简历分析 |
| `POST` | `/api/ai/notes/summarize` | Public / User | 生成笔记摘要 |

### 说明

- 陪练与简历分析支持 SSE
- `notes/summarize` 返回普通 JSON，不是流式
- 匿名访问时，后端会回退到用户名之外的标识方式，例如客户端 IP

## 10. 访客统计接口

基础前缀：`/api/analytics`

| Method | Path | Auth | 说明 |
| --- | --- | --- | --- |
| `POST` | `/api/analytics/visit` | Public | 上报页面访问行为 |

## 11. 管理端认证接口

基础前缀：`/api/admin/auth`

| Method | Path | Auth | 说明 |
| --- | --- | --- | --- |
| `POST` | `/api/admin/auth/login` | Public | 管理员登录 |

## 12. 管理端总览与监控接口

基础前缀：`/api/admin`

| Method | Path | Auth | 说明 |
| --- | --- | --- | --- |
| `GET` | `/api/admin/dashboard` | Admin | 获取后台总览数据 |
| `GET` | `/api/admin/system-monitor` | Admin | 获取系统监控信息 |
| `GET` | `/api/admin/visitor-records` | Admin | 获取访客记录列表 |

## 13. 管理端用户接口

基础前缀：`/api/admin/users`

| Method | Path | Auth | 说明 |
| --- | --- | --- | --- |
| `GET` | `/api/admin/users` | Admin | 分页查询用户 |
| `GET` | `/api/admin/users/{id}` | Admin | 查询用户详情 |
| `POST` | `/api/admin/users` | Admin | 创建用户 |
| `PUT` | `/api/admin/users/{id}` | Admin | 更新用户 |
| `DELETE` | `/api/admin/users/{id}` | Admin | 删除用户 |

## 14. 管理端题目接口

基础前缀：`/api/admin/questions`

| Method | Path | Auth | 说明 |
| --- | --- | --- | --- |
| `GET` | `/api/admin/questions` | Admin | 分页查询题目 |
| `GET` | `/api/admin/questions/{id}` | Admin | 查询题目详情 |
| `POST` | `/api/admin/questions` | Admin | 创建题目 |
| `PUT` | `/api/admin/questions/{id}` | Admin | 编辑题目 |
| `DELETE` | `/api/admin/questions/{id}` | Admin | 删除题目 |

## 15. 管理端题目分类接口

基础前缀：`/api/admin/question-categories`

| Method | Path | Auth | 说明 |
| --- | --- | --- | --- |
| `GET` | `/api/admin/question-categories` | Admin | 查询题目分类 |
| `POST` | `/api/admin/question-categories` | Admin | 创建题目分类 |
| `DELETE` | `/api/admin/question-categories/{code}` | Admin | 删除题目分类 |

## 16. 管理端题库接口

基础前缀：`/api/admin/question-banks`

| Method | Path | Auth | 说明 |
| --- | --- | --- | --- |
| `GET` | `/api/admin/question-banks` | Admin | 查询用户上传题库 |
| `POST` | `/api/admin/question-banks/official/import` | Admin | 导入官方题库 |
| `PUT` | `/api/admin/question-banks/{id}/audit` | Admin | 审核题库 |

说明：

- 官方题库导入接口使用 `multipart/form-data`

## 17. 管理端笔记接口

基础前缀：`/api/admin/notes`

| Method | Path | Auth | 说明 |
| --- | --- | --- | --- |
| `GET` | `/api/admin/notes` | Admin | 分页查询笔记 |
| `GET` | `/api/admin/notes/{id}` | Admin | 查询笔记详情 |
| `POST` | `/api/admin/notes` | Admin | 创建笔记 |
| `PUT` | `/api/admin/notes/{id}` | Admin | 编辑笔记 |
| `PUT` | `/api/admin/notes/{id}/status` | Admin | 更新审核状态 |
| `DELETE` | `/api/admin/notes/{id}` | Admin | 删除笔记 |

## 18. 管理端愿望墙接口

基础前缀：`/api/admin/wishes`

| Method | Path | Auth | 说明 |
| --- | --- | --- | --- |
| `GET` | `/api/admin/wishes` | Admin | 分页查询愿望 |
| `GET` | `/api/admin/wishes/{id}` | Admin | 查询愿望详情 |
| `GET` | `/api/admin/wishes/queue/stats` | Admin | 查看审核队列统计 |
| `GET` | `/api/admin/wishes/dead-letter` | Admin | 查看死信队列 |
| `POST` | `/api/admin/wishes` | Admin | 创建愿望 |
| `PUT` | `/api/admin/wishes/{id}` | Admin | 编辑愿望 |
| `DELETE` | `/api/admin/wishes/{id}` | Admin | 删除愿望 |
| `PUT` | `/api/admin/wishes/{id}/status` | Admin | 更新愿望状态 |
| `POST` | `/api/admin/wishes/{id}/retry` | Admin | 重试审核任务 |

## 19. 常见请求头

普通 JSON 请求：

```http
Content-Type: application/json
Authorization: Bearer <token>
```

SSE 请求：

```http
Accept: text/event-stream
Authorization: Bearer <token>
```

文件上传：

```http
Content-Type: multipart/form-data
Authorization: Bearer <token>
```

## 20. 前端路由对照

### 用户端路由

- `/login`
- `/register`
- `/forgot-password`
- `/terms`
- `/privacy`
- `/`
- `/questions`
- `/notes`
- `/resume`
- `/coach`
- `/wishes`
- `/game`
- `/me`
- `/leaderboard`
- `/profile`

### 管理端路由

- `/login`
- `/dashboard`
- `/users`
- `/questions`
- `/notes`
- `/wishes`
- `/visitor-records`
- `/monitor`

## 21. 部署说明

当前版本更适合同机部署：

- `nova-frontend` 与 `nova-admin` 容器内部 Nginx 已内置 `/api` 代理
- 默认会将 `/api` 转发到 `nova-backend:8080`
- 因此前后端同机部署时，不必额外让浏览器直连后端公网端口

推荐生产接入方式：

- `novaleap.xyz` -> 用户端容器
- `admin.novaleap.xyz` -> 管理端容器
- 主机 Nginx 统一处理 `80/443`

## 22. 文档边界

本文档描述的是当前控制器可见接口与当前前端实际使用到的页面路径。

不包含以下内容：

- 具体 DTO 字段的完整校验规则
- 每个返回对象的全部嵌套字段
- 数据库表结构细节

如果后续你准备把这个项目对外开放成正式协作文档，下一步建议继续补：

- DTO 字段表
- VO 返回字段表
- 错误码清单
- Postman / Apifox 导出文件
