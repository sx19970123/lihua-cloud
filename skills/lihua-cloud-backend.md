---
name: lihua-cloud-backend
description: 指导 lihua-cloud 仓（Maven 多模块 Spring Cloud 微服务后端：gateway/auth/system/file/monitor 五服务 + Nacos 配置中心）的服务端开发与验证。适用于 Maven 模块与服务落位、Gateway 路由与过滤器、@RemoteClient 远程调用、Resilience4j 熔断降级、Controller/Service/Mapper、RBAC 安全、日志、字典、附件、WebSocket、Nacos 配置、数据库 SQL，以及二开边界、与 lihua 单体仓的 base 层双仓同步、与 lihua-web / lihua-app 的契约对齐。
---

# 狸花猫微服务版后端开发（lihua-cloud 仓）

处理本仓服务端改动时使用。本 skill 自包含——与单体仓（`lihua`）同基线的规约直接写在本文并标注「**双仓同基线**」（与 lihua 仓对应章节同源，**改动任一侧须同步另一侧**，同差异地图纪律）；微服务特有的差异单独成文。兄弟仓：`lihua`（单体后端）、`lihua-web`（Vue 管理端）、`lihua-app`（UniApp 移动端）——两端共用同一套 web/app 代码库。

## 仓库结构

- Maven 父工程：本仓根 `pom.xml`，顶层 `<modules>` 为 `lihua-base`、`lihua-websocket`、`lihua-biz`、`lihua-api`、`lihua-auth`、`lihua-gateway`；编译版本 `java.version=25`。
- 基础模块（`lihua-base/`，15 个）：attachment、cache、captcha、**client**、common、dict、doc、excel、job、log、mybatis、security、sensitive、web、**ws**（`lihua-base-client` 是远程调用基建，mono 无此模块；无独立 ip 模块——IP 能力收口在 base-web/WebUtils）。
- **WS 消息面边界 `lihua-base-ws`**（base 下公共库，业务服务随手引；mono 仓同名同位）：包 `com.lihua.ws.push`（下行投递 `WebSocketPushUtils` + `WsPushMessage`）+ `com.lihua.ws.receive`（上行契约：SPI `WsMessageReceiver` + 回写通道 `WsReply` + 帧实体 `WsClientMessage`——lihua-websocket 进程内 handleTextMessage 解析帧后按 type 分发；内置心跳参考实现 `receive/impl/HeartbeatWsMessageReceiver`（客户端 30s 一跳 ping→服务端回 pong，不做超时踢线）。**处理器生效范围=连接所在进程（lihua-websocket 服务）**，业务服务（system 等）进程注册的处理器收不到调用——跨服务上行业务处理预留 WS 上行 Redis topic 桥接，勿用进程内 event（不跨进程））。业务与连接层的中间层，投递唯一入口；依赖 base-cache（Redis 原语）。
- **WS 连接服务 `lihua-websocket`（cloud 第六服务，顶层）**：连接层代码 + 服务启动引导一体（`LiHuaWebSocketApplication`，端口 8086，nacos 注册名 `lihua-websocket`）；与 mono 仓同名模块的关系同 lihua-system 双仓关系——**连接层源码双仓一致，cloud 侧多服务启动引导**（mono 侧为纯库由 admin 装配引入）。持有 `/ws-connect` 连接，订阅 Redis pub/sub 扇出推送本地会话；**可多实例水平扩容**（每实例各推自己持有的连接，天然扇出）。业务服务零依赖本模块。
- 远程调用契约模块：`lihua-api/lihua-api-system`，包含 `client`、`facade`、`model` 三层。
- 可运行服务（六服务）：
  - `lihua-gateway`：Spring Cloud Gateway 网关，外部唯一入口。过滤器三件 = `RequestIpFilter`（IP 黑名单）、`RequestTokenFilter`（**只拦非法 token——无 token 直接放行，鉴权由各服务 SecurityConfig 裁决**）、`TraceIdFilter`；另有 FallbackController（CB 降级）与 GatewayExceptionHandler。路由配置在 nacos `lihua-gateway/lihua-gateway.yaml`（**路由顺序承重**：具体路径必须先于 `/system/**` 通配——lihua-file 先于 system 的先例写在路由注释里）。
  - `lihua-auth`：认证服务（登录/注册/验证码；AuthenticationManager Bean 在此服务 LoginConfig）。
  - `lihua-biz/lihua-system`：系统/RBAC/字典/配置/通知等核心业务。
  - `lihua-biz/lihua-file`：附件与文件服务。
  - `lihua-biz/lihua-monitor`：监控服务。
  - `lihua-websocket`：WS 连接服务（顶层第六服务，无库基础设施，连接层代码 + 启动引导一体）——持有 `/ws-connect` 连接，订阅 Redis pub/sub 扇出推送本地会话；**可多实例水平扩容**（每实例各推自己持有的连接，天然扇出）。system 不再引入 WS 连接层。
- 启动类使用 `@ComponentScan({"com.lihua.**"})`；含持久化的服务使用 `@MapperScan({"com.lihua.**.mapper"})`；新增包保持在 `com.lihua` 命名空间下。业务枚举在消费服务的 `enums` 包（现例 `lihua-biz/lihua-system/.../com/lihua/system/enums`）；双版本控制器基类统一在 `controller/base` 子包（现存 6 个：system 的 setting/dictData/notice/profile、auth 的 authentication、file 的 attachment storage）。
- **配置双轨**：各服务 `src/main/resources/application.yml` + `application-dev/prod.yml`（经 `spring.config.import` 引入 Nacos 配置）；运行时配置在 Nacos，种子为仓库导出 `deploy/nacos/nacos_config_export.zip`（按服务分组：lihua-common/lihua-system/lihua-file/lihua-auth/lihua-gateway/lihua-monitor/lihua-websocket/lihua-resilience 各一份 yaml）。token 参数唯一来源 `lihua-common.yaml`（如 `token.tokenSecret`，网关验签与 auth 签发共用）；熔断策略统一 `lihua-resilience.yaml`；附件配置 `lihua-file.yaml`（上传模式/100MB 限制/下载时效/签名密钥）。biz 模块不自带业务配置文件。
- 数据面：数据源 dynamic-datasource、Redis Redisson 项目自定义 codec；虚拟线程、Jackson 忽略 null、MyBatis-Plus 逻辑删除 `delFlag`、mapper XML 扫描 `classpath*:com/lihua/**/mapper/**/*.xml`。
- 种子 SQL：`deploy/db/lihua.sql` 基线 + `deploy/db/upgrade-3.0.0.sql`（与 lihua 仓同文件双仓同步）；结构/种子变更走独立幂等脚本（DDL 用 information_schema+PREPARE 判存在，DML 用 WHERE NOT EXISTS 或天然幂等 UPDATE），**脚本按业务名称命名**（迁移期统一 `upgrade-3.0.0.sql`，二开业务如 `upgrade-<业务名>.sql`），涉及双仓的业务双仓同名同文件同步。
- 业务枚举与 mono 同名同值（mono `com.lihua.enums` / cloud `com.lihua.system.enums`）。

## 核心规则

1. **代码落位决策树**：①新增业务 → 在 `lihua-biz` 下新建服务模块（同步 `lihua-biz/pom.xml` 的 `<modules>` 与根 dependencyManagement；接入点见「微服务边界与二开」）；②偏公共、需要单独引入某个依赖实现一类能力的组件（如工作流、消息队列）→ 在 `lihua-base` 下新建对应子项目，向外部暴露接口供业务调用（新增 base 模块同步父工程 dependencyManagement 与消费方依赖）；③简单公共实现（如通用字符串校验）→ 在 `lihua-base-common` 的 `utils` 包中添加，或在现有类基础上扩展；④轻量级工具方法**先查 base 有无对应实现**——没有先补齐公共方法再使用，有则直接调用，勿在服务内手搓 base 已有能力。
2. 服务归属：系统管理/RBAC/字典/配置进 `lihua-system`；文件、附件、上传下载进 `lihua-file`；认证登录、注册、刷新登录态进 `lihua-auth`（如需读取用户资料，通过 `lihua-api` 调用系统服务）；Gateway 只处理路由、鉴权前置、IP/Token 过滤、fallback 和网关异常，不承载普通业务逻辑。
3. JSON API 继承 `ApiResponseController` 返回 `ApiResponseModel<T>`（正常 `success(...)`、错误 `error(ResultCodeEnum, msg)`），配 `@Tag`/`@Operation`；校验用 `@Validated`/`@Valid` 并沿用现有校验分组。
4. 管理端写操作一律 `@PreAuthorize`（现有口径 `hasRole('ROLE_admin')` 粗粒度；细粒度 authorities 通道保留但不消费）+ `@Log(description=..., type=LogTypeEnum.*)`，密码等敏感参数排除；操作日志用 base-log 注解能力。
5. App 专用接口放 `controller/app`，路径 `app/...` 前缀；已有更窄 App 接口的领域不让 App 复用管理端接口。
6. 持久化用 MyBatis-Plus：分页 `POST /page` + `@Validated(MaxPageSizeLimit.class)` + BaseDTO（pageNum/pageSize 上限 999999/100），Service 返回 `IPage<VO>`/`IPage<Entity>`；简单 CRUD 用 Wrapper，复杂查询才加 XML（XML 名与 Mapper 同名同层）；base 模块已处理的能力不在 Service 重复设置。
7. 文件下载或导出接口参考现有 `ExcelUtils.export(...)` 和附件流式下载模式，不返回普通 JSON。
8. 有逻辑消费的字典值一律经 `DictEnum` 枚举引用（细则见「字典与枚举」）。
9. **biz 服务在设计上平级、互不依赖——这正是服务拆分边界**：跨服务数据/动作走 `lihua-api` 的 @RemoteClient RPC（勿直连他服务库表）；用户/部门等平台数据经登录用户上下文 `LoginUserContext`（base-security）的 `CurrentUser` 会话内容获取。

## 字典与枚举（双仓同基线）

- **建枚举判据**：后端有逻辑消费（if/switch、写入特定语义值）的字典才建枚举；纯展示字典（tag 颜色、下拉选项、Excel label）不建，展示走 `DictUtils` 字典缓存（base-dict）。
- **一个字典类型一个枚举**，实现 `base-common` 的 `DictEnum` 接口（`getType()`/`getValue()`）；读判断与写值都走枚举。按消费方分层：跨服务/跨模块消费放 `base-common/enums`，单服务消费放该服务 `enums` 包；出现第二个消费服务时同名同值各自维护或下沉 base-common。域字典枚举一律 `implements DictEnum`（getType 返回字典 typeCode）+ 值集对齐种子（Java 侧全量替换、XML 字面量保持+注释锚定）。
- **三通道**：选项集→字典枚举；运行时布尔（"是否"语义字段）→Boolean 化（动契约需前端同步）；结构标记（树根 `"0"`）→常量。禁止跨字典复用通用枚举——`sys_whether`/`sys_status`/`sys_log_status` 值集同为 0/1 但语义不同，各自建枚举。
- **所有权边界**：字典 value（码）归代码所有，不可在字典 UI 改码（Service 层配禁改 guard，如 sys_status save 直接抛异常）；label/tagStyle/sort 归字典 UI 所有，运行时可改。新增字典值 = 枚举加常量 + 同步种子。
- **新建业务字典三件套**：①枚举（消费服务 enums 包，implements DictEnum）②`sys_dict_type`/`sys_dict_data` 种子（双仓 lihua.sql 基线 + 对应业务命名的幂等脚本段）③存量引用点全量替换为枚举（`AppVersionStatusEnum.PUBLISHED.getValue()` 引用形态）。
- **逻辑删除与缓存铁律**：①逻辑删除表删除后还要按已删行取信息（如清缓存要 typeCode）必须**先查后删**——全局 logic-delete 会给后续 selectList 追加 `del_flag='0'`，已删行查不到；②QueryWrapper 勿再手写 `del_flag='0'`（与全局重复）；③缓存回源 SQL 必须带 `ORDER BY sort ASC`，否则缓存内选项顺序不保证。
- **updateStatus 判空两模式**：无缓存模块前置 selectById 判空抛「xx不存在」（防无效 id 静默假成功）；有缓存模块后置判空仅用于跳过缓存清理。
- **校验注解范式**：save 直收实体的模块，varchar 按 DB 列长补 `@Size(max=N)`（消息「xx长度不能超过N个字符」），char(1) 状态字段补 `@Pattern(regexp="^[01]$")`；主键/数字列不加。
- **校验分组陷阱**：`@Validated(XxxGroup.class)` 只评估挂了该组的约束——补新注解时 `groups` 必须跟随字段既有组集合（或按表单适用组挂），否则静默不生效（Default 组约束在组校验下不评估）。
- **同级排序归一化**：业务表带同组排序列的，保存/删除后调 `SortUtils.normalize(mapper, 组键列名, 组键值)`（base-mybatis，实体 `implements SortEntity`）——组内重编 1..N 只写变化行（单条 CASE 批量，不碰审计字段）。接入点三处：insert 后、update 前取原组键后两组各归一、deleteByIds 删前收集组键删后归一。有缓存域 changed>0 才刷缓存；单组上万行再升级服务专用窗口 SQL。
- **SQL 禁 `${}` 列名拼接**：列名动态的需求拆成列名明确的专用 Mapper 方法。
- **登录态模型字段须与查询列对齐**：随接口序列化的模型（如 `CurrentRole`）只保留 XML 实际查询的列，恒 null 字段（status/delFlag/remark）删除——模型字段必须有数据来源。
- **动作端点 URL 范式**：状态开关 `PUT /资源/status/{id}/{currentStatus}`（URL 去动词）；命令类 `POST /资源/动作名`；查询/保存/删除既有约定不动。前端 api 调用点与端点同一微步原子改。

## 附件域（双仓同基线；归属 lihua-file 服务，配置在 nacos `lihua-file.yaml`）

- **上传面端点一律显式 DTO**，实体不直收（防 id/status/path 经表单注入）；派生字段（originalName/size/type/md5）服务端自文件流派生、uploadMode 端点自证、businessName 缺省回显 businessCode；响应统一 VO（首次访问链接随上传返回）。上传类型限制是可选配置 `attachment.uploadAllowExtensions`（空=不限制，作用全部上传，与附件公开性无关）；上传模式由 `attachment.uploadFileModel` 控制，已实现策略只有 `LOCAL` 与 `ALIYUN-OSS`——配置注释里的其他存储不是已实现能力。
- **契约参数名为 Java 关键字时**：字段名以 `isXxx` 承载 + `@Getter/@Setter(AccessLevel.NONE)` 关闭 Lombok 生成 + 手写 `getXxx/setXxx` 别名访问器——表单（BeanWrapper 按 setter 属性名）与 JSON（Jackson 按属性名）都以契约属性名收参，且 Bean 只暴露单属性不产生双属性。
- **同 md5 多行是常态**（秒传复制行、重复上传各持文件）：按 md5 查找不可任取 `list.get(0)`，判存/秒传统一「任一行物理文件在存即命中」语义（delFlag=0 + status=0 + path 去重逐个 isExists）。**行定位不信任客户端回传 id**（chunk/merge 按 uploadId 查 start 建立的行）。
- **MultipartFile 全链路禁 getBytes**（唯一全量进堆入口）：md5 用 `DigestUtils.md5Hex(InputStream)`（commons-codec，1KB 缓冲流式）；LOCAL 落盘 `FileChannel.transferTo`（零拷贝）；OSS 走 `getInputStream()` 交 SDK——大文件上传内存占用恒定在小缓冲区级别。
- **写侧路径校验三道防线**：落盘路径构造唯一入口 `SysAttachmentStorageServiceImpl#buildUploadFilePath`（拼接 businessCode 段后必经 `FileUtils.checkWritePath`，统一 `/` 分隔）；物理写盘必经 `FileUtils.upload`（LOCAL 策略与分片临时片自动覆盖；OSS putObject 不经 FileUtils，靠构造点校验）。写侧判定=两侧 `toAbsolutePath().normalize()` 后 `Path.startsWith` 目录包含——勿复用下载侧 `checkPath`（它要求目标已存在并做软链解析，首写前不成立）。
- **分片合并原子性与幂等**：合并/生成类落盘一律「唯一临时名 + 校验 + 原子 move（`ATOMIC_MOVE` 降级 `REPLACE_EXISTING`）」，目标只以完整形态出现，异常兜底只清自己的临时文件、**永不删目标**（目标在存=并发另一路的成功产物）。无锁幂等三件套：入口终态短路（行 status 已成功直接返回 VO）+ 失败复查（`isExists` 目标在存按成功收尾，不把成功行覆盖成失败）+ 临时资源清理走 `cleanChunks`（幂等/不抛错；OSS abortMultipartUpload 是计费资源必须正确中止）。md5 复用与秒传同信任模型（声明 md5 + 物理在存即复用命中行 path）；复用行的 originalName/extensionName 跟随本行声明、不复制命中行。
- **HMAC 签名链接**：自签自验短时效链接一律「明文参数 + HMAC 签名」（`SignedUrlUtils`：`<expireMs>.<Base64Url(path)>.<hmacHex(path::expireMs)>`）——**禁止用对称加密伪装防伪**（AES key 随 jar 发布=公开可伪造，加密≠认证）。签名密钥外置 `attachment.download-sign-key`（cloud 在 nacos `lihua-file.yaml`），@PostConstruct fail-fast（缺失启动失败、禁止默认值回退），生产经环境变量/nacos 注入；签名比对必须 `MessageDigest.isEqual` 常量时间（防时序侧信道）；验签失败不区分格式/签名原因（不为探测提供信息），非法→`PARAMS_ERROR(400)`。时效收口单漏斗：`attachment.downloadExpireTime`/`downloadMaxExpireTime` 缺省 1h/30d、默认≤上限启动校验——策略类参数用「代码缺省值+可覆盖」而非硬编码常量；**配置时长一律 `Duration` + `@DurationUnit(ChronoUnit.MINUTES)`**（yml/nacos 写 `1h`/`30d` 带单位、裸数字按分钟兜底），勿用裸 int 分钟数（第三方/框架属性类不受此约束）。
- **下载单路由与数据面分层**：唯一端点 `download` 双参二选一（`key=`私密验签免查库 / `fullPath=`公开行校验），permitAll 链接即凭证；**签发与存储模式无关**（一律 entry 相对链，对象存储绝对 URL 禁止直发客户端）。公开性判定只认行级 `is_public`（Caffeine path→is_public 长 TTL——不可变标记免失效 + 索引兜底查行），目录白名单模式已废弃。数据面 `getDownloadRedirectUrl`：OSS=302 现场短时效预签名（附件字节不过网关，302 缓存必须短于预签时效）、LOCAL=null 直读 File 流式。
- **头像 URL**：统一走 `SysUserService.getAvatarUrl(userId)` / `resolveAvatarUrl(avatarJson)`，勿自行拼 URL——`sys_user.avatar` 是前端 AvatarType JSON 串（value=path、type∈image/icon/text），仅 `type=image` 才有附件 URL（其余/解析失败返回 null）；entry URL 组装一律 `AttachmentUrlUtils.resolvePublicUrl(path, urlBasePath)`（纯函数在 base-common/utils/url，无查库；反代前缀经 `attachment.url-base-path`，服务端不持有部署前缀）；avatarUrl 填充在 CurrentUser 对象上随 userInfo 下发（不单列响应顶层字段，落库与缓存均不依赖）。

## 通用契约（双仓同基线；微服务差异加粗标注）

- **响应码比较必须 equals**：`ResultCodeEnum.code` 与 `ApiResponseModel.code` 均为 Integer——字面量 `200 == resp.getCode()` 走拆箱值比较合法，但换成枚举常量 `SUCCESS.getCode() == ...` 后变为引用比较（200 超出 Integer 缓存 −128~127 必不相等，编译器不报错、运行期静默判否）——**远程响应码判断一律 `ResultCodeEnum.SUCCESS.getCode().equals(resp.getCode())`（微服务下 facade 消费 RPC 响应码是高发场景，先例 auth 5 处）**。
- **校验链下沉 Service 判据**：控制器 `error(ResultCodeEnum.ERROR, msg)` 改抛 `ServiceException(msg)` 的前提是全局异常处理器（`GlobalExceptionHandle` 的 BaseException 通道，base-web，双仓一致）返回同构响应（HTTP 200 + 业务码 + msg），前端 `resp.code !== 200` 分支行为等价；副作用为业务失败多记一条 error 日志（全库既有模式，可接受）。
- **save 接口 Entity 直收 body 是全库既定口径**（dict/post/dept/role/menu/setting + @Validated/分组校验），BaseEntity 字段（delFlag/审计字段）经 body 可注入的面已接受为已知口径——后续体检不再提议收窄 DTO；notice/profile/log/user/attachment 的 DTO 形态是各自模块历史定稿，非全库规范。
- **双版本控制器去重**：管理版与 App 版（`controller/app`）方法体逐行相同时，共有端点收抽象基类（无 @RestController/@RequestMapping/@Tag，`@Resource protected` 注入 Service，方法注解随方法留基类，Spring 继承扫描自动注册路由；**基类统一落 `controller/base` 子包**）。子类只挂各自 `@RequestMapping` 前缀与 `@Tag` 分组；管理版独有端点族由管理版子类声明、App 子类可为空壳；**对外行为双版本一致是去重前提**。推广判据：仅 `@Operation` summary 措辞差异视为可抽；端侧逻辑差异端点一律留子类、不引入钩子方法；注解差异经拍板统一=接受行为对齐；**同路径 HTTP 动词分叉不可抽**；子类/基类跨包必须显式 import。cloud 现存 6 个基类（见仓库结构），auth 基类按仓库现状注入 `SysSettingClientFacade`（RPC 从严降级版 checkCaptcha 两子类本就一致）。
- **异步上下文接力唯一通道 = ContextCopyTaskDecorator**（base-web）：经 Spring executor 提交的任务自动被装饰（提交时快照 MDC+SecurityContext、执行时覆盖恢复、finally 清理；写返回值的 @Async 方法同样被装饰）。**MODE_INHERITABLETHREADLOCAL 策略已下线勿恢复**（正确性依赖「每任务新线程」形态，回池化 executor 即静默数据错误）。**非 executor 线程（手动 new Thread/commonPool/parallelStream/@Scheduled/reactor 调度）明确无上下文**——需要上下文时经 executor 提交或显式传参。setTaskDecorator 单槽。**微服务注意：线程上下文不跨进程——RPC 调用方的会话信息靠 base-client 的 token 透传到目标服务重建**。
- **token/IP 取值唯一源头**：取 token/IP 一律经 `WebUtils.getToken/getIpAddress`（base-web）或 `LoginUserContext`（base-security），任何新代码不得自行读 Authorization/Request-IP 头解析。cloud 侧 IP 链路见「云端运行态」（网关裁决头优先）。**防双击写接口挂 `@PreventDuplicateSubmit`**（base-web，唯一参数 `interval` 秒默认 5；幂等键=同会话 token（匿名退 IP）+URI+参数摘要，SET NX PX 占键、窗口期 TTL 自然过期不删键；重复抛 DuplicateSubmitException → REPEAT_SUBMIT_ERROR(511)）——Redis 占键使幂等**天然跨服务实例生效**（多实例部署不失效）。
- **新表设计默认口径**：业务表继承 `BaseEntity`（base-mybatis，审计字段 + 逻辑删除 `delFlag`）；带状态语义的字段用字典 + 枚举承载（见「字典与枚举」）；有同组排序需求加 `sort` 列且实体 `implements SortEntity`；char(1) 状态列配 `@Pattern`、varchar 按 DB 列长配 `@Size`。
- **跨模块/跨服务解耦分层**：**服务内**用领域事件——`ApplicationEventPublisher` 发事件、监听器消费，勿跨模块直调内部实现；**事件载体统一放 `base-common` 的 `com.lihua.common.model.event/<业务域>/` 子包、类名统一 `Event` 后缀**（可携带数据 `event/log/LogEvent`，也可纯信号空类 `event/setting/CacheBlackIpEvent`；同一业务域多个事件在域包内并列分类）。**微服务关键差异：进程内事件不跨服务、不跨实例传播——跨服务联动只有三条正路：`lihua-api` RPC、独立事实源（如 Redis 标记），或 Redis pub/sub（WS 推送即此形态）**。原「权限更新三件套」（PermissionUpdateEvent + PermissionUpdateEventListener 进程内事件腿）已退役：WS 实时推送统一走 `WebSocketPushUtils`（lihua-base-ws）Redis pub/sub 投递，`PermissionUpdateUtils`（base-security）保留 Redis 标记（事实源）+ markChanged 内 runAfterCommit 后 pub/sub 投递。事务内推送/联动用 `TransactionSendUtils.runAfterCommit`（**base-common** `utils/spring`：有活动事务挂 afterCommit、否则立即执行——事务内直接推送会先于数据提交到达，客户端回拉读不到数据；发布点包裹、勿嵌套）。

## 远程调用契约

- 跨服务调用优先在 `lihua-api/<api-module>` 中定义 `@RemoteClient(serverName = "...")` 接口，并使用 Spring `@HttpExchange`、`@GetExchange`、`@PostExchange` 声明路径；`serverName` 必须与目标服务的 `spring.application.name` 一致（如 `lihua-system`）；路径必须与目标 Controller **精确一致**——改 Controller 路径时同步改 API client。
- 同步调用默认使用 `RestClientFactoryBean`；需要异步时在 `@RemoteClient` 设置 `executionMode = ASYNC`，由 `WebClientFactoryBean` 创建代理。
- `lihua-base-client` 已统一处理服务发现负载均衡、token 透传、签名、连接超时和响应超时——**不要手写固定 URL、RestTemplate 或 WebClient 绕过它**。
- API 模块中的 facade 负责包装 client 调用、统一异常/响应处理或响应拆箱；业务服务优先依赖 facade，不要在各处重复写远程调用细节。
- **跨服务共用的公共行为（本服务和其他服务都要用的能力，如 @Log 落库）用「一个接口、两个实现」**：①接口定义在**消费域**模块（先例 `base-log` 的 `LogClient`，javadoc 注明接口属于消费者域）；②远程实现由 `lihua-api` 契约层提供（`SysLogClientFacade` implements LogClient，带 @CircuitBreaker）；③表 Owner 服务提供本地直写实现（`LogClientLocalImpl` 落 `loader/` 包，**@Primary** 压过 RPC 版——本服务自调用不走远程，跨服务才走 RPC）；消费方只注入接口、不感知实现。新增此类能力照此三件套落位，勿在消费方写 if-else 判断本地还是远程。
- 远程调用的 DTO/model 放在 API 模块，避免调用方依赖目标服务内部 entity。

## 熔断降级

- 跨服务 facade 方法优先使用 Resilience4j `@CircuitBreaker`，不要把熔断逻辑直接堆在 Controller 中；熔断名称按业务或目标资源稳定命名（如 `sysUser`、`sysSetting`、`sysLog`），并与 Nacos resilience 配置保持可追踪。
- 有明确可降级结果的方法提供 `fallbackMethod`：fallback 参数必须与原方法一致并在末尾追加 `Throwable`；fallback 中记录异常和关键请求参数；同步接口返回统一 `ApiResponseModel` 错误，异步接口按现有模式返回 `Mono.error(...)` 或与原返回类型一致的降级结果。
- 如果只是允许默认异常传播，也要确认调用方能正确处理失败响应，不要吞掉远程调用错误。
- 修改熔断窗口、失败率阈值、OPEN 等待时间、超时时间等策略时，优先维护 Nacos 配置种子 `lihua-resilience/lihua-resilience.yaml` 或目标部署环境配置（各服务经 `nacos:lihua-resilience.yaml?group=lihua-resilience` 引入）。
- Gateway 使用 Reactor Resilience4j 处理网关层降级（路由的 CircuitBreaker filter → FallbackController）；服务间远程调用使用 API facade 上的 Resilience4j 注解——两者职责不同，不要把网关 fallback 当作服务间调用的唯一保护。
- **@Retry 注解与 CB fallbackMethod 组合不可行**（fallback 吞异常外层永不触发），勿再评估。**读 RPC 重试脚手架留白（已拍板由二开自决）**——脚手架不内置重试，勿擅自引入。

## 云端运行态（Nacos / 网关 / 内部 RPC）

- **本地测试环境**：本地 `java -jar` 跑服务（`SERVER_PORT` 显式指定——auth 的 application.yml 默认 8082 与 system 冲突；config 拉 dev 命名空间、discovery 注册 public）+ docker nacos + 宿主 mysql8/redis。nacos v3.1.1 容器 **`NACOS_AUTH_TOKEN`（≥32 字节 base64）与 `NACOS_AUTH_IDENTITY_KEY/VALUE` 三项强制**，缺一或 token 过短即 exit 255/启动失败（`NACOS_AUTH_ENABLE=false` 也不例外）——auth 开启的实例须先 `POST /nacos/v3/auth/user/admin?username=&password=` 无头初始化 admin（nacos ≥2.4 无默认密码，`NACOS_ADMIN_PASSWORD` 官方镜像不受支持）；v3.1.1 console 健康端点路径漂移且恒 5xx，健康探活用主端口根 `GET /nacos/`（匿名 200）。本地调试实例开 `NACOS_AUTH_ENABLE=false` 后可带头 `nacos: nacos` 直调 admin API（8848 端口）：建命名空间 `POST /nacos/v3/admin/core/namespace`（参数 `namespaceId`）、发配置 `POST /nacos/v3/admin/cs/config`（dataId/groupName/namespaceId=dev/type=yaml/content），配置源=仓库 zip 解包逐份发布；nacos 无持久卷，容器删除即丢配置，重建后须重导。
- **停机成功窗口机制**：SIGTERM 后 SCA 先从 nacos 注销、再满血服务 `graceful-shutdown-wait-time`（默认 10s）——这是登录仍成功的窗口主体；调用方 loadbalancer caffeine 缓存 ttl（默认 35s，nacos 推送不主动失效）只决定失败形态（僵尸连接 refused vs No instances）不产生成功；两值已收口 nacos `lihua-common.yaml` 停机段（2000ms/10s）。体面期内建立的 keep-alive 连接排水期后变僵尸，各吃满一次 RPC 响应超时（默认 5s）→ 网关 CB 慢调用降级 501（FallbackController SYSTEM_ERROR），属既有网关行为非缺陷。
- **内部 RPC 验签 = 纯身份门（留章去窗）**：`@InternalOnly` 端点（loginSelect/register/日志 insert 等 permitAll 内部端点的唯一防线）经 `InternalRequestInterceptor` 验 HMAC（盖 method:URI:timestamp，`HmacUtils` 输出 **Base64 非 hex**；缺头/格式/签名不匹配三分支拒 401+留 warn）——**时间戳仅作签名材料使各请求签名互异，不校验时效，勿恢复时间窗**（时间窗已移除：内网无截获延迟重放威胁面、内部端点全只读/幂等，时钟比对反是登录链自我瘫痪源；生态对照若依 Cloud 仅明文头 from-source:inner）。真要防重放用 nonce/去重或签名纳入 body（现签名不盖请求体，属既有弱点）；「容器时钟同步」不再是部署前提。
- **IP 取值**：cloud 侧 `Request-IP` 网关裁决头优先（RequestIpFilter 解析后写 header 透传下游）→ IpResolveUtils 兜底；token/IP 一律经 `WebUtils`/`LoginUserContext` 取，勿自行读头解析（收口细则与 mono 同文）。
- **鉴权双层职责**：网关只做前置（IP 黑名单 + 非法 token 拒绝），**无 token 请求原样放行**；真正的匿名/认证裁决在各服务 SecurityConfig（base-security 共用一份）——permitAll 清单含后台接口组、app 接口组、系统基础组（captcha/actuator/ws-connect/swagger）与 cloud 特有的内部端点组（log insert / user auth / setting，配 @InternalOnly 验签），其余 `anyRequest().authenticated()`。
- **TraceId 链路**：网关 `TraceIdFilter`（Order -110 最先）单点生成 traceId，**覆写外部携带的同名头**（下游读到的必为网关产物，防伪造），随路由注入请求头转发 + 回写响应头供前端排障定位；下游经 `CustomHttpHeader.TRACE_ID` / `TraceIdUtils`（base-common）消费。
- **CORS 双层与 ACAO 去重**：网关 `globalcors` **自答预检 OPTIONS**（无此配置时网关本地终结预检且零 CORS 头，浏览器直连网关必被拦；`allowedOriginPatterns: *`、不开 credentials，水位对齐下游）+ 服务层 base-web `CorsConfig` 答实际请求；**网关与下游会在实际响应上各加一份 ACAO，default-filters 必须去重**（否则浏览器报 multiple values）——调整跨域水位两侧同步对齐，勿只改一层。

## 微服务边界与二开

**边界：脚手架 vs 二开**

- 平台公共层（`lihua-base-*` 各模块、gateway 过滤器与异常装配、base-security 的 SecurityConfig、auth 服务登录装配、nacos 种子结构）是脚手架契约——二开**用而不改**；确需改动 = 影响全部服务 + 脚手架升级时的手工合并成本，动前必评估影响面（公共 jar 配置义务见红线；base 层双仓同步纪律见差异地图）。
- 平台既有端点契约（各服务 URL、DTO/VO 字段、权限标识、种子数据）**只增不改**——被 web/app 双端消费且进入升级路径；新能力以新端点/新字段扩展，勿重构平台接口形态。
- **新业务落位**：在 `lihua-biz` 下新建服务模块（独立 `spring.application.name`，nacos 服务发现自动注册），接入四件套 = ①nacos `lihua-gateway.yaml` 加路由（**具体路径的 route order 必须先于 `/system/**` 通配**，配 CircuitBreaker filter + fallbackUri）②服务 SecurityConfig 引用 base-security 共用配置（免登录端点登记 permitAll 对应组）③被其他服务调用则在 `lihua-api` 建 client/facade 契约 ④配置进 nacos `<服务名>/<服务名>.yaml` 并补种子 zip。勿把二开业务混入 system/file/auth/monitor（平台自身功能）。
- **服务平级互不依赖**（见核心规则 9）：跨服务一切走 `lihua-api` RPC + 熔断；服务内跨模块用领域事件；进程内事件不跨服务。
- **配置归属**：服务配置在 nacos `<服务名>/<服务名>.yaml`（种子同步进仓库 zip），全局横切参数（token/停机等）唯一来源 `lihua-common.yaml`；勿把配置写死代码或散落服务 yml。
- **WS 连接归属独立 lihua-websocket 服务（架构拍板 2026-09-25，命名收口 09-26）**：WS 连接层即顶层第六服务 `lihua-websocket`（无库、可多实例部署，连接层代码与启动引导一体，同 lihua-system 的双仓关系），system 已卸除 WS 连接层依赖；gateway 路由 `/ws-connect/**` 单独指向 lihua-websocket（具体路径先于 `/system/**` 通配）。**网关侧运维注意：WebSocket 升级请求被 CB fallback 拦截时表现为握手失败（非 501 错误体）——ws 服务下线时先摘流量再停实例**。**推送投递统一走 Redis pub/sub**（`RedisTopicEnum.WS_PUSH`）：业务侧（任意服务，引 lihua-base-ws 即可）经 `WebSocketPushUtils.push`（lihua-base-ws `com.lihua.ws.push` 包，**业务投递唯一入口**，业务与 lihua-websocket 的中间层）投递 `{userIdList, type, data}` 最小化消息体（null userIdList=全员广播；载荷客户端拉取，推送是在线即时提示非送达承诺，红点事实源仍是 Redis 标记），所有订阅该 topic 的 WS 实例各收一次、各自推本地连接=多实例天然扇出。**铁纪律：禁止业务侧依赖 lihua-websocket 或直调 WebSocketManager/进程内事件触达**——那是单实例语义，双实例部署即漏推（单实例测试发现不了，多实例才暴露）；「写库 + 推送」一律 `TransactionSendUtils.runAfterCommit` 包裹（发布点包，勿嵌套）。
- **无分布式事务（拍板）**：跨服务写操作按最终一致设计（RPC 即边界，先例 auth 注册链无补偿机制），不引入 Saga/分布式事务框架。
- **主键与分布式 ID 现状**：BaseEntity 未显式配置 `@TableId`、全局未配 `id-type`——走 MyBatis-Plus 默认 **ASSIGN_ID（雪花）**（存量显式声明亦以 ASSIGN_ID 为主），跨服务天然不撞，勿随意改全局 id-type；无统一发号器（业务单号/对外编号类），出现诉求时再立。
- **auth 服务无数据库**（nacos 配置无 datasource、全程无 @Transactional）：认证服务纯 RPC + Redis——需要落表的业务勿放 auth。
- **共库分表纪律**：有库服务（system/file）共用 `lihua` 单库——**物理同库也不得跨服务 JOIN / 直查他服务表**（服务边界靠 RPC 维持，为未来拆库留路）。

**一体感：照先例写代码**

- 落笔前先在仓内找同类先例（最接近的 Controller/Service/Mapper/facade 与前端页面），照其结构写——不引入第二套风格（自造返回包装、自拼分页、绕过 base-client 手写远程调用都是破窗）。
- 横切能力一律用平台现成实现，新增写端点配三件套（`@Tag`/`@Operation` 文档 + `@PreAuthorize` 权限 + `@Log` 操作日志）：统一返回 `ApiResponseController`、全局异常 `ServiceException` → `GlobalExceptionHandle`、参数校验 `@Validated` + 分组、分页 BaseDTO + `MaxPageSizeLimit`、登录态 `LoginUserContext`、字典 `DictEnum` + `DictUtils`、附件 base-attachment 全家（file 服务）、实时通知 `WebSocketPushUtils`（lihua-base-ws，Redis pub/sub 扇出，投递不依赖连接持有方）+ `TransactionSendUtils.runAfterCommit`、防重提交 `@PreventDuplicateSubmit`、排序归一化 `SortUtils`、IP 归属地 `WebUtils`、跨服务 RPC `lihua-api` facade + `@CircuitBreaker`。
- 表命名边界：平台表前缀 `sys_`；二开业务表用业务域自己的前缀，勿冒用 `sys_`。

## 红线与已否决方案

- **跨模块字段/方法「死活」判定**：判 VO/Entity 字段可删前按 **getter/setter 调用形态**全仓 grep（`xx.getUserId()`、`SysPostVO::getUserId`），不能只搜类型名——消费方常以变量名调用，按类型名匹配会漏判并引发跨模块编译错误。删 Mapper 方法前按方法名 grep 双仓库（含测试）。**grep 消费点禁止 `head` 等截断管道**——必须看全量输出并以宽模式二次复核。删 public 方法/字段前先看是否 @Override 框架接口（UserDetails/InitializingBean 等）——接口多态调用静态 grep 不可见。
- **枚举/常量/配置键死活**：grep 必须**「符号名 + 值字符串」双模式**（消费代码常只引用枚举名，值字符串零命中 ≠ 零引用）；「零引用」结论必须以**消费方模块编译（`-am` 全链）**佐证。删除类改动 commit 前双核一遍。
- **依赖死活**：`mvn dependency:analyze` 报的 "Unused declared dependencies" 几乎全是运行时依赖误报，逐类核对勿直接删：①starter 族（spring-boot/spring-cloud-starter-*/redisson/springdoc/snail-job client）——自动装配运行时需要，编译期无 import 是常态；②驱动与自动装配类（mysql-connector-j、mybatis-plus、dynamic-datasource、resilience4j starter）——永不直接 import；③内部聚合模块——由可运行服务聚合打包消费或经传递路径供给。真死依赖画像：非 starter/驱动/聚合的工具库，analyze 报 Unused 且按包名全仓 grep import 零命中才可判死。根 pom 版本属性核查按 `${属性名}` grep 全部模块 pom（双仓库都查）。
- **公共 jar 配置义务边界**：被各服务 `@ComponentScan("com.lihua.**")` 全量注册的 base-* jar，其 fail-fast 校验与无默认值 `@Value` 会把配置义务强加给**所有**引入服务（微服务下消费方更多，义务面更大）——fail-fast 限定在「声明了该域角色的服务」内生效；排查占位符勿只看配置类（jar 内任意 @Component 自带的无默认值 `@Value` 同样被宿主解析）；**Class 型共享配置绑定会加载整个继承链——条目须在全部消费方（含 WebFlux 网关）classpath 可解析且父类无 servlet/security 栈依赖**（NoHandlerFoundException/BadCredentialsException 都曾炸 gateway，逐个删到绑定通过）。对外下发的 URL 一律相对链（部署前缀由各端 base 拼接）；跨服务消费纯函数优先下沉 base-common，勿让消费方依赖域 jar。
- **依赖升级两铁律**：版本核对必须直接查 Central `maven-metadata.xml`（`versions:display-dependency-updates` 不穿透 BOM import）；验证必须 `mvn clean compile/test`（增量编译对依赖版本变更假绿）。
- **已否决勿再提议**：save 接口收窄 DTO；恢复 MODE_INHERITABLETHREADLOCAL；恢复 RPC 时间戳校验窗（见「云端运行态」）；@Retry 注解与 CB fallbackMethod 组合（fallback 吞异常外层永不触发）。
- **跨双仓 Bash 坑**：复合命令 cd 与相对路径组合易发生 cwd 漂移（曾在错误仓库执行 sed/perl/rm）——跨仓操作一律绝对路径或 `mvn -f` 指定 pom，rm/批量改后立即 grep 验证目标仓库。
- **不强制统一项（已拍板，勿再提议立规）**：单元测试、`@Transactional` 事务边界、定时任务（Snail Job）接入、Excel 导入、`ResultCodeEnum` 错误码扩展——均不做脚手架级强制规范，按所在服务既有写法随项目走。

## 双仓库差异地图（lihua-cloud ↔ lihua）

> 本节与 lihua 仓 `skills/lihua-backend.md` 的同名章节同文；**改任一处须同步另一处**。改 base 层/横切逻辑前必读；两仓 base 层改动遵循双同步原则（同 commit 粒度、行为一致）。

- **直接复制即可**（源码级一致，仅包名差异）：base 的 attachment/captcha/doc/job/mybatis/sensitive/ws 全部 + 顶层 `lihua-websocket`（双仓同名同位的受控基建库）；`lihua-system` 绝大多数文件逐字节一致（@Log/@PreAuthorize/@Sensitive 用法同构）。cloud 多一个 `lihua-base-client`（远程调用基建），mono 无。
- **cloud 特有处理（相对 mono 的分叉点）**：
  1. 认证/注册/验证码：mono 在 lihua-system，cloud 拆到独立 lihua-auth 服务且走 RPC（`lihua-api-system` 的 SysUserAuthClient + facade）；AuthenticationManager Bean 位置不同（mono 在 SecurityConfig，cloud 在 auth 服务 LoginConfig）。
  2. 附件：cloud 归 lihua-file 服务，配置在 nacos `lihua-file.yaml`（上传模式/100MB 限制/下载链接过期）。
  3. 操作日志落库：mono 用 ApplicationEventPublisher 发事件（事件模型 LogEvent，`common/model/event/log`），cloud 走 `LogClient` 双实现——本服务 `LogClientLocalImpl`（@Primary 本地直写）、跨服务 `SysLogClientFacade` RPC（base-log 依赖 lihua-api-system）。
  4. IP 黑名单：mono 是 Servlet 拦截器（RequestIpInterceptor + Ip2region），cloud 上移到 gateway 过滤器并把 IP 写入 `Request-IP` header 透传下游。
  5. SecurityConfig：cloud 多 3 条内部端点 permitAll（log insert / user auth / setting）。
  6. TokenEnum 位置：cloud 在 base-common，mono 在 base-security（值相同，硬编码 JWT 密钥）。
- **配置双轨**：mono 用 application-dev/prod.yml，cloud 用 nacos（仓库导出 `deploy/nacos/nacos_config_export.zip`，目录=group）；token 参数唯一来源是 nacos `lihua-common.yaml`。
- **WS 部署形态**：mono 单 jar WS 嵌 admin 进程（顶层 `lihua-websocket` 纯库由 lihua-admin 显式引入，业务模块零依赖；推送投递经 Redis pub/sub 回本进程订阅器推送，mono 部署多份天然扇出）/ cloud 独立 `lihua-websocket` 服务（连接层即服务本体，无库、可多实例；gateway 路由 `/ws-connect/**` 指向它；system 已卸除 WS 连接层依赖）。两形态同构：连接层与业务模块完全隔离、仅经 Redis 交互（WS_PUSH 投递/订阅 + once token 握手鉴权读写）；业务侧投递一律 `WebSocketPushUtils.push`（lihua-base-ws），禁止依赖连接层/直调 WebSocketManager。
- **范式约定**：分页 `POST /page` + `@Validated(MaxPageSizeLimit.class)` + BaseDTO（pageNum/pageSize 上限 999999/100）；权限维持 `hasRole('ROLE_admin')` 粗粒度（细粒度 authorities 通道保留但不消费，项目定位类若依脚手架）。
- **同步纪律**：base 层/公共契约改动必须双仓成对（同 commit 粒度），改任一侧先检查另一侧对应文件；业务枚举双仓同名同值（mono `com.lihua.enums` / cloud `com.lihua.system.enums`）。

## 部署

- Dockerfile 基础镜像必须与根 pom 编译主版本匹配（Java 25 = class major 69，低版 JRE 容器 `java -jar` 即 `UnsupportedClassVersionError`）；镜像选 **Ubuntu 系变体（noble/jammy），禁用 alpine**——精简镜像无 fontconfig/系统字体，验证码字体加载报错；temurin Ubuntu 系自带 fontconfig + fonts-dejavu。tag 用精确 pin（如 `eclipse-temurin:25.0.4_7-jre-noble`）；README 镜像口径随 dockerfile 同步。
- 换/升镜像容器内实测三件套：① `java -version` 对主版本；② `fc-list`/`dpkg -l` 查 fontconfig 与 dejavu 字体在位；③ 挂 captcha-font 真实 TTF 跑 `Font.createFont` + `drawString` 中文渲染探针（非空白像素 >1000 且 `canDisplay('狸')` 为真才判通）。
- compose 六服务依赖 nacos；nacos 容器必须带合规 auth 三件套 env（见「云端运行态」），重建后重导 admin 与配置。

## 验证

- 定向优先：`mvn -pl <module> -am test`（如 `mvn -pl lihua-biz/lihua-system -am test`）；改动 `lihua-api`、`lihua-base`、父 POM 或跨服务契约时，优先运行覆盖调用方和被调用方的检查；广义 `mvn test`。
- MySQL/Redis/Nacos/对象存储不可用时，仍应运行不依赖这些服务的编译/测试检查，并说明剩余验证缺口。
- 跨双仓 Bash 操作一律绝对路径或 `mvn -f` 指定 pom，rm/批量改后立即 grep 验证目标仓库。
