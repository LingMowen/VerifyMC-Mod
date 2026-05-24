# VerifyMC - Minecraft 白名单验证系统

## 项目简介

本项目是 [VerifyMC](https://github.com/KiteMC/VerifyMC) 的移植版本，主要目标是将原项目适配到不同的 Minecraft 模组加载器，使其能够在更多的 Minecraft 版本和加载器环境下运行。

## 当前适配状态

| 项目 | 状态 | 版本 |
|------|------|------|
| 加载器 | ✅ 已适配 | NeoForge |
| Minecraft 版本 | ✅ 已适配 | 1.21.1 |
| NeoForge 版本 | ✅ 已适配 | 21.1.1 |

## 移植进度

- [x] NeoForge 1.21.1 适配
- [ ] Fabric 适配（计划中）
- [ ] Forge 适配（计划中）
- [ ] 其他版本适配（计划中）

## 功能特性

VerifyMC 是一个完整的 Minecraft 服务器白名单管理系统，包含以下功能：

### 核心功能
- ✅ **Web 白名单申请系统** - 玩家可以通过网页提交白名单申请
- ✅ **管理员审核系统** - 管理员可以在网页后台审核申请
- ✅ **多种验证方式** - 支持邮箱验证、Discord 验证
- ✅ **问卷系统** - 可配置申请问卷，自动评分
- ✅ **多语言支持** - 支持中文和英文

### 安全特性
- ✅ **密码加密存储** - 使用 BCrypt 加密用户密码
- ✅ **验证码机制** - 支持邮箱验证码
- ✅ **操作审计日志** - 记录所有管理操作
- ✅ **管理员权限控制** - 区分管理员和普通用户

### 技术特性
- ✅ **内嵌 Web 服务器** - 无需额外部署 Web 服务
- ✅ **WebSocket 实时通知** - 申请状态实时推送
- ✅ **响应式前端界面** - 基于 Vue 3 + Tailwind CSS
- ✅ **多种数据库支持** - 支持 SQLite 和 MySQL

## 技术栈

### 后端
- Java 21
- NeoForge 21.1.1
- Gson - JSON 处理
- Java-WebSocket - WebSocket 服务
- JavaMail - 邮件服务

### 前端
- Vue 3
- TypeScript
- Tailwind CSS
- Vite

## 项目结构

```
MDK-1.21.1-ModDevGradle/
├── src/main/java/com/verifymc/    # Java 源代码
│   ├── VerifyMC.java              # 主类
│   ├── auth/                      # 认证相关
│   ├── config/                    # 配置管理
│   ├── db/                        # 数据库操作
│   ├── listener/                  # 事件监听
│   ├── service/                   # 业务服务
│   ├── web/                       # Web 服务器
│   └── whitelist/                 # 白名单管理
├── src/main/resources/            # 资源文件
│   ├── assets/verifymc/           # 模组资源
│   │   ├── web/                   # 前端源码
│   │   ├── www/                   # 编译后的前端
│   │   ├── email/                 # 邮件模板
│   │   └── i18n/                  # 国际化文件
│   └── META-INF/                  # 模组元数据
├── build/libs/                    # 构建输出
└── run/                           # 运行目录
```

## 构建说明

### 环境要求
- JDK 21 或更高版本
- Gradle 8.5+

### 构建命令

```bash
# 构建模组
./gradlew build

# 运行客户端（开发测试）
./gradlew runClient

# 运行服务端（开发测试）
./gradlew runServer

# 清理构建
./gradlew clean
```

### 构建输出

构建完成后，模组文件位于：
```
build/libs/verifymc-<version>.jar
```

## 安装说明

1. **安装 NeoForge**
   - 下载并安装 NeoForge 21.1.1 for Minecraft 1.21.1

2. **安装模组**
   - 将 `verifymc-1.7.1.jar` 放入服务器的 `mods/` 文件夹
   - （可选）将 `libs/` 目录下的依赖 jar 放入 `mods/` 文件夹（如果需要邮件功能）

3. **启动服务器**
   - 首次启动会生成配置文件到 `config/verifymc-common.toml`

4. **配置模组**
   - 编辑 `config/verifymc-common.toml` 进行配置
   - 默认管理员账号：第一个 OP 用户名 / `{OP用户名}123`
   - 例如：如果 OP 用户名为 `admin`，则密码为 `admin123`

## 配置说明

主要配置项：

```toml
# Web 服务器端口
webPort = 8080

# 数据库配置
databaseType = "sqlite"  # 或 "mysql"

# 邮件服务配置
[smtp]
enabled = false
host = "smtp.example.com"
port = 587

# Discord 验证配置
[discord]
enabled = false
clientId = ""
clientSecret = ""
```

详细配置说明请参考 `config/verifymc-common.toml` 文件内的注释。

## 原项目

本项目基于原 VerifyMC 项目进行移植和适配：
- 原项目地址：https://github.com/KiteMC/VerifyMC
- 移植原因：适配多种模组加载器和 Minecraft 版本

## 许可证

MIT License

## 贡献

欢迎提交 Issue 和 Pull Request 来帮助改进这个项目。

## 联系方式

如有问题或建议，请通过以下方式联系：
- GitHub Issues
- [其他联系方式]

---

**注意**：这是一个移植版本，目前仅适配了 NeoForge 1.21.1。其他加载器和版本的适配工作正在进行中。
