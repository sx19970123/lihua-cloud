# 后端开发参考

## 模块

- Maven 父工程：`lihua-cloud/pom.xml`。
- 微服务公共模块：`lihua-base/lihua-base-*`，包括 attachment、cache、captcha、client、common、dict、doc、excel、ip、job、log、mybatis、security、sensitive、web、websocket 等能力。
- 远程调用契约模块：`lihua-api/lihua-api-system`，包含 `client`、`facade`、`model`。
- 可运行服务：
  - `lihua-gateway`：Spring Cloud Gateway 网关。
  - `lihua-auth`：认证服务。
  - `lihua-biz/lihua-system`：系统/RBAC/字典/配置/通知等核心业务。
  - `lihua-biz/lihua-file`：附件与文件服务。
  - `lihua-biz/lihua-monitor`：监控服务。
- 启动类使用 `@ComponentScan({"com.lihua.**"})`；含持久化的服务使用 `@MapperScan({"com.lihua.**.mapper"})`。新增包应保持在 `com.lihua` 下。

## 运行与配置事实

- Java/Maven 编译版本以 `lihua-cloud/pom.xml` 为准；当前 `maven.compiler.source` 和 `maven.compiler.target` 为 `25`。README 中的 Java 版本描述只能作为最低环境参考。
- 各服务环境配置位于对应服务的 `src/main/resources/application.yml`、`application-dev.yml`、`application-prod.yml`。
- 服务发现和配置中心使用 Nacos；各服务 `application-*.yml` 中通过 `spring.config.import` 引入 Nacos 配置。
- 网关服务是外部入口；前端和 App 不应绕过网关直接调用内部服务。
- `application.yml` 配置了虚拟线程、Jackson 忽略 null；含数据库服务还配置 MyBatis-Plus 逻辑删除字段 `delFlag` 和 mapper XML 扫描路径 `classpath*:com/lihua/**/mapper/**/*.xml`。
- 数据源使用 dynamic-datasource；Redis 客户端使用 Redisson，并配置项目自定义 codec。
- 附件上传模式由 `attachment.uploadFileModel` 控制。代码中可见的存储策略组件是 `LOCAL` 和 `ALIYUN-OSS`；不要把配置注释中的其他存储当作已实现能力，除非先确认代码已有对应策略。

## 新增模块与服务归属

- 顶层 `<modules>` 目前包含 `lihua-base`、`lihua-biz`、`lihua-api`、`lihua-auth`、`lihua-gateway`。
- 业务服务优先放在 `lihua-biz` 下，并在 `lihua-biz/pom.xml` 中声明模块。
- 系统管理/RBAC/字典/配置优先进入 `lihua-biz/lihua-system`。
- 文件、附件、上传下载能力优先进入 `lihua-biz/lihua-file`。
- 认证登录、注册、刷新登录态优先进入 `lihua-auth`；如需读取用户资料，通过 `lihua-api` 调用系统服务。
- Gateway 只处理路由、鉴权前置、IP/Token 过滤、fallback 和网关异常，不承载普通业务逻辑。
- 新增可复用 base 模块时，需要在父工程 dependencyManagement 和相关服务依赖中保持版本与 artifactId 一致。

## 远程调用契约

- 跨服务调用优先在 `lihua-api/<api-module>` 中定义 `@RemoteClient(serverName = "...")` 接口，并使用 Spring `@HttpExchange`、`@GetExchange`、`@PostExchange` 等声明路径。
- `serverName` 必须与目标服务的 `spring.application.name` 一致，例如 `lihua-system`。
- `@HttpExchange`、`@GetExchange`、`@PostExchange` 的路径必须与目标 Controller 精确一致；改 Controller 路径时同步改 API client。
- 同步调用默认使用 `RestClientFactoryBean`；需要异步时在 `@RemoteClient` 设置 `executionMode = ASYNC`，由 `WebClientFactoryBean` 创建代理。
- `lihua-base-client` 已统一处理服务发现负载均衡、token 透传、签名、连接超时和响应超时；不要手写固定 URL、RestTemplate 或 WebClient 绕过它。
- API 模块中的 facade 负责包装 client 调用、统一异常/响应处理或响应拆箱；业务服务优先依赖 facade，不要在各处重复写远程调用细节。
- 远程调用的 DTO/model 放在 API 模块，避免调用方依赖目标服务内部 entity。
- 同步/异步调用模式遵循 `@RemoteClient` 的 `executionMode` 和现有 facade 写法；不要临时手写 RestTemplate/WebClient 绕过项目封装。

## 熔断降级

- 跨服务 facade 方法优先使用 Resilience4j `@CircuitBreaker`，不要把熔断逻辑直接堆在 Controller 中。
- 熔断名称按业务或目标资源稳定命名，例如 `sysUser`、`sysSetting`、`sysLog`，并与 Nacos resilience 配置保持可追踪。
- 有明确可降级结果的方法提供 `fallbackMethod`；fallback 方法参数必须与原方法参数一致，并在末尾追加 `Throwable`。
- fallback 中记录异常和关键请求参数。同步接口返回统一 `ApiResponseModel` 错误；异步接口按现有模式返回 `Mono.error(...)` 或与原返回类型一致的降级结果。
- 如果只是允许默认异常传播，也要确认调用方能正确处理失败响应，不要吞掉远程调用错误。

## Nacos Resilience 配置

- 各服务的 `application-dev.yml`、`application-prod.yml` 通过 `nacos:lihua-resilience.yaml?group=lihua-resilience` 引入统一 resilience 配置。
- 修改熔断窗口、失败率阈值、OPEN 等待时间、超时时间等策略时，优先维护 Nacos 配置种子 `res/nacos/nacos_config.zip` 中的 `lihua-resilience/lihua-resilience.yaml` 或目标部署环境配置。
- Gateway 使用 Reactor Resilience4j 处理网关层降级；服务间远程调用使用 API facade 上的 Resilience4j 注解。两者职责不同，不要把网关 fallback 当作服务间调用的唯一保护。

## 标准业务结构

普通系统功能应参考 `lihua-biz/lihua-system` 的结构：

- `entity`：数据库表实体。
- `model/dto`：请求和查询模型。查询、分页或前端入参不要随意污染 Entity，优先新建 DTO。
- `model/vo`：响应模型；多表字段或展示扩展字段可继承 Entity 后追加属性。
- `mapper`：MyBatis-Plus Mapper 接口，继承 `BaseMapper<Entity>`。
- `mapper/xml`：自定义 SQL XML，与 Mapper 包保持同层结构。
- `service`：业务接口。
- `service/impl`：业务实现，常见写法是继承 `ServiceImpl<Mapper, Entity>`。
- `controller`：Web 管理端接口。
- `controller/app`：App 专用接口。

## Controller 规则

- JSON API 优先继承 `ApiResponseController` 并返回 `ApiResponseModel<T>`，方便 SpringDoc 推导返回类型。
- 正常响应使用 `success(...)`，明确错误使用 `error(ResultCodeEnum, message)`。
- 使用 `@Tag` 和 `@Operation` 描述接口文档。
- 需要校验时，在 Controller 和请求体上使用 `@Validated` 或 `@Valid`，并沿用现有校验分组。
- 角色或权限保护使用 `@PreAuthorize`。现有管理员操作常用 `hasRole('ROLE_admin')`。
- 新增、修改、删除、导入、导出、下载、状态变更等重要操作使用 `@Log(description = ..., type = LogTypeEnum.*)`；密码等敏感参数要排除。
- 文件下载或导出接口应参考现有 `ExcelUtils.export(...)` 和附件流式下载模式，不要返回普通 JSON。

## 数据和持久化规则

- 分页 DTO 应沿用现有基础分页 DTO 模式，并在 Controller 中按已有做法使用 `MaxPageSizeLimit` 等校验分组。
- 分页 Service 返回 `IPage<VO>` 或 `IPage<Entity>`。
- 简单 CRUD 优先使用 MyBatis-Plus Wrapper 和内置能力；只有复杂查询、多表联查或 VO 投影才新增 Mapper XML。
- XML 名称与 Mapper 接口保持一致，例如 `SysUserMapper.java` 对应 `mapper/xml/SysUserMapper.xml`。
- 通用字段、自动填充和基础能力若已由 base 模块处理，不要在每个 Service 中重复手动设置。

## 平台接口

- Web 管理端 Controller 通常使用 `system/user` 这类路径。
- App Controller 放在 `controller/app`，通常使用 `app/system/profile` 这类路径。
- 如果某个领域已经有更窄的 App 专用接口，不要直接让 App 复用 Web 管理端接口。

## 共享能力

- 字典：使用后端字典模块维护字典数据和翻译，并与前端 `dict-tag`/Dict 工具配合。
- 附件：使用现有附件和存储服务处理上传、下载、秒传、分片和存储供应商行为。
- 日志：优先使用 `lihua-base-log` 的注解能力，不要临时写审计逻辑。
- 安全：使用基础安全模块和方法级安全注解；除非现有安全工具要求，不要手写认证判断。
- WebSocket：通知类实时能力优先使用基础 websocket 模块。
- 远程调用：优先使用 `lihua-base-client` 和 `lihua-api`，在 API facade 层加熔断降级，不要绕过服务发现手写固定服务地址。

## SQL 和种子数据

- 功能涉及默认菜单、权限、字典、配置或表结构时，参考并维护 `res/db/lihua.sql`。
- 新增字典或配置 key 时，要保持后端常量/枚举和 SQL 种子值同步。

## 验证

- 在 `lihua-cloud/` 下运行 `mvn test` 做广义验证。
- 如果只影响单个模块，可使用 Maven `-pl <module> -am` 做更快的定向检查，例如 `mvn -pl lihua-biz/lihua-system -am test`。
- 改动 `lihua-api`、`lihua-base`、父 POM 或跨服务契约时，优先运行覆盖调用方和被调用方的检查。
- 如果 MySQL、Redis、Nacos、对象存储等服务不可用，仍应运行不依赖这些服务的编译/测试检查，并说明剩余验证缺口。
