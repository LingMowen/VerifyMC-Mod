# VerifyMC Mod

[![Minecraft Version](https://img.shields.io/badge/Minecraft-1.21.1-green.svg)](https://minecraft.net)
[![NeoForge Version](https://img.shields.io/badge/NeoForge-21.1.1-blue.svg)](https://neoforged.net)
[![Version](https://img.shields.io/badge/Version-1.7.2-brightgreen.svg)](../../releases)
[![Java Version](https://img.shields.io/badge/Java-21-orange.svg)](https://java.com)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

> 一个功能强大的 Minecraft 白名单管理系统，基于 Web 界面，支持多种验证方式。

这是 [VerifyMC](https://github.com/KiteMC/VerifyMC) 项目的 NeoForge 移植版本，适配 Minecraft 1.21.1 和 NeoForge 21.1.1。

## ✨ 功能特性

### 🔐 用户系统
- **用户注册** - 支持邮箱验证和验证码
- **用户登录** - 安全的密码验证机制
- **个人资料管理** - 编辑邮箱、修改密码
- **状态查看** - 实时查看审核状态

### 👮 管理员功能
- **审核管理** - 通过/拒绝用户申请
- **用户管理** - 查看所有用户、搜索、分页
- **审计日志** - 记录所有审核操作
- **服务器状态** - 实时监控服务器状态

### 🔗 验证方式
- **邮箱验证** - 支持 SMTP 邮件发送
- **Discord OAuth** - Discord 账号绑定
- **问卷系统** - 可配置的问卷和自动评分
- **LLM 智能评分** - 支持 DeepSeek 等 AI 模型自动评估问卷答案

### 📦 其他功能
- **下载中心** - 可配置客户端整合包、资源包下载
- **WebSocket 实时通知** - 申请状态实时推送
- **多语言支持** - 中文/英文切换
- **响应式设计** - 支持移动端访问
- **版本检查** - 自动检查模组更新

### 🔐 安全特性
- **密码加密存储** - 使用 BCrypt 加密用户密码
- **验证码机制** - 支持邮箱验证码和数学验证码
- **操作审计日志** - 记录所有管理操作
- **邮箱域名白名单** - 限制可注册的邮箱域名

## 📋 适配状态

| 加载器 | 状态 | Minecraft 版本 | NeoForge 版本 |
|--------|------|----------------|---------------|
| NeoForge | ✅ 已适配 | 1.21.1 | 21.1.1 |
| Fabric | ⏳ 计划中 | - | - |
| Forge | ⏳ 计划中 | - | - |

## 🚀 快速开始

### 环境要求
- Java 21 或更高版本
- Minecraft 1.21.1
- NeoForge 21.1.1

### 安装步骤

1. **下载模组文件**
   - 从 [Releases](../../releases) 下载最新版本的 `verifymc-1.7.2.jar`

2. **安装到服务器**
   ```bash
   # 将 JAR 文件放入服务器的 mods 文件夹
   cp verifymc-1.7.2.jar /path/to/server/mods/
   ```

3. **启动服务器**
   - 首次启动会自动生成配置文件

4. **访问 Web 界面**
   - 打开浏览器访问 `http://服务器IP:8080`
   - 默认管理员账号：第一个 OP 用户名 / `{OP用户名}123`
   - 例如：如果 OP 用户名为 `admin`，则密码为 `admin123`

### 配置说明

配置文件位于 `config/verifymc-common.toml`：

```toml
[web]
    # Web 服务器端口
    port = 8080
    
    # WebSocket 端口（用于实时通知）
    websocketPort = 8081

[database]
    # 数据库类型: file 或 mysql
    type = "file"

[email]
    # 邮件服务器配置
    enabled = false
    host = "smtp.example.com"
    port = 587
    username = "your-email@example.com"
    password = "your-password"
    
    # 邮箱域名白名单
    enableDomainWhitelist = true
    domainWhitelist = ["gmail.com", "qq.com", "163.com", "outlook.com"]

[discord]
    # Discord OAuth 配置
    enabled = false
    clientId = ""
    clientSecret = ""
    redirectUri = "http://localhost:8080/api/auth/discord/callback"
    guildId = ""  # Discord 服务器 ID（可选）

[questionnaire]
    # 问卷系统配置
    enabled = false
    passingScore = 60
    
[llm]
    # LLM 智能评分配置
    enabled = false
    provider = "deepseek"  # 支持: deepseek, openai
    apiBase = "https://api.deepseek.com/v1"
    apiKey = ""
    model = "deepseek-chat"
    timeout = 10000
    maxConcurrency = 4

downloads]
    # 下载资源配置（JSON 格式）
    resourcesJson = '[]'
    # 示例配置：
    # resourcesJson = '[{"id":"modpack","name":"客户端整合包","description":"服务器必需模组","version":"1.0.0","size":"256 MB","url":"https://example.com/modpack.zip","icon":"gamepad"}]'
```

## 🛠️ 构建项目

### 前置要求
- Java 21
- Node.js 18+

### 构建步骤

```bash
# 1. 克隆仓库
git clone https://github.com/LingMowen/VerifyMC-Mod.git
cd VerifyMC-Mod

# 2. 构建前端
cd src/main/resources/assets/verifymc/web
npm install
npm run build
cd ../../../../../../..

# 3. 构建模组
./gradlew build
```bash
# 4. 获取构建产物
# 构建后的 JAR 文件位于 build/libs/verifymc-1.7.2.jar
```

## 📁 项目结构

```
MDK-1.21.1-ModDevGradle/
├── src/main/java/com/verifymc/     # Java 后端源码
│   ├── VerifyMC.java               # 主类
│   ├── web/WebServer.java          # Web 服务器
│   ├── whitelist/                  # 白名单管理
│   ├── auth/                       # 认证相关
│   ├── db/                         # 数据库访问
│   └── service/                    # 业务服务
├── src/main/resources/
│   ├── assets/verifymc/
│   │   ├── web/                    # Vue 前端源码
│   │   ├── www/                    # 编译后的前端
│   │   ├── email/                  # 邮件模板
│   │   └── i18n/                   # 国际化文件
│   └── questionnaire.json          # 问卷配置
└── build.gradle                    # Gradle 构建配置
```

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

1. Fork 本仓库
2. 创建你的特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交你的更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开一个 Pull Request

## 📜 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件

## 🙏 致谢

- 原项目 [VerifyMC](https://github.com/KiteMC/VerifyMC) 提供的优秀设计
- [NeoForge](https://neoforged.net/) 团队提供的模组加载器
- [Vue.js](https://vuejs.org/) 团队提供的前端框架

## 📞 联系方式

- GitHub Issues: [提交问题](../../issues)
- 原项目地址: https://github.com/KiteMC/VerifyMC

## 📝 更新日志

### v1.7.2 (2025-01-XX)

#### ✨ 新功能
- **LLM 智能评分** - 集成 DeepSeek 等 AI 模型，自动评估问卷答案
- **下载中心** - 支持配置客户端整合包、资源包等下载资源
- **WebSocket 实时通知** - 审核状态实时推送到前端
- **邮箱域名白名单** - 限制可注册的邮箱域名
- **Discord Guild 验证** - 支持强制加入 Discord 服务器

#### 🔧 功能增强
- **默认管理员密码** - 改为动态生成（OP用户名 + 123）
- **配置系统** - 添加更多配置选项（LLM、下载资源、Bedrock支持等）
- **前端优化** - 修复侧边栏层级问题，改进 UI 显示

#### 🐛 修复
- 修复底栏遮挡侧边栏的问题
- 修复对话框 z-index 层级问题
- 优化移动端显示效果

### v1.7.1 (2024-XX-XX)
- 初始 NeoForge 移植版本
- 基础白名单功能完整
- Web 管理界面
- 邮箱验证系统

---

**注意**: 本项目是原 VerifyMC 的移植版本，主要适配 NeoForge 加载器。目前仅适配了 NeoForge 1.21.1 版本，其他加载器版本正在计划中。
