# 狸花猫后台管理系统（Lihua Cloud）

> 一套基于 **Spring Cloud + Vue3 + UniApp** 的现代化 RBAC 权限管理系统（微服务版）

[![Gitee Stars](https://gitee.com/yukino_git/lihua-cloud/badge/star.svg?theme=dark)](https://gitee.com/yukino_git/lihua-cloud/stargazers) [![GitHub Stars](https://img.shields.io/github/stars/sx19970123/lihua-cloud)](https://github.com/sx19970123/lihua-cloud) <a href="https://gitcode.com/weixin_44118742/lihua-cloud" target="_blank"><img src="https://gitcode.com/weixin_44118742/lihua-cloud/star/badge.svg" alt="GitCode Star"/></a>

## 🧩 项目仓库

**3.0 起项目按端拆分为四个独立仓库**，共用统一的账号、权限与数据模型，可按需组合使用：

| 仓库 | 说明 | 地址 |
|------|------|------|
| lihua | 后端 · Spring Boot 单体版 | https://gitee.com/yukino_git/lihua |
| lihua-cloud | 后端 · Spring Cloud 微服务版（本仓库） | https://gitee.com/yukino_git/lihua-cloud |
| lihua-web | 前端 · Vue3 管理端（Antdv Next 组件库） | https://gitee.com/yukino_git/lihua-web |
| lihua-app | 移动端 · UniApp（Android / iOS / 鸿蒙 / 微信小程序） | https://gitee.com/yukino_git/lihua-app |

> Web 端与移动端可同时对接单体版与微服务版后端，接口契约保持一致。
> 按项目规模选择：快速起步、部署简单选单体版；需要弹性伸缩、服务隔离选微服务版。

## 📚 文档

- 📖 开发文档：https://doc.lihua.xyz（含 1.0 / 2.0 / 3.0 全版本）
- 🤖 AI 文档（DeepWiki）：https://deepwiki.com/sx19970123/lihua-cloud
- 🎥 功能介绍视频：https://www.bilibili.com/video/BV14Z1oY8EKh/

## 💬 交流反馈

- 欢迎提交 Issues（功能建议 / Bug / 优化建议）
- QQ 交流群：850464676

## 🛠 主要技术栈

- Java 25（虚拟线程默认开启）
- Spring Boot 4.0.8 / Spring Cloud 2025.1.3 / Spring Cloud Alibaba 2025.1.0.0
- Nacos v3.1.1（注册中心 + 配置中心）
- Spring Cloud Gateway / LoadBalancer / Resilience4j
- MyBatis-Plus 3.5.17 / MySQL 8+
- Redis（Redisson + Caffeine 二级缓存）

## 🖥 服务列表

| 服务 | 默认端口 | 职责 |
|------|----------|------|
| lihua-gateway | 8085 | 网关：路由转发、JWT 预校验、IP 黑名单、traceId、熔断降级 |
| lihua-auth | 8082 | 认证中心：登录 / 注册 / 验证码 / 一次性令牌 |
| lihua-system | 8084 | 核心业务：RBAC、字典、通知公告、日志、系统设置 |
| lihua-file | 8083 | 附件服务：上传 / 秒传 / 分片 / 签名下载 |
| lihua-monitor | 8081 | 监控服务：在线用户、缓存监控、服务器监控 |
| lihua-websocket | 8086 | WS 连接服务：连接持有与消息推送（无库，可多实例） |

## 📁 目录结构

``` bash
lihua-cloud/
├── lihua-api/              # 服务间 RPC 契约（client 接口 + facade + model）
├── lihua-auth/             # 认证中心
├── lihua-gateway/          # 网关服务
├── lihua-websocket/        # WS 连接服务（第六服务，连接层代码 + 启动引导一体）
├── lihua-base/             # 基础能力层（含 client 远程调用框架与 ws 消息边界，共 15 个子模块）
├── lihua-biz/              # 业务服务（lihua-system / lihua-file / lihua-monitor）
├── deploy/                 # 数据库脚本、nacos 配置导出、docker 部署编排
├── LICENSE
└── README.md
```

## 🚀 快速开始

1. 准备环境：JDK 25、MySQL 8.0+、Redis、Nacos v3.1.1
2. 导入数据库脚本 `deploy/db/lihua.sql`（升级场景执行 `deploy/db/upgrade-3.0.0.sql`）
3. 导入 Nacos 配置：`deploy/nacos/nacos_config_export.zip`（lihua-common / lihua-resilience + 五服务各自配置）
4. 配置环境变量：`NACOS_ADDR` / `NACOS_USERNAME` / `NACOS_PASSWORD`，以及 `MYSQL_*`、`REDIS_*`、`ATTACHMENT_DOWNLOAD_SIGN_KEY`（3.0 必配）
5. 依次启动 `lihua-system` → `lihua-file` → `lihua-monitor` → `lihua-auth` → `lihua-websocket` → `lihua-gateway`
6. 配合 [lihua-web](https://gitee.com/yukino_git/lihua-web) 访问 `http://localhost:90`（代理目标改为网关地址）

### 🔐 数据库脚本默认账号

| 账号 | 密码 |
|------|------|
| admin | admin123 |

## 🧠 项目简介

本仓库为狸花猫系统的 **Spring Cloud 微服务版后端**，与单体版业务能力一致，额外提供服务治理能力：

- 🔗 **声明式 RPC**：`@RemoteClient` 注解即接口，LoadBalancer 服务名寻址，Resilience4j 熔断兜底
- 🔏 **内部签名机制**：服务间调用自动附加 HMAC-SHA256 签名，`@InternalOnly` 端点常量时间验签
- 🧩 **Nacos 配置中心**：公共配置 / 熔断配置 / 服务配置分组管理，优雅停机全链路支持
- 🚪 **统一网关**：JWT 合法性预校验、IP 黑名单、traceId 注入、路由级熔断降级
