# 使用 Docker 部署应用程序

## 文件结构

```text
├── docker                                                # Docker 部署目录
│   ├── compose.yaml                                      # Docker Compose 编排文件
│   ├── client                                            # Web 前端服务目录
│   │   ├── dist                                          # lihua-vue 打包后的 dist 目录（需自行添加或替换）
│   │   ├── nginx.conf                                    # Nginx 配置
│   │   ├── dockerfile                                    # 前端镜像构建文件
│   ├── lihua-auth                                        # 认证中心服务目录
│   │   ├── lihua-auth-exec.jar                           # 后端打包后的 jar 文件（需自行添加）
│   │   ├── dockerfile                                    # 后端镜像构建文件
│   ├── lihua-file                                        # 文件服务目录
│   │   ├── lihua-file-exec.jar                           # 后端打包后的 jar 文件（需自行添加）
│   │   ├── dockerfile                                    # 后端镜像构建文件
│   ├── lihua-gateway                                     # 网关服务目录
│   │   ├── lihua-gateway-exec.jar                        # 后端打包后的 jar 文件（需自行添加）
│   │   ├── dockerfile                                    # 后端镜像构建文件
│   ├── lihua-monitor                                     # 监控服务目录
│   │   ├── lihua-monitor-exec.jar                        # 后端打包后的 jar 文件（需自行添加）
│   │   ├── dockerfile                                    # 后端镜像构建文件
│   ├── lihua-system                                      # 核心业务服务目录
│   │   ├── lihua-system-exec.jar                         # 后端打包后的 jar 文件（需自行添加）
│   │   ├── dockerfile                                    # 后端镜像构建文件
```

## 部署说明

请确保服务器已安装 Docker 和 Docker Compose，并且 80、443、3306、6379、8090、8848、9848 端口未被占用，或已在 `compose.yaml` 中调整端口映射。

### 准备打包文件

后端服务在 `lihua-cloud` 目录下打包：

```shell
cd lihua-cloud
mvn clean package -DskipTests
```

将生成的 exec jar 复制到 Docker 部署目录：

```shell
cp lihua-auth/target/lihua-auth-exec.jar ../res/docker/lihua-auth/
cp lihua-gateway/target/lihua-gateway-exec.jar ../res/docker/lihua-gateway/
cp lihua-biz/lihua-system/target/lihua-system-exec.jar ../res/docker/lihua-system/
cp lihua-biz/lihua-file/target/lihua-file-exec.jar ../res/docker/lihua-file/
cp lihua-biz/lihua-monitor/target/lihua-monitor-exec.jar ../res/docker/lihua-monitor/
```

前端在 `lihua-vue` 目录下打包：

```shell
cd lihua-vue
npm install
npm run build
```

将生成的 `dist` 目录复制或替换到 `res/docker/client/dist`。

### client

`client` 为 Web 管理端部署目录，包含：

- `dist`：`lihua-vue` 打包产物。
- `nginx.conf`：Nginx 配置文件，会覆盖镜像中的 `/etc/nginx/nginx.conf`。
- `dockerfile`：基于 `nginx` 构建前端镜像。

镜像构建时会将 `dist` 复制到 `/usr/share/nginx/html/dist`，并通过 Nginx 暴露 80 端口。当前 compose 将宿主机 `80:80`、`443:443` 映射到前端容器。

Nginx 已配置：

- `/`：访问前端页面。
- `/prod-api/`：反向代理到 `lihua-gateway-server:8080`。
- `/ws-connect`：反向代理 WebSocket 到网关服务。

生产发布前端时，替换 `client/dist` 后重建 `client` 服务即可。

### server

后端服务包含 `auth-server`、`system-server`、`file-server`、`monitor-server`、`gateway-server` 五个容器。

各服务镜像基于 `eclipse-temurin:25.0.4_7-jre-noble`（与 Java 25 编译目标匹配；镜像内含 fontconfig 与 DejaVu 字体，满足验证码字体渲染）构建，构建时将对应的 `*-exec.jar` 复制到 `/app/` 目录，容器启动时执行 `java -jar`。

当前服务端口如下：

| 服务 | 容器名 | 容器端口 | 说明 |
| --- | --- | --- | --- |
| `auth-server` | `lihua-auth-server` | `8081` | 认证中心 |
| `system-server` | `lihua-system-server` | `8082` | 核心业务服务 |
| `file-server` | `lihua-file-server` | `8083` | 文件服务 |
| `monitor-server` | `lihua-monitor-server` | `8084` | 监控服务 |
| `gateway-server` | `lihua-gateway-server` | `8080` | 网关服务 |

后端容器默认不直接暴露到宿主机，外部请求通过 `client` 容器的 Nginx 代理进入网关。部署时可通过 `compose.yaml` 中的环境变量调整端口、Nacos 地址、命名空间和账号密码：

- `SERVER_PORT`
- `NACOS_USERNAME`
- `NACOS_PASSWORD`
- `NACOS_ADDR`
- `NACOS_NAMESPACE`
- `TZ`

### mysql

`mysql` 使用 `mysql:8.0` 镜像，容器名为 `lihua-mysql`，默认映射 `3306:3306`。

当前默认配置：

- root 密码：`password`
- 配置卷：`mysql-conf`
- 数据卷：`mysql-data`

首次部署后，需要导入项目数据库脚本 `res/db/lihua.sql`。可以使用 Navicat、DataGrip 等工具连接宿主机 `3306` 端口后执行，也可以进入容器执行导入命令。

生产环境请务必修改 `MYSQL_ROOT_PASSWORD`，并根据需要限制数据库端口对外暴露。

### redis

`redis` 使用 `redis:7` 镜像，容器名为 `lihua-redis`，默认映射 `6379:6379`。

当前启动命令为：

```shell
redis-server --requirepass password
```

生产环境请修改 Redis 密码，并按实际安全策略决定是否暴露 `6379` 端口。

### nacos

`nacos` 使用 `nacos/nacos-server:v3.1.1` 镜像，容器名为 `lihua-nacos`，以单机模式启动。

当前默认配置：

- 控制台端口：宿主机 `8090` 映射到容器 `8080`
- 服务端口：`8848`
- gRPC 端口：`9848`
- 鉴权：已开启
- 默认账号密码：`nacos` / `nacos`
- 命名空间：后端服务默认使用 `prod`

首次部署时，需要将 `res/nacos/nacos_config_export.zip` 导入到 Nacos，并确认配置中的 MySQL、Redis、网关地址、文件存储等参数与当前 compose 环境一致。

### compose.yaml

启动前请根据服务器实际情况修改 `compose.yaml` 中的密码、端口、Nacos token、命名空间和 JVM 参数。

将 `res/docker` 目录上传到服务器后，在该目录执行：

```shell
docker compose up -d --build
```

当前 compose 会启动以下容器：

- `lihua-web-client`
- `lihua-auth-server`
- `lihua-system-server`
- `lihua-file-server`
- `lihua-monitor-server`
- `lihua-gateway-server`
- `lihua-mysql`
- `lihua-redis`
- `lihua-nacos`

如果是首次部署，建议启动基础组件并完成数据初始化后，再启动业务服务：

```shell
docker compose up -d mysql redis nacos
```

导入 `res/db/lihua.sql` 和 `res/nacos/nacos_config_export.zip`，确认配置无误后再执行：

```shell
docker compose up -d --build auth-server system-server file-server monitor-server gateway-server client
```

查看容器状态和日志：

```shell
docker compose ps
docker compose logs -f gateway-server
docker compose logs -f auth-server
```

配置仅包含基础项目启动，更多生产需求请结合实际情况修改 `dockerfile`、`compose.yaml`、`nginx.conf` 和 Nacos 配置。

## 容器健康与自愈

五个后端服务引入 actuator 探针（健康端点挂各服务主端口 `/actuator/health`，Nacos `lihua-common.yaml` 的 `management` 段统一配置——主端口每服务天然唯一，本机多服务同跑无冲突；SecurityConfig 白名单放行且网关无该前缀路由，仅内网/本机可达）。compose 据此为每个容器配置了体检与自愈：

- **healthcheck 定时体检**：后端服务探各自主端口的 `/actuator/health`（聚合数据库/Redis 连通性）；mysql/redis/nacos 用各自官方命令探活。`docker compose ps` 的 STATUS 列显示 `(healthy)` 即体检通过。
- **depends_on 启动排序**：四个业务服务等 mysql/redis/nacos 全部 `(healthy)` 后才启动（首次部署不再需要手动分步起基础组件，直接 `docker compose up -d --build` 即可）；gateway 等 nacos；前端等 gateway。
- **restart: unless-stopped 宿主机重启自愈**：服务器重启后 Docker 自动拉起全部容器（手动 `docker compose stop` 停掉的不会被拉起，尊重运维意图）。
- **资源与日志**：各容器已设 `mem_limit`（JVM 堆经 `-XX:MaxRAMPercentage=75.0` 跟随容器限额），日志统一 json-file 轮转（单文件 10MB × 3 份）。
- **OOM 行为**：JVM 内存溢出时异常栈进容器日志（`docker compose logs` 可见），堆快照 dump 到各服务数据卷（`java_pid*.hprof`，约等于堆大小——排查 OOM 的现场材料，低频事件手动清理），随后进程立即退出交由 restart 自愈——OOM 半死僵尸态不会出现。

### 优雅停机与更新

后端服务开启优雅停机（`server.shutdown=graceful`，收尾超时 30s）：容器收到停止信号后先从 Nacos 注销实例（负载均衡摘流）、停止接收新请求、等待在途请求完成（最长 30s，compose `stop_grace_period=35s` 兜底）。更新版本时 `docker compose up -d --build --force-recreate <服务>` 的单服务重建即处于该语义下——大文件上传/流式下载等长请求若超 30s 仍会被截断，安排在低峰期更新。

### 容器时钟同步要求

服务间内部调用带 HMAC 防重放验签（时间窗 10s，`rpc.signTimeout`），**宿主机时钟漂移超过该窗口会导致内部 RPC 鉴权失败**（表现为服务间调用 401 类错误）。服务器须启用 NTP/chrony 等时钟同步；多机部署时所有宿主机统一对时。

## 卷映射

通过卷映射可以持久化数据库、缓存、前端资源、后端 jar 包和服务数据。

Docker 命名卷默认位于服务器 `/var/lib/docker/volumes` 目录下。

- `mysql-conf`：MySQL 配置目录，挂载到 `/etc/mysql/`。
- `mysql-data`：MySQL 数据目录，挂载到 `/var/lib/mysql`。
- `redis-data`：Redis 数据目录，挂载到 `/data/`。
- `dist-resource`：前端资源目录，挂载到 `/usr/share/nginx/html/`。
- `auth-server-data`：认证中心服务数据目录，挂载到 `/lihua-auth/data/`。
- `auth-jar-resource`：认证中心 jar 目录，挂载到 `/app/`。
- `system-server-data`：核心业务服务数据目录，挂载到 `/lihua-system/data/`。
- `system-jar-resource`：核心业务服务 jar 目录，挂载到 `/app/`。
- `file-server-data`：文件服务数据目录，挂载到 `/lihua-file/data/`。
- `file-jar-resource`：文件服务 jar 目录，挂载到 `/app/`。
- `gateway-server-data`：网关服务数据目录，挂载到 `/lihua-gateway/data/`。
- `gateway-jar-resource`：网关服务 jar 目录，挂载到 `/app/`。
- `monitor-server-data`：监控服务数据目录，挂载到 `/lihua-monitor/data/`。
- `monitor-jar-resource`：监控服务 jar 目录，挂载到 `/app/`。

注意：各后端服务同时在镜像内复制 jar，并将 `/app/` 映射为命名卷。首次创建命名卷时 Docker 会将镜像内 `/app/` 的 jar 初始化到卷中；后续如果只替换部署目录中的 jar，需要重建镜像并重建对应容器，或直接替换对应 `*-jar-resource` 卷中的 jar。

## 更新版本

### 更新前端

重新打包 `lihua-vue`，替换 `res/docker/client/dist` 后执行：

```shell
docker compose up -d --build --force-recreate client
```

如果只替换了 `dist-resource` 卷中的静态文件，可重载或重启前端容器：

```shell
docker compose restart client
```

### 更新后端

重新打包目标服务，替换对应目录下的 `*-exec.jar` 后，重建对应容器。

示例：更新网关服务。

```shell
cp lihua-cloud/lihua-gateway/target/lihua-gateway-exec.jar res/docker/lihua-gateway/
cd res/docker
docker compose up -d --build --force-recreate gateway-server
```

其他后端服务名称对应如下：

- `auth-server`：`lihua-auth/lihua-auth-exec.jar`
- `system-server`：`lihua-system/lihua-system-exec.jar`
- `file-server`：`lihua-file/lihua-file-exec.jar`
- `monitor-server`：`lihua-monitor/lihua-monitor-exec.jar`
- `gateway-server`：`lihua-gateway/lihua-gateway-exec.jar`

### 更新配置

- 修改 `compose.yaml` 后，执行 `docker compose up -d` 使配置生效。
- 修改 `client/nginx.conf` 后，执行 `docker compose up -d --build --force-recreate client`。
- 修改 Nacos 配置后**不热生效**：rpc 超时/连接池、resilience4j 熔断、数据源、redisson 等启动期组件的参数修改后需重启对应后端容器；仅业务运行时读取的自定义配置项随推送即时生效。


## 3.0 升级部署必读

1. **先执行数据库升级**：导入本仓库 `deploy/db/upgrade-3.0.0.sql`（幂等可重跑），再启动服务；基线新装直接用 `lihua.sql`（含全部变更）。
2. **下载链接签名密钥必配**：3.0 起 `attachment.download-sign-key` 缺失服务启动失败（fail-fast）。docker 部署在 compose 同目录 `.env` 提供：
   ```
   ATTACHMENT_DOWNLOAD_SIGN_KEY=<openssl rand -hex 32 生成，至少 16 字符>
   ```
   （compose 已透传该变量；mono 生产 yml 与 cloud Nacos lihua-file.yaml 均以 `${ATTACHMENT_DOWNLOAD_SIGN_KEY}` 占位。）
3. **Nacos 配置重导入（cloud）**：`deploy/nacos/nacos_config_export.zip` 已更新（附件 attachment 段 3.0 形态、附件路由超时 10m、路由显式 order、uploadFilePath 指向数据卷），升级后需在 Nacos 重新导入并发布。
4. **附件存储卷（cloud）**：lihua-file 的 `uploadFilePath` 已指向 `/lihua-file/data/upload/`（落在 `file-server-data` 卷）；请勿改回相对路径，否则容器重建附件丢失。
