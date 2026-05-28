# AI 爆款文章创作器 (ai-passage-creator)

基于 Spring AI Alibaba + Vue 3 的多智能体协作图文创作平台。5 个 AI Agent 分工协作，从选题到图文合成全流程自动化，SSE 实时推送进度，支持人机协作介入。


## 必须遵守
1. 任何时候使用中文简体回复我

## 技术栈速览

| 层 | 技术 | 版本 |
|---|------|------|
| 后端框架 | Spring Boot | 3.5.9 |
| JDK | Java | 21 |
| ORM | MyBatis-Flex | 1.11.1 |
| Agent 框架 | Spring AI Alibaba Agent Framework | 1.1.0-RC2 |
| LLM | 通义千问 (DashScope) | - |
| 数据库 | MySQL 8.0 + Redis 7 | - |
| 前端框架 | Vue 3 + TypeScript | 3.5 / 5.8 |
| UI 库 | Ant Design Vue | 4.2 |
| 构建 | Maven + Vite 7 | - |

## 快速命令

```bash
# === 后端 ===
mvn spring-boot:run                         # 启动后端 (port 8567, context-path /api)
mvn clean package -DskipTests               # 打包

# === 前端 ===
cd frontend && npm install && npm run dev   # 启动前端 (port 5173, 代理 /api → localhost:8567)
cd frontend && npm run build                # 构建前端
cd frontend && npm run lint                 # ESLint 检查
cd frontend && npm run type-check           # TypeScript 类型检查

# === Docker 一键部署 ===
docker compose up -d --build                # 全栈启动 (前端:80, 后端:8123)

# === 接口文档 ===
# 本地: http://localhost:8567/api/doc.html
# Docker: http://localhost:8123/api/doc.html
```

## 项目结构

```
ai-passage-creator/
├── src/main/java/com/yyyouth/template/
│   ├── agent/                    # 多智能体模块 (Spring AI Alibaba StateGraph)
│   │   ├── agents/               # 5 个 Agent 实现
│   │   ├── parallel/             # 并行配图生成
│   │   ├── config/               # Agent 全局配置
│   │   ├── context/              # 流式处理器 ThreadLocal 上下文
│   │   ├── tools/                # Agent 可调用工具
│   │   └── ArticleAgentOrchestrator.java  # StateGraph 编排器(核心)
│   ├── annotation/               # @AuthCheck、@AgentExecution
│   ├── aop/                      # AuthInterceptor(权限)、AgentExecutionAspect(日志)
│   ├── config/                   # CorsConfig、AsyncConfig、JsonConfig、各服务Config
│   ├── constant/                 # ArticleConstant、PromptConstant、UserConstant
│   ├── controller/               # REST 控制器 (Article/User/Payment/Statistics/Health)
│   ├── exception/                # GlobalExceptionHandler、BusinessException、ErrorCode
│   ├── manager/                  # SseEmitterManager (SSE 连接池)
│   ├── mapper/                   # MyBatis-Flex Mapper 接口
│   ├── model/
│   │   ├── dto/article/          # ArticleState (Agent 间共享状态)、各种 Request
│   │   ├── entity/               # Article、User、AgentLog、PaymentRecord
│   │   ├── enums/                # ArticlePhaseEnum、ImageMethodEnum、SseMessageTypeEnum 等
│   │   └── vo/                   # ArticleVO、UserVO、StatisticsVO
│   ├── service/                  # 服务接口 (ArticleService extends IService)
│   │   └── impl/                 # 服务实现 (ArticleServiceImpl extends ServiceImpl<M, E>)
│   └── utils/                    # 通用工具
├── frontend/                     # 前端项目 (Vue 3 + TS + Ant Design Vue)
│   └── src/
│       ├── api/                  # 后端 API 调用 (openapi2ts 生成)
│       ├── pages/                # 页面组件 (article/admin/user/Vip)
│       ├── components/           # 公共组件 (GlobalHeader/Footer/StatusBadge)
│       ├── layouts/              # BasicLayout
│       ├── stores/               # Pinia (loginUser)
│       ├── router/               # Vue Router 配置
│       └── utils/                # markdown、sse、date、permission 工具
├── sql/                          # 数据库初始化脚本 (按顺序执行)
└── docker-compose.yml            # MySQL + Redis + Backend + Frontend
```

## 核心架构

### 三阶段创作流程 (人机协作)

```
[用户输入选题+风格] 
  → 阶段1: Agent1 生成 3-5 个标题方案 → SSE 推送 → 用户选择标题
  → 阶段2: Agent2 流式生成大纲 → SSE 推送 → 用户编辑/确认大纲
  → 阶段3: Agent3 流式生成正文 → Agent4 分析配图需求 → Agent5 并行生成配图 → 图文合成
  → 文章完成
```

每个阶段都是异步执行(`@Async("articleExecutor")`)，通过 SSE 实时推送进度。

### StateGraph 编排 (阶段3)

```
START → ContentGeneratorAgent → ImageAnalyzerAgent → ParallelImageGenerator → ContentMergerAgent → END
```

5 个 Agent 通过 `ArticleState` 共享状态，状态键使用 `ReplaceStrategy`。

### 配图策略模式 (6 种 + 自动降级)

| 方式 | 实现服务 | 权限 |
|------|---------|------|
| Pexels 图库 | PexelsService | 全部用户 |
| Mermaid 流程图 | MermaidService | 全部用户 |
| Iconify 图标 | IconifyService | 全部用户 |
| 表情包 (Bing) | EmojiPackService | 全部用户 |
| Nano Banana AI 生图 | NanoBananaService | VIP |
| SVG 示意图 | SvgDiagramService | VIP |
| Picsum 降级 | (内联) | 自动触发 |

所有配图服务实现 `ImageSearchService` 接口，通过 `ImageServiceStrategy` 策略选择器动态路由。

### SSE 实时通信

`SseEmitterManager` 使用 `ConcurrentHashMap` 管理 SSE 连接，超时 30 分钟。消息类型定义在 `SseMessageTypeEnum`。流式消息格式：`"AGENT2_STREAMING:内容片段"`。

### 权限模型

- 登录校验：`UserService.getLoginUser(request)` 从 Redis Session 获取
- 角色校验：`@AuthCheck(mustRole = "admin")` + `AuthInterceptor` AOP
- VIP 功能隔离：NanoBanana、SVG Diagram、AI 修改大纲均为 VIP 专属
- 文章权限：用户只能操作自己的文章（管理员除外）

### 配额系统

`QuotaService.checkAndConsumeQuota()` - 创建文章时消耗配额，与任务创建在同一事务中 (`@Transactional`)。VIP 用户不限配额。

## 环境变量

| 变量 | 必需 | 说明 |
|------|------|------|
| `DASHSCOPE_API_KEY` | 是 | 通义千问 API Key |
| `PEXELS_API_KEY` | 是 | Pexels 图片搜索 API Key |
| `NANO_BANANA_API_KEY` | 否 | Gemini AI 生图 |
| `STRIPE_API_KEY` | 否 | Stripe 支付 |
| `TENCENT_COS_SECRET_ID/KEY` | 否 | 腾讯云 COS 图片上传 |

## 关键约定

### 后端

- **包名**: `com.yyyouth.template`，但在 README、日志中记为 `com.yupi.template`
- **响应格式**: 统一 `BaseResponse<T>` {code, data, message}，成功 code=0
- **异常处理**: `BusinessException` + `ErrorCode` 枚举 + `GlobalExceptionHandler`
- **ORM**: MyBatis-Flex，Mapper 继承 `BaseMapper`，Service 继承 `ServiceImpl<Mapper, Entity>` + 实现自定义接口
- **实体**: 使用 Lombok `@Data @Builder @NoArgsConstructor @AllArgsConstructor`，`@Table(camelToUnderline = false)`
- **主键策略**: User 用雪花ID，Article 用自增ID
- **逻辑删除**: `isDelete` 字段 + `@Column(isLogicDelete = true)`
- **JSON 序列化**: Gson (GsonUtils) 用于内部 JSON 处理；Jackson (ObjectMapper) 用于 Web 层，Long 序列化为 String 防精度丢失
- **依赖注入**: `@Resource` (非 `@Autowired`)
- **配置**: `@Value` 或 `@ConfigurationProperties` + 独立 Config 类
- **Async**: `@Async("articleExecutor")`，线程池 core=5, max=10, queue=100

### 前端

- **目录**: `@/` 映射 `src/`
- **API 层**: `src/api/` 由 openapi2ts 自动生成，手动扩展用 `request.ts` Axios 实例
- **状态管理**: Pinia Composition API (`defineStore`)
- **路由**: 懒加载 (`() => import(...)`) + `access.ts` 前置守卫权限校验
- **组件风格**: `<script setup lang="ts">` + Composition API

## 常见问题

- **配置文件**: `application.yml`(本地) → `application-prod.yml`(Docker，通过环境变量注入)
- **Docker 端口**: MySQL/Redis 默认不暴露，安全起见仅内部网络访问
- **SSE 超时**: 30 分钟，之后自动断开
- **配图降级**: 当主方式失败时自动使用 Picsum 随机图，确保文章不中断
- **枚举校验**: `ArticleStyleEnum.isValid()`、`ArticlePhaseEnum.canTransitionTo()` 等状态转换校验
- **测试账号**: admin/12345678, user/12345678, test/12345678
