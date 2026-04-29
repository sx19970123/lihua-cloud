# 跨端开发流程

## 项目形态

- `lihua-cloud/` 是 Spring Cloud 微服务后端。顶层父工程是 `lihua-cloud/pom.xml`。
- 可运行后端服务包括 `lihua-gateway`、`lihua-auth`、`lihua-biz/lihua-system`、`lihua-biz/lihua-file`、`lihua-biz/lihua-monitor`。
- 公共基础能力位于 `lihua-base/lihua-base-*`；跨服务调用契约位于 `lihua-api/lihua-api-system`。
- `lihua-vue/` 是 Web 管理端。业务 API 在 `src/api`，页面在 `src/views`，全局状态在 `src/stores`，公共组件在 `src/components`。
- `lihua-app/` 是 UniApp 移动端。API 在 `src/api`，主包页面在 `src/pages`，分包页面在 `src/subpackages`，状态在 `src/stores`，路由拦截在 `src/router`。
- `res/db/lihua.sql` 是基础数据库脚本。涉及默认菜单、权限、字典、配置或表结构时，以它作为种子数据参考。
- `res/nacos/nacos_config.zip` 和各服务 `application-*.yml` 是 Nacos/环境配置参考。

## 跨端功能顺序

1. 先确认后端服务归属：系统/RBAC/字典/配置通常在 `lihua-system`，附件在 `lihua-file`，认证在 `lihua-auth`，网关行为在 `lihua-gateway`，监控在 `lihua-monitor`。
2. 如果功能跨服务调用，先在 `lihua-api/lihua-api-system` 等 API 模块维护 `@RemoteClient` + `@HttpExchange` 契约和 facade，再由调用方依赖 facade；不要在业务服务里手写固定 URL、RestTemplate 或 WebClient。
3. 服务端实现优先沿用已有统一返回、DTO/VO、校验分组、日志注解、安全注解和 MyBatis-Plus 模式。
4. 保持接口前缀一致：
   - Web 管理端接口通常使用 `system/...`。
   - App 接口通常使用 `app/system/...`，后端实现通常位于 `controller/app`。
5. 在 `lihua-vue/src/api/<domain>/<feature>/` 新增或更新 Web API，并在 `type/` 下维护 TypeScript 类型。
6. 在 `lihua-vue/src/views/<domain>/<feature>/` 新增或更新 Web 页面。静态路由在 `src/router/index.ts`，多数业务菜单通过系统菜单动态配置。
7. 在 `lihua-app/src/api/<domain>/<feature>/` 新增或更新 App API，并在 `type/` 下维护 TypeScript 类型。
8. 在 `lihua-app/src/pages.json` 注册 App 页面。公共页和 tabBar 页放 `src/pages`，业务功能页优先放 `src/subpackages`，除非必须进主包。

## 契约规则

- 两个前端都复用已有全局响应类型和分页响应类型。
- DTO 请求字段应与后端 DTO 对齐；列表/详情展示字段应与后端 VO 对齐。
- 后端接口如果使用 `@PreAuthorize`，要让对应菜单/权限配置可被开发者发现；必要时更新 SQL 菜单种子数据。
- 如果字段由字典驱动，应维护字典数据，并在前端使用现有字典工具或组件，不要硬编码标签。
- 附件相关能力应复用现有附件上传/下载组件和后端附件存储服务。
- 不要让前端绕过 Gateway 直接依赖内部服务地址；前端 URL 应与现有请求封装和网关路由保持一致。
- 跨服务 facade 方法优先使用 Resilience4j `@CircuitBreaker`；有明确降级结果时提供 `fallbackMethod`，fallback 参数与原方法一致并追加 `Throwable`。
- 各服务通过 Nacos 引入 `lihua-resilience.yaml` 统一 resilience 配置；调整熔断窗口、失败率、打开等待时间或超时时间时，维护 Nacos 配置种子或目标环境配置。

## 验证命令

- 后端：在 `lihua-cloud/` 下运行 `mvn test`；如果只影响某个模块，可运行更窄的 Maven 模块检查，例如 `mvn -pl lihua-biz/lihua-system -am test`。
- Web：在 `lihua-vue/` 下运行 `npm run type-check`；需要验证生产构建时运行 `npm run build`。
- App：在 `lihua-app/` 下运行 `npm run type-check`；只有涉及具体平台时才运行对应 `dev:*` 或 `build:*` 命令。

优先运行能覆盖本次改动的最小检查。如果命令因为 MySQL、Redis、Nacos、对象存储或其他服务不可用而无法完整运行，要明确说明剩余验证缺口。
