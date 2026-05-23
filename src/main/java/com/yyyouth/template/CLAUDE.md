# 后端模块 - Spring Boot 3.5 代码规范与约束

## 分层架构

```
controller/     → 接收请求、参数校验、调用 Service、返回 BaseResponse
service/        → 业务接口 (extends IService<Entity>)
service/impl/   → 业务实现 (extends ServiceImpl<Mapper, Entity>)
mapper/         → 数据访问 (MyBatis-Flex BaseMapper)
model/entity/   → 数据库实体 (@Table)
model/dto/      → 请求 DTO (XxxRequest)
model/vo/       → 响应 VO (XxxVO)
model/enums/    → 枚举 (状态、类型、角色等)
agent/          → 智能体模块 (Spring AI Alibaba StateGraph)
config/         → @Configuration 配置类
aop/            → 切面 (权限、日志)
exception/      → 异常定义 + 全局处理器
constant/       → 常量接口
manager/        → 通用管理器组件
utils/          → 工具类
```

## Controller 层规范

### 必须遵守
- 类注解：`@RestController` + `@RequestMapping("/xxx")` + `@Slf4j`
- 注入：`@Resource`（不用 `@Autowired`）
- 返回类型：统一 `BaseResponse<T>`，通过 `ResultUtils.success(data)` 构造
- 参数校验：使用 `ThrowUtils.throwIf()` 抛 `BusinessException`，**不在 Controller 写业务逻辑**
- 获取登录用户：`userService.getLoginUser(request)`（从 Redis Session 读取）
- 权限校验：方法上加 `@AuthCheck(mustRole = "admin")`
- 接口文档：方法上加 `@Operation(summary = "xxx")`

```java
@RestController
@RequestMapping("/article")
@Slf4j
public class ArticleController {

    @Resource
    private ArticleService articleService;

    @PostMapping("/create")
    @Operation(summary = "创建文章任务")
    public BaseResponse<String> createArticle(
            @RequestBody ArticleCreateRequest request,
            HttpServletRequest httpServletRequest) {

        ThrowUtils.throwIf(request.getTopic() == null || request.getTopic().trim().isEmpty(),
                ErrorCode.PARAMS_ERROR, "选题不能为空");

        User loginUser = userService.getLoginUser(httpServletRequest);
        String taskId = articleService.createArticleTaskWithQuotaCheck(
                request.getTopic(), request.getStyle(),
                request.getEnabledImageMethods(), loginUser);

        // 异步执行阶段1
        articleAsyncService.executePhase1(taskId, request.getTopic(), request.getStyle());

        return ResultUtils.success(taskId);
    }
}
```

### 禁止
- Controller 中写业务逻辑
- 直接操作 Mapper
- 返回 `Map` 或裸对象
- 吞异常不 log

## Service 层规范

### 接口-实现分离

```java
// 接口：继承 MyBatis-Flex IService
public interface ArticleService extends IService<Article> {
    String createArticleTask(String topic, String style,
            List<String> enabledImageMethods, User loginUser);
}

// 实现：继承 ServiceImpl + 实现自定义接口
@Service
@Slf4j
public class ArticleServiceImpl
        extends ServiceImpl<ArticleMapper, Article>
        implements ArticleService {
    // 通过 this.save/this.updateById/this.page 等继承方法操作数据库
}
```

### 事务控制

```java
@Override
@Transactional(rollbackFor = Exception.class)
public String createArticleTaskWithQuotaCheck(...) {
    // 配额扣减和任务创建在同一事务中
    quotaService.checkAndConsumeQuota(loginUser);
    return createArticleTask(topic, style, enabledImageMethods, loginUser);
}
```

- 只在 Service 层开启事务
- `@Transactional(rollbackFor = Exception.class)` — 所有异常都回滚
- 事务内不要做网络 IO 或耗时操作

### 查询构建 (MyBatis-Flex QueryWrapper)

```java
QueryWrapper queryWrapper = QueryWrapper.create()
    .eq("isDelete", 0)
    .eq("userId", userId)           // 等于
    .orderBy("createTime", false);  // 降序
Page<Article> page = this.page(new Page<>(current, size), queryWrapper);
```

## Entity 规范

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(value = "article", camelToUnderline = false)
public class Article implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)       // 自增主键（Article）
    // @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)  雪花ID（User）
    private Long id;

    @Column(isLogicDelete = true)      // 逻辑删除
    private Integer isDelete;

    private LocalDateTime createTime;  // 使用 LocalDateTime，不用 Date
}
```

### 规则
- 必须使用 Lombok：`@Data @Builder @NoArgsConstructor @AllArgsConstructor`
- `@Table(camelToUnderline = false)` — 字段名与数据库列名一致（不转下划线）
- 日期时间统一 `LocalDateTime`
- JSON 字段（如 titleOptions、outline、images）用 `String` 存储，读写时通过 `GsonUtils` 转换
- 逻辑删除字段 `isDelete` + `@Column(isLogicDelete = true)`
- 枚举字段存字符串值（如 status/phase），不存序数

## DTO / VO 规范

### Request DTO
```java
@Data
public class ArticleCreateRequest implements Serializable {
    private String topic;
    private String style;
    private List<String> enabledImageMethods;
}
```

### Response VO
```java
@Data
public class ArticleVO implements Serializable {
    // 提供 objToVo(Entity) 静态转换方法
    public static ArticleVO objToVo(Article article) { ... }
}
```

### ArticleState — Agent 间共享状态
- 位于 `model/dto/article/ArticleState.java`
- 包含所有阶段涉及的字段（taskId, topic, style, title, outline, content, images 等）
- 内部嵌套类：`TitleOption`, `TitleResult`, `OutlineResult`, `OutlineSection`, `ImageRequirement`, `ImageResult`, `Agent4Result`
- 在 StateGraph 中作为 OverAllState 的 value 传递

## 枚举规范

```java
@Getter
public enum ArticlePhaseEnum {
    PENDING("PENDING", "等待处理"),
    TITLE_GENERATING("TITLE_GENERATING", "生成标题中");
    // ...
    private final String value;
    private final String description;

    public static ArticlePhaseEnum getByValue(String value) { ... }
    public boolean canTransitionTo(ArticlePhaseEnum targetPhase) { ... }
}
```

- 统一存储/传输用 `value` 字段（字符串），不用 `ordinal()`
- 提供 `getByValue(String)` 静态查找方法
- 状态机转换逻辑放在枚举内（如 `canTransitionTo()`）

## 异常处理规范

### 错误码枚举
```java
@Getter
public enum ErrorCode {
    SUCCESS(0, "ok"),
    PARAMS_ERROR(40000, "请求参数错误"),
    NOT_LOGIN_ERROR(40100, "未登录"),
    NO_AUTH_ERROR(40101, "无权限"),
    NOT_FOUND_ERROR(40400, "请求数据不存在"),
    SYSTEM_ERROR(50000, "系统内部异常"),
    OPERATION_ERROR(50001, "操作失败");
}
```

### 抛异常
```java
// 使用 ThrowUtils 快速断言
ThrowUtils.throwIf(condition, ErrorCode.PARAMS_ERROR);
ThrowUtils.throwIf(condition, ErrorCode.PARAMS_ERROR, "自定义消息");

// 直接抛业务异常
throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "更详细的消息");
```

### 全局异常拦截
`GlobalExceptionHandler` (`@RestControllerAdvice`) 统一处理：
- `BusinessException` → 返回对应 code/message
- `RuntimeException` → 返回 `SYSTEM_ERROR`，隐藏内部细节

## AOP 规范

### @AuthCheck — 权限校验
```java
@AuthCheck(mustRole = "admin")  // 仅管理员可访问
@AuthCheck                       // 必须登录（不限制角色）
```

### @AgentExecution — 智能体执行日志
```java
@AgentExecution("TitleGenerator")
```
自动记录：taskId、输入/输出数据、耗时、成功/失败状态

## Agent 模块规范

### 编排器 (ArticleAgentOrchestrator)
- 通过 Spring AI Alibaba StateGraph 编排 Agent 执行顺序
- 状态键常量用 `private static final String KEY_XXX = "xxx"`
- `KeyStrategyFactory` 使用 `ReplaceStrategy` — 同名 key 后值覆盖前值
- `StreamHandlerContext` (ThreadLocal) 传递流式回调，**必须 finally 清理**

### Agent 实现
- 实现 `AsyncNodeAction` (函数式接口)
- 从 `OverAllState` 读取输入，写入输出
- 流式输出通过 `StreamHandlerContext.get()` 获取回调
- 通过 `@AgentExecution("AgentName")` 自动记录日志

### 并行配图 (ParallelImageGenerator)
- 内部使用线程池并行调用各配图服务
- 每张配图完成后通过 `IMAGE_COMPLETE` SSE 消息实时通知前端

## 配置类规范

- 每个外部服务一个独立 Config 类（如 `PexelsConfig`、`StripeConfig`、`CosConfig`）
- 使用 `@ConfigurationProperties` 或 `@Value` 读取配置
- API Key 等敏感配置优先从环境变量读取：`${PEXELS_API_KEY:}`
- 在 `application.yml` 中设置默认值，`application-prod.yml` 中通过 `${ENV_VAR:}` 覆盖

## 依赖注入

- 统一使用 `@Resource` (JSR-250)，**不用 `@Autowired`**
- 不用构造器注入 (Lombok `@RequiredArgsConstructor` 不适用)

## 工具库使用

| 用途 | 工具 |
|------|------|
| JSON 内部处理 | `GsonUtils` (基于 Gson) |
| JSON Web 序列化 | Jackson ObjectMapper |
| UUID 生成 | `cn.hutool.core.util.IdUtil.simpleUUID()` |
| 通用工具 | Hutool (按需使用，不滥用) |

## 关键约束速查

1. **状态键名必须与 StateGraph 中一致**——修改 `ArticleState` 字段时同步更新 `KEY_XXX` 常量
2. **异步方法必须加 `@Async("articleExecutor")`** ——否则阻塞请求线程
3. **StreamHandlerContext 必须 finally 清理** ——防止内存泄漏
4. **JSON 字段存储使用 GsonUtils**——不用 Jackson，保持统一的序列化策略
5. **数据库字段名与 Java 字段名一致** ——`camelToUnderline = false`
6. **Long 型 ID 序列化为 String** —— `JsonConfig` 全局配置，防止前端精度丢失
7. **Docker 环境用 `application-prod.yml`** ——通过 `SPRING_PROFILES_ACTIVE: prod` 激活
