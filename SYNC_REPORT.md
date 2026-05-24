# VerifyMC 移植版本同步报告

## 版本信息
- **原项目版本**: 1.7.2 (VerifyMC-master1.7.2)
- **移植版本**: 1.7.2 (MDK-1.21.1-ModDevGradle)
- **目标平台**: NeoForge 1.21.1

## 架构差异

### 原项目 (Bukkit/Paper)
- 使用 Bukkit API
- 支持 Paper/Folia 调度器
- 使用 AuthMe 插件集成
- 支持 BungeeCord/Velocity 代理

### 移植版本 (NeoForge)
- 使用 NeoForge API
- 使用 Minecraft 原生事件系统
- 需要重新实现部分功能

## 功能对比

### ✅ 已实现功能

#### 核心功能
- [x] Web 服务器 (HTTP)
- [x] WebSocket 服务器 (实时通知)
- [x] 用户注册和登录
- [x] 白名单管理
- [x] 管理员审核系统
- [x] 邮箱验证码 (5分钟有效期)
- [x] 验证码服务 (图形验证码)
- [x] Discord OAuth 集成
- [x] 问卷系统
- [x] LLM 评分 (DeepSeek/OpenAI)
- [x] 多语言支持 (i18n)
- [x] 数据库支持 (File/MySQL)
- [x] 审计日志
- [x] 版本检查
- [x] Metrics (bStats)

#### 配置选项
- [x] 用户名大小写敏感配置
- [x] 邮箱域名白名单
- [x] 邮箱别名限制
- [x] 问卷通过分数
- [x] LLM 配置 (熔断器、并发控制)
- [x] 验证码有效期 (5分钟)

### ⚠️ 需要调整的功能

#### 1. AuthMe 集成
**原项目**: 直接集成 AuthMe 插件，支持密码同步
**移植版本**: NeoForge 没有 AuthMe 插件，需要替代方案
**建议**: 
- 方案1: 使用 NeoForge 的认证系统
- 方案2: 自建密码管理系统
- 方案3: 跳过 AuthMe 功能，仅使用原版白名单

#### 2. 调度器 (Scheduler)
**原项目**: 使用 Bukkit/Paper/Folia 调度器
**移植版本**: 使用 Minecraft 原生 tick 事件
**状态**: 已实现替代方案

#### 3. 代理支持 (Proxy)
**原项目**: 支持 BungeeCord/Velocity 代理插件
**移植版本**: 暂不支持
**建议**: 未来可考虑添加 Velocity 支持

#### 4. Bedrock 支持
**原项目**: 支持 Geyser/Floodgate (基岩版)
**移植版本**: 需要测试 NeoForge 下的兼容性

### ❌ 暂无法实现的功能

#### 1. Bukkit 命令系统
**原因**: NeoForge 使用不同的命令 API
**替代**: 已使用 NeoForge 命令 API 重新实现

#### 2. Bukkit 事件系统
**原因**: NeoForge 使用不同的事件系统
**替代**: 已使用 NeoForge 事件系统重新实现

## 文件结构对比

### 原项目特有文件
```
plugin/src/main/java/team/kitemc/verifymc/
├── command/VmcCommandExecutor.java      # Bukkit 命令执行器
├── core/PluginScheduler.java            # Bukkit 调度器包装
├── registration/                        # 注册流程相关
│   ├── RegistrationOutcome.java
│   ├── RegistrationOutcomeMessageKeyMapper.java
│   └── RegistrationOutcomeResolver.java
├── service/AuthmeService.java           # AuthMe 集成
├── util/PluginScheduler.java            # 调度器工具
└── web/handler/                         # HTTP 处理器 (更细粒度)
    ├── AdminAuditHandler.java
    ├── AdminAuthUtil.java
    ├── AdminSyncHandler.java
    ├── AdminUserHandler.java
    ├── AdminVerifyHandler.java
    ├── CaptchaHandler.java
    ├── ConfigHandler.java
    ├── DiscordHandler.java
    ├── DownloadsHandler.java
    ├── LoginHandler.java
    ├── QuestionnaireConfigHandler.java
    ├── QuestionnaireSubmitHandler.java
    ├── ReviewStatusHandler.java
    ├── ServerStatusHandler.java
    ├── StaticFileHandler.java
    ├── UserProfileHandler.java
    ├── VerifyCodeHandler.java
    └── VersionHandler.java
```

### 移植版本当前结构
```
src/main/java/com/verifymc/
├── auth/AuthManager.java
├── auth/PasswordUtil.java
├── command/VmcCommand.java              # NeoForge 命令
├── config/VerifyMCConfig.java
├── core/ConfigManager.java
├── core/I18nManager.java
├── core/OpsManager.java
├── core/PluginContext.java
├── core/ResourceManager.java
├── db/                                  # 数据访问层
├── listener/PlayerLoginListener.java    # NeoForge 事件监听
├── mail/MailService.java
├── service/                             # 业务服务
├── web/WebServer.java                   # 集成的 Web 服务器
├── web/NotificationWebSocketServer.java
└── whitelist/WhitelistManager.java
```

## 需要同步的更新

### 1. 移除模拟数据 (最新提交)
**文件**:
- `frontend/glassx/src/components/dashboard/DownloadCenter.vue`
- `frontend/glassx/src/components/dashboard/ServerStatus.vue`
- `plugin/src/main/resources/config.yml`

### 2. 前端验证码流程
**状态**: 已同步

### 3. 用户名大小写敏感
**状态**: 已同步

## 建议的后续工作

### 高优先级
1. 完善 AuthMe 替代方案
2. 测试所有 Web API 端点
3. 验证数据库迁移功能

### 中优先级
1. 添加更多配置选项
2. 完善错误处理
3. 优化性能

### 低优先级
1. 添加 Velocity 代理支持
2. 完善 Bedrock 支持
3. 添加更多主题

## 配置差异

### 原项目 config.yml 特有选项
```yaml
auth_methods: [email]                    # 认证方式列表
max_accounts_per_email: 2                # 每个邮箱最大账号数
whitelist_mode: plugin                   # 白名单模式
web_register_url: https://domain.com/    # 注册页面 URL
register:
  auto_approve: false                    # 自动审核
username_regex: "^[a-zA-Z0-9_-]{3,16}$"  # 用户名正则
username_case_sensitive: false           # 大小写敏感
user_notification:                       # 用户通知
  enabled: true
  on_approve: true
  on_reject: true
frontend:                                # 前端配置
  theme: glassx
  logo_url: /logo.png
  announcement: Welcome!
smtp:                                    # SMTP 配置
  ...
email_subject: VerifyMC Verification Code
auto_update_resources: true
enable_email_domain_whitelist: true
enable_email_alias_limit: false
email_domain_whitelist: [...]
storage: file                           # 存储类型
mysql: {...}
authme: {...}                           # AuthMe 配置
captcha: {...}
bedrock: {...}
questionnaire: {...}
llm: {...}
discord: {...}
downloads: {...}
```

### 移植版本当前配置
```yaml
[NeoForge Config Format]
web.port = 8080
web.host = "0.0.0.0"
language = "en"
debug = false
smtp.host = "smtp.qq.com"
smtp.port = 587
...
```

## 结论

移植版本已经实现了原项目 1.7.2 的大部分核心功能。主要差异在于：

1. **平台差异**: Bukkit vs NeoForge API
2. **AuthMe 集成**: 需要寻找替代方案
3. **配置格式**: YAML vs NeoForge Config

建议继续完善配置系统和测试所有功能。
