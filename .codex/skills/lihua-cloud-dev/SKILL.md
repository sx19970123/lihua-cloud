---
name: lihua-cloud-dev
description: 指导狸花猫后台管理系统微服务版仓库中的 AI 辅助开发。适用于跨端业务功能、Spring Cloud 微服务后端 + Vue Web + UniApp App 协同改造、RBAC 菜单/权限/字典/附件/日志变更，或需要先理解三个子工程协作关系再修改代码的任务。
---

# 狸花猫微服务版全栈开发

当任务跨越多个狸花猫微服务版子工程，或暂时无法确定应该改哪一端时，使用本技能。

## 仓库结构

- 后端微服务：`lihua-cloud/`，Maven 多模块 Spring Cloud 工程。
- Web 管理端：`lihua-vue/`，Vue 3 + Vite + Ant Design Vue 工程。
- 移动端：`lihua-app/`，UniApp + Vue 3 工程。
- 共享资源：`res/`，包含数据库 SQL、Nacos 配置、Docker 部署等资源。

规划或实现跨端功能前，先阅读 `references/full-stack-workflow.md`。

## 默认流程

1. 判断功能是否涉及数据库、RBAC 菜单、权限标识、字典、附件、日志、系统配置或跨服务调用。
2. 先确认后端服务归属和接口契约；跨服务能力先维护 `lihua-api` client/facade，再实现服务端 Controller/Service。
3. 同步 Web 管理端 API/类型，再实现页面和组件。
4. 如果功能需要移动端入口，同步 App API/类型，再实现页面或分包。
5. 对每个受影响子工程运行最小但有效的验证命令。

## 分层技能

- 使用 `lihua-cloud-backend` 处理微服务后端模块、Gateway、Auth、业务服务、远程调用、持久化、RBAC、日志、字典、附件、WebSocket、Nacos 配置或后端验证。
- 使用 `lihua-cloud-web` 处理 Vue 管理端 API、页面、路由、状态、组件、权限展示、表格、表单和主题。
- 使用 `lihua-cloud-app` 处理 UniApp API、页面、分包、状态、路由守卫、移动端 UI、通知和 App 专用接口。

如果任务只涉及单端，优先使用对应单端技能。如果任务涉及端到端契约，从本技能开始，再按需阅读各端 reference。
