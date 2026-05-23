# 前端模块 - Vue 3 + TypeScript + Ant Design Vue 代码规范

## 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue 3 | 3.5 | Composition API + `<script setup>` |
| TypeScript | 5.8 | 类型安全 |
| Ant Design Vue | 4.2 | UI 组件库 |
| Pinia | 3.0 | 状态管理 |
| Vue Router | 4.5 | 路由 |
| Axios | 1.11 | HTTP 请求 |
| Vite | 7.0 | 构建工具 |
| marked | 17.0 | Markdown 渲染 |
| ECharts | 6.0 | 图表 (管理后台) |

## 目录结构

```
frontend/src/
├── api/                # 后端 API 调用 (openapi2ts 生成 + 手动扩展)
│   ├── index.ts        # 聚合导出
│   ├── typings.d.ts    # API 类型定义
│   ├── articleController.ts
│   ├── userController.ts
│   ├── paymentController.ts
│   └── ...
├── pages/              # 页面组件 (按业务分组)
│   ├── article/        # 文章创作/列表/详情
│   │   └── components/ # 文章页私有组件
│   ├── admin/          # 管理后台 (用户管理/数据统计)
│   └── user/           # 登录/注册
├── components/         # 全局公共组件
├── layouts/            # 布局组件 (BasicLayout)
├── stores/             # Pinia Store
├── router/             # 路由配置 + 权限守卫
├── constants/          # 常量定义
├── config/             # 配置 (环境变量映射)
└── utils/              # 工具函数
```

## 组件规范

### 必须使用 Composition API

```vue
<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'

const loading = ref(false)
const data = ref<API.ArticleVO[]>([])

const filteredData = computed(() => data.value.filter(...))

onMounted(async () => {
  await fetchData()
})
</script>

<template>
  <!-- 模板直接使用 ref, 无需 .value -->
</template>
```

### 禁止
- Options API (`data()`, `methods: {}`, `computed: {}`)
- `<script>` 不带 `setup` 和 `lang="ts"`
- 在 template 中使用 `.value`
- 复杂表达式直接写在 template 中

## 类型定义

### API 类型

API 类型由 `openapi2ts` 从后端 Knife4j OpenAPI 文档自动生成到 `src/api/typings.d.ts`，命名空间 `API.`：

```typescript
// 使用自动生成的类型
const loginUser = ref<API.LoginUserVO>({})
const articleList = ref<API.ArticleVO[]>([])
```

### 类型生成命令

```bash
cd frontend && npm run openapi2ts
```

该命令读取后端 `/api/v3/api-docs/default` 生成完整的 TypeScript 类型。

## API 调用规范

### 请求实例 (`src/request.ts`)

```typescript
import myAxios from '@/request'

// 响应拦截器：自动处理 40100 未登录 → 跳转登录页
// 请求拦截器：自动携带 Cookie (withCredentials: true)
// 超时时间：由 constants 中 REQUEST_TIMEOUT 控制
// baseURL：由 config/env.ts 中 API_BASE_URL 控制
```

### Controller API 文件示例

```typescript
// src/api/articleController.ts
import myAxios from '@/request'

export const createArticle = (data: API.ArticleCreateRequest) =>
  myAxios.post<API.BaseResponseString>('/article/create', data)

export const getArticleDetail = (taskId: string) =>
  myAxios.get<API.BaseResponseArticleVO>(`/article/${taskId}`)
```

### 调用约定

- 所有 API 调用通过 Controller 文件导出函数，**不在组件中直接调 axios**
- 响应类型统一 `BaseResponse<T>`：`{ code: number, data: T, message: string }`
- `code === 0` 表示成功
- `code === 40100` 表示未登录（由响应拦截器统一处理跳转）

## 路由规范

```typescript
// 路由配置: src/router/index.ts
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/create',
      name: '创作文章',
      component: () => import('@/pages/article/ArticleCreatePage.vue'),  // 懒加载
    },
    {
      path: '/user/login',
      name: '用户登录',
      component: UserLoginPage,  // 首屏直接导入
    },
  ],
})
```

### 权限守卫 (`src/access.ts`)

```typescript
router.beforeEach(async (to, from, next) => {
  // 首次加载时等待获取登录用户信息
  if (firstFetchLoginUser) {
    await loginUserStore.fetchLoginUser()
    firstFetchLoginUser = false
  }
  // 管理页面需要 admin 角色
  if (toUrl.startsWith('/admin')) {
    if (!loginUser || loginUser.userRole !== USER_ROLE_ADMIN) {
      next(`/user/login?redirect=${to.fullPath}`)
      return
    }
  }
  next()
})
```

### 规则
- 除首页/登录/注册外，其他页面组件使用懒加载
- 管理路由以 `/admin` 开头，由 `access.ts` 统一鉴权
- 页面切换后状态不保留（除非存入 Pinia）

## 状态管理 (Pinia)

```typescript
// src/stores/loginUser.ts
export const useLoginUserStore = defineStore('loginUser', () => {
  const loginUser = ref<API.LoginUserVO>({ userName: '未登录' })

  async function fetchLoginUser() {
    const res = await getLoginUser()
    if (res.data.code === 0 && res.data.data) {
      loginUser.value = res.data.data
    }
  }

  return { loginUser, fetchLoginUser, setLoginUser }
})
```

- 必须用 Composition API 风格 (`defineStore` 第二参数为函数)
- Store 文件以用途命名（如 `loginUser`），不用 `use` 前缀（使用处加 `use`）

## SSE 实时通信

```typescript
// src/utils/sse.ts — 封装 EventSource 连接
// 用于文章创作进度的实时推送

// 组件中使用：
const eventSource = connectSSE(taskId, {
  onMessage: (data) => {
    // 根据 data.type 处理不同的 SSE 消息类型
    // TITLES_GENERATED / OUTLINE_GENERATED / AGENT2_STREAMING
    // / AGENT3_STREAMING / IMAGE_COMPLETE / ALL_COMPLETE / ERROR
  },
  onError: (error) => { ... }
})

// 组件卸载时必须关闭连接
onUnmounted(() => {
  eventSource?.close()
})
```

### SSE 消息类型 (对应后端 `SseMessageTypeEnum`)

| type | 说明 | data 字段 |
|------|------|----------|
| `TITLES_GENERATED` | 标题方案已生成 | `titleOptions` |
| `AGENT2_STREAMING` | 大纲流式输出 | `content` |
| `OUTLINE_GENERATED` | 大纲生成完成 | `outline` |
| `AGENT3_STREAMING` | 正文流式输出 | `content` |
| `AGENT4_COMPLETE` | 配图需求分析完成 | `imageRequirements` |
| `IMAGE_COMPLETE` | 单张配图完成 | `image` |
| `AGENT5_COMPLETE` | 所有配图完成 | `images` |
| `MERGE_COMPLETE` | 图文合成完成 | `fullContent` |
| `ALL_COMPLETE` | 全部完成 | `taskId` |
| `ERROR` | 错误 | `message` |

## 页面组件的流程模型

文章创作页面 (`ArticleCreatePage.vue`) 采用状态机驱动的阶段式 UI：

```
InputState (输入选题) 
  → CreatingState (创作中: SSE 进度展示)
  → TitleSelectingStage (选择标题)
  → OutlineEditingStage (编辑大纲)
  → CreatingState (生成正文+配图)
  → CompletedState (展示完成文章)
```

子组件位于 `pages/article/components/`，通过 props/emits 与父组件通信。

## 主要约定

### 路径别名
- `@/` → `src/`
- 在 `vite.config.ts` 和 `tsconfig` 中统一配置

### 开发代理
- `/api` → `http://localhost:8567` (开发时通过 Vite proxy，生产时 Nginx 反代)

### 样式
- 使用 Ant Design Vue 组件内置样式，辅以 scoped SCSS
- 全局样式变量在 `App.vue` 或统一入口中定义

### 常量
- 用户相关：`@/constants/user` (USER_ROLE_ADMIN, DEFAULT_USERNAME 等)
- 文章相关：`@/constants/article` (文章状态/阶段映射等)
- 通用：`@/constants/index` (REQUEST_TIMEOUT, UNAUTHORIZED_CODE 等)

### 工具函数
- `utils/markdown.ts` — Markdown 渲染 (基于 marked)
- `utils/date.ts` — 日期格式化 (基于 dayjs)
- `utils/sse.ts` — SSE EventSource 封装
- `utils/permission.ts` — 前端权限判断
- `utils/article.ts` — 文章相关工具

### 禁止
- 直接使用 `axios` 而非 `@/request` 封装的 `myAxios`
- 在组件中硬编码 API URL
- SSE 连接未在 `onUnmounted` 中关闭
- 使用 `any` 类型（除非确实无法确定）
- 使用 Options API (`this.xxx` 模式)
