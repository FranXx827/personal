# AI 对话系统改造 — 最终架构

## 架构变更：存库逻辑移至 Java Backend

```
前端 (Vue 3)  ──SSE──▶  ai-assistant (Python FastAPI)
                            │
                            │ HTTP (X-Service-Token)
                            ▼
                     Java Backend (Spring Boot)
                            │
                            ▼
                      MySQL (你的外接数据库)
```

**关键变化**：
- ai-assistant **不再直连 MySQL**，改为通过 HTTP 调用 Java Backend
- ai-assistant 依赖从 28 个精简到 23 个（移除 sqlalchemy/aiomysql/alembic）
- Java Backend 新增 `chat` 模块，MyBatis-Plus 管理 `chat_session` / `chat_message` 表

## 新增 Java 文件

```
backend/src/main/java/com/ecommerce/modules/chat/
├── controller/ChatController.java        # REST 接口
├── dto/
│   ├── ChatSessionVO.java                # 会话 VO
│   ├── ChatMessageVO.java                # 消息 VO
│   ├── ChatSessionDetailVO.java          # 详情 VO
│   ├── SessionCreateRequest.java         # 创建会话 DTO
│   ├── MessageCreateRequest.java         # 添加消息 DTO
│   └── TitleUpdateRequest.java           # 更新标题 DTO
├── entity/
│   ├── ChatSession.java                  # 会话实体
│   └── ChatMessage.java                  # 消息实体
├── mapper/
│   ├── ChatSessionMapper.java            # 会话 Mapper
│   └── ChatMessageMapper.java            # 消息 Mapper
└── service/
    ├── ChatService.java                  # 服务接口
    └── impl/ChatServiceImpl.java         # 服务实现
```

## 新增 SQL

`backend/sql/init_chat.sql` — 在你外接数据库中执行

## 修改的 Java 文件

`config/SecurityConfig.java` — PUBLIC_URLS 新增 `/api/chat/**`

## 修改的 Python 文件

- `app/api/v1/chat.py` — 用 `backend_client` 替换直接 DB 操作
- `app/services/backend_client.py` — **新建**，HTTP 客户端封装
- `app/main.py` — 移除 DB 初始化，精简 lifespan
- `requirements.txt` / `pyproject.toml` — 移除 sqlalchemy / aiomysql / alembic

## 启动步骤

```bash
# 1. 在你的外接数据库执行建表
mysql -u your_user -p < backend/sql/init_chat.sql

# 2. 启动 Java Backend
cd backend && mvn spring-boot:run

# 3. 启动 AI 服务
cd ai-assistant && uvicorn app.main:app --reload --port 8000

# 4. 启动前端
cd frontend && npm run dev
```
