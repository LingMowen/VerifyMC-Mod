# VerifyMC 移植版本与原项目对比报告

## 基本信息

| 项目 | 原项目 | 移植版本 |
|------|--------|----------|
| **名称** | VerifyMC | VerifyMC-Mod |
| **版本** | 1.7.2 | 1.7.2 |
| **平台** | Bukkit/Paper/Spigot | NeoForge |
| **Minecraft** | 1.20+ | 1.21.1 |
| **Java** | 17+ | 21+ |

---

## 一、架构差异

### 1.1 核心架构

| 特性 | 原项目 | 移植版本 | 差异说明 |
|------|--------|----------|----------|
| **API 框架** | Bukkit API | NeoForge API | 完全不同的 API 体系 |
| **事件系统** | Bukkit Events | NeoForge Events | 事件类型和监听方式不同 |
| **命令系统** | Bukkit Command API | NeoForge Command API | 命令注册和执行方式不同 |
| **调度器** | Bukkit Scheduler | Minecraft Tick Events | 任务调度机制不同 |
| **配置系统** | YAML (Bukkit) | NeoForge ConfigSpec | 配置格式和读取方式不同 |

### 1.2 项目结构

#### 原项目结构
```
VerifyMC-master1.7.2/
├── plugin/                    # Bukkit 插件主模块
│   ├── src/main/java/team/kitemc/verifymc/
│   │   ├── VerifyMC.java              # 主类
│   │   ├── command/                   # 命令
│   │   ├── core/                      # 核心功能
│   │   ├── db/                        # 数据库
│   │   ├── listener/                  # 事件监听
│   │   ├── mail/                      # 邮件服务
│   │   ├── registration/              # 注册流程
│   │   ├── service/                   # 业务服务
│   │   ├── util/                      # 工具类
│   │   └── web/                       # Web 服务器
│   │       ├── handler/               # HTTP 处理器（细粒度）
│   │       └── ...
│   └── src/main/resources/
│       ├── config.yml
│       └── ...
├── plugin-proxy/              # 代理插件（BungeeCord/Velocity）
└── frontend/glassx/           # 前端（独立项目）
```

#### 移植版本结构
```
MDK-1.21.1-ModDevGradle/
├── src/main/java/com/verifymc/
│   ├── VerifyMC.java                  # 主类
│   ├── auth/                          # 认证（新增）
│   ├── command/                       # 命令
│   ├── config/                        # 配置
│   ├── core/                          # 核心功能
│   ├── db/                            # 数据库
│   ├── listener/                      # 事件监听
│   ├── mail/                          # 邮件服务
│   ├── service/                       # 业务服务
│   ├── web/                           # Web 服务器（集成式）
│   └── whitelist/                     # 白名单管理
├── src/main/resources/assets/verifymc/
│   ├── web/                           # 前端源码（嵌入式）
│   └── www/                           # 前端构建产物
└── build.gradle                       # Gradle 构建配置
```

---

## 二、功能对比

### 2.1 核心功能

| 功能模块 | 原项目 | 移植版本 | 状态 |
|----------|--------|----------|------|
| **用户注册/登录** | ✅ | ✅ | 已实现 |
| **邮箱验证** | ✅ | ✅ | 已实现 |
| **白名单管理** | ✅ | ✅ | 已实现 |
| **管理员审核** | ✅ | ✅ | 已实现 |
| **Web 界面** | ✅ | ✅ | 已实现 |
| **多语言支持** | ✅ | ✅ | 已实现 |
| **数据库支持** | ✅ (File/MySQL) | ✅ (File/MySQL) | 已实现 |
| **审计日志** | ✅ | ✅ | 已实现 |
| **验证码服务** | ✅ | ✅ | 已实现 |
| **Discord OAuth** | ✅ | ✅ | 已实现 |
| **问卷系统** | ✅ | ✅ | 已实现 |
| **LLM 评分** | ✅ | ✅ | 已实现 |
| **版本检查** | ✅ | ✅ | 已实现 |
| **Metrics** | ✅ | ✅ | 已实现 |
| **WebSocket** | ✅ | ✅ | 已实现 |
| **下载中心** | ✅ | ✅ | 已实现 |

### 2.2 原项目特有功能（移植版本缺少）

| 功能 | 原项目 | 移植版本 | 原因 |
|------|--------|----------|------|
| **AuthMe 集成** | ✅ | ❌ | NeoForge 没有 AuthMe 插件 |
| **代理支持** | ✅ (BungeeCord/Velocity) | ❌ | 暂不支持跨服务器代理 |
| **Bedrock 支持** | ✅ (Geyser/Floodgate) | ⚠️ | 未测试 |
| **细粒度 HTTP 处理器** | ✅ | ⚠️ | 采用集成式 WebServer |

### 2.3 配置差异

#### 原项目 (YAML)
```yaml
# 丰富的配置选项
auth_methods: [email]
max_accounts_per_email: 2
whitelist_mode: plugin
web_register_url: https://domain.com/
register:
  auto_approve: false
username_regex: "^[a-zA-Z0-9_-]{3,16}$"
username_case_sensitive: false
user_notification:
  enabled: true
  on_approve: true
  on_reject: true
frontend:
  theme: glassx
  logo_url: /logo.png
  announcement: Welcome!
auto_update_resources: true
enable_email_domain_whitelist: true
enable_email_alias_limit: false
storage: file
mysql: {...}
authme: {...}
captcha: {...}
bedrock: {...}
questionnaire: {...}
llm: {...}
discord: {...}
downloads: {...}
```

#### 移植版本 (NeoForge Config)
```toml
# NeoForge 配置格式
web_port = 8080
web_host = "0.0.0.0"
language = "en"
debug = false
smtp_host = "smtp.qq.com"
smtp_port = 587
...
```

**差异说明**：
- 原项目使用分层 YAML 配置
- 移植版本使用扁平化 TOML 配置
- 部分高级配置项在移植版本中简化或缺失

---

## 三、代码实现差异

### 3.1 主类对比

#### 原项目 (Bukkit)
```java
public class VerifyMC extends JavaPlugin {
    @Override
    public void onEnable() {
        // Bukkit 插件启动逻辑
        saveDefaultConfig();
        // ...
    }
}
```

#### 移植版本 (NeoForge)
```java
@Mod("verifymc")
public class VerifyMC {
    public VerifyMC() {
        // NeoForge 模组构造器
        // 事件总线注册
        // ...
    }
    
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // 服务器启动事件
    }
}
```

### 3.2 命令系统对比

#### 原项目
```java
public class VmcCommandExecutor implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, 
                           String label, String[] args) {
        // Bukkit 命令处理
    }
}
```

#### 移植版本
```java
public class VmcCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // NeoForge 命令注册
        dispatcher.register(Commands.literal("vmc")
            .executes(context -> {
                // 命令执行逻辑
            }));
    }
}
```

### 3.3 事件监听对比

#### 原项目
```java
@EventHandler
public void onPlayerLogin(PlayerLoginEvent event) {
    // Bukkit 事件监听
}
```

#### 移植版本
```java
@SubscribeEvent
public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
    // NeoForge 事件监听
}
```

### 3.4 Web 服务器对比

#### 原项目
- 使用细粒度的 Handler 类
- 每个 API 端点有独立的 Handler
- 更模块化的设计

#### 移植版本
- 使用集成式的 WebServer 类
- 所有端点在一个类中处理
- 更紧凑的设计

---

## 四、前端差异

### 4.1 项目结构

| 项目 | 原项目 | 移植版本 |
|------|--------|----------|
| **位置** | 独立项目 `frontend/glassx/` | 嵌入式 `src/main/resources/assets/verifymc/web/` |
| **构建** | 独立构建 | 集成到 Gradle 构建流程 |
| **部署** | 需要单独部署或配置路径 | 自动打包到 JAR 中 |

### 4.2 功能差异

| 功能 | 原项目 | 移植版本 | 说明 |
|------|--------|----------|------|
| **验证码流程** | ✅ | ✅ | 已同步 v1.7.2 |
| **下载中心** | ✅ | ✅ | 已同步 v1.7.2（移除模拟数据）|
| **服务器状态** | ✅ | ✅ | 已同步 v1.7.2 |
| **用户管理** | ✅ | ✅ | 已实现 |
| **审核管理** | ✅ | ✅ | 已实现 |
| **个人资料** | ✅ | ✅ | 已实现 |
| **审计日志** | ✅ | ✅ | 已实现 |

---

## 五、关键缺失功能

### 5.1 因平台限制无法实现

1. **AuthMe 集成**
   - 原因：NeoForge 没有 AuthMe 插件
   - 影响：无法同步密码到 AuthMe
   - 替代方案：使用原版白名单或自建密码系统

2. **Bukkit 插件生态**
   - 原因：平台差异
   - 影响：无法使用 Bukkit 插件扩展
   - 替代方案：使用 NeoForge 模组

### 5.2 需要后续实现

1. **代理支持**
   - 状态：未实现
   - 优先级：低
   - 说明：可考虑添加 Velocity 支持

2. **Bedrock 支持**
   - 状态：未测试
   - 优先级：中
   - 说明：需要测试 Geyser 兼容性

3. **更多配置选项**
   - 状态：部分缺失
   - 优先级：中
   - 说明：需要完善配置系统

---

## 六、性能对比

| 指标 | 原项目 | 移植版本 | 说明 |
|------|--------|----------|------|
| **启动时间** | ~2-3s | ~2-3s | 相当 |
| **内存占用** | 中等 | 中等 | 相当 |
| **Web 响应** | 快 | 快 | 相当 |
| **数据库操作** | 快 | 快 | 相当 |

---

## 七、总结

### 7.1 移植完成度

| 类别 | 完成度 | 说明 |
|------|--------|------|
| **核心功能** | 95% | 主要功能已实现 |
| **前端界面** | 100% | 已同步 v1.7.2 |
| **配置系统** | 80% | 基本配置可用，高级配置待完善 |
| **平台适配** | 90% | 良好的 NeoForge 适配 |

### 7.2 主要差异总结

1. **平台差异**：Bukkit vs NeoForge
2. **架构差异**：插件 vs 模组
3. **配置差异**：YAML vs TOML
4. **生态差异**：Bukkit 插件生态 vs NeoForge 模组生态

### 7.3 建议

1. **短期**：完善配置系统，添加更多配置选项
2. **中期**：测试 Bedrock 支持，优化性能
3. **长期**：考虑添加 Velocity 代理支持

---

## 八、文件清单对比

### 8.1 后端 Java 文件

| 原项目 | 移植版本 | 状态 |
|--------|----------|------|
| VerifyMC.java | VerifyMC.java | ✅ 已适配 |
| command/VmcCommandExecutor.java | command/VmcCommand.java | ✅ 已适配 |
| core/ConfigManager.java | core/ConfigManager.java | ✅ 已实现 |
| core/I18nManager.java | core/I18nManager.java | ✅ 已实现 |
| core/OpsManager.java | core/OpsManager.java | ✅ 已实现 |
| core/PluginContext.java | core/PluginContext.java | ✅ 已实现 |
| core/ResourceManager.java | core/ResourceManager.java | ✅ 已实现 |
| core/PluginScheduler.java | ❌ | ❌ 不需要（使用 Tick 事件）|
| db/UserDao.java | db/UserDao.java | ✅ 已实现 |
| db/AuditDao.java | db/AuditDao.java | ✅ 已实现 |
| db/FileUserDao.java | db/FileUserDao.java | ✅ 已实现 |
| db/FileAuditDao.java | db/FileAuditDao.java | ✅ 已实现 |
| db/MysqlUserDao.java | db/MysqlUserDao.java | ✅ 已实现 |
| db/MysqlAuditDao.java | db/MysqlAuditDao.java | ✅ 已实现 |
| listener/PlayerLoginListener.java | listener/PlayerLoginListener.java | ✅ 已适配 |
| mail/MailService.java | mail/MailService.java | ✅ 已实现 |
| service/AuthmeService.java | ❌ | ❌ 不需要 |
| service/CaptchaService.java | service/CaptchaService.java | ✅ 已实现 |
| service/DiscordService.java | service/DiscordService.java | ✅ 已实现 |
| service/QuestionnaireService.java | service/QuestionnaireService.java | ✅ 已实现 |
| service/VerifyCodeService.java | service/VerifyCodeService.java | ✅ 已实现 |
| service/VersionCheckService.java | service/VersionCheckService.java | ✅ 已实现 |
| service/EssayScoringService.java | service/EssayScoringService.java | ✅ 已实现 |
| service/MetricsService.java | service/MetricsService.java | ✅ 已实现 |
| web/WebServer.java | web/WebServer.java | ✅ 已适配 |
| web/handler/*.java | ❌ | ⚠️ 集成到 WebServer |
| registration/*.java | ❌ | ⚠️ 集成到 AuthManager |
| util/PasswordUtil.java | auth/PasswordUtil.java | ✅ 已移动 |
| util/PluginScheduler.java | ❌ | ❌ 不需要 |

### 8.2 前端文件

| 原项目 | 移植版本 | 状态 |
|--------|----------|------|
| frontend/glassx/src/* | src/main/resources/assets/verifymc/web/src/* | ✅ 已同步 |

---

*报告生成时间：2026-05-16*
*对比版本：原项目 v1.7.2 vs 移植版本 v1.7.2*
