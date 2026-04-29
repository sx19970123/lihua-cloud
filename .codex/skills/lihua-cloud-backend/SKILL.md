---
name: lihua-cloud-backend
description: 指导狸花猫微服务版仓库中的 Spring Cloud 后端开发。适用于 Maven 多模块、Gateway、Auth、业务服务、Nacos、HttpExchange 远程调用、Controller、Service、MyBatis-Plus Mapper、DTO/VO/Entity、RBAC 安全、日志、字典、附件、WebSocket、系统配置、数据库 SQL 和后端验证。
---

# 狸花猫微服务版后端开发

处理 `lihua-cloud/` 下的服务端改动时，使用本技能。

修改后端代码前，先阅读 `references/backend-development.md`。如果是跨端功能，也要参考 `lihua-cloud-dev`。

## 核心规则

- 普通业务代码放到合适的 `lihua-biz` 服务模块；只有可复用基础能力才放入 `lihua-base`。
- 跨服务调用契约放在 `lihua-api`，使用 `@RemoteClient` + Spring `@HttpExchange`，调用方优先依赖 facade。
- 遵循现有 `controller`、`service`、`service/impl`、`mapper`、`mapper/xml`、`model/dto`、`model/vo`、`entity` 结构。
- 面向前端的 JSON API 优先使用 `ApiResponseController` + `ApiResponseModel<T>`。
- 使用 MyBatis-Plus 约定和项目已有分页能力。
- 受保护的管理端写操作要一致使用 `@PreAuthorize` 和 `@Log`。
- App 专用接口放在 `controller/app`，接口路径使用 `app/...` 前缀。

## 验证

后端检查在 `lihua-cloud/` 下运行。优先对受影响模块运行定向 Maven 检查；改动共享模块、远程调用契约或公共配置时，再运行更广的检查。
