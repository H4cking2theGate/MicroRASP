# MicroRASP

[![Java](https://img.shields.io/badge/Java-8%2B-blue.svg)](https://www.oracle.com/java/)
[![Byte Buddy](https://img.shields.io/badge/Byte%20Buddy-1.14.12-green.svg)](https://bytebuddy.net/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

> **MicroRASP** 是一个轻量级的 Java 运行时应用自我保护（RASP）Agent，基于 Byte Buddy 实现，提供零侵入式的实时安全防护能力。

## ✨ 特性

- 🚀 **零侵入部署** - 通过 Java Agent 方式加载，无需修改应用代码
- 🛡️ **多维度防护** - 覆盖反序列化、JNDI 注入、RMI、进程执行、Native 库加载等攻击面
- 🔧 **灵活扩展** - 基于注解的 Hook 机制，支持快速添加自定义防护规则
- ⚡ **性能优化** - 精准匹配目标方法，最小化运行时开销
- 🌐 **跨版本兼容** - 支持 JDK 8 / JDK 11 / JDK 17+
- 🎯 **上下文感知** - 只在 HTTP 请求上下文中拦截，避免误杀正常流程

## 📋 支持的攻击防护

### ✅ 已实现

| 攻击类型 | Hook 点 | 描述 | 状态 |
|---------|---------|------|------|
| **Java 反序列化** | `ObjectInputStream.resolveClass` | 拦截危险类的反序列化操作 | ✅ 已实现 |
| **JNDI 注入** | `NamingManager.getObjectFactoryFromReference` | 检测远程 codebase 和危险工厂类 | ✅ 已实现 |
| **RMI 远程加载** | `LoaderHandler.lookupLoader` | 阻止 RMI codebase 远程类加载 | ✅ 已实现 |
| **命令执行** | `ProcessImpl.create` / `forkAndExec` | 阻止 HTTP 请求上下文中的进程执行 | ✅ 已实现 |
| **Native 库加载** | `NativeLibraries.load` / `NativeLibrary.load` | 阻止动态加载恶意 Native 库 | ✅ 已实现 |
| **HTTP 请求跟踪** | `HttpServlet.service` | 记录请求上下文，实现上下文感知防护 | ✅ 已实现 |

### 🚧 计划中

| 攻击类型 | 计划 Hook 点 | 描述 | 优先级 |
|---------|-------------|------|--------|
| **SQL 注入** | `Statement.execute*` / `PreparedStatement.execute*` | 检测和阻止 SQL 注入攻击 | 🔥 高 |
| **文件操作** | `FileInputStream` / `FileOutputStream` / `RandomAccessFile` | 防止任意文件读写 | 🔥 高 |
| **XXE 攻击** | `DocumentBuilder.parse` / `XMLReader.parse` | 防止 XML 外部实体注入 | 🔥 高 |
| **SSRF 攻击** | `URL.openConnection` / `HttpURLConnection.connect` | 防止服务端请求伪造 | 🔥 高 |
| **表达式注入** | `ScriptEngineManager.eval` / OGNL / SpEL | 防止代码注入和表达式注入 | 🔶 中 |
| **反射调用** | `Method.invoke` / `Class.forName` | 监控危险的反射调用 | 🔶 中 |
| **文件上传** | `FileUpload` / `MultipartFile` | 检测恶意文件上传 | 🔶 中 |
| **模板注入** | Freemarker / Velocity / Thymeleaf | 防止模板注入攻击 | 🔶 中 |
| **WebSocket** | WebSocket 连接和消息处理 | WebSocket 安全防护 | 🔷 低 |
| **GraphQL** | GraphQL 查询执行 | GraphQL 注入防护 | 🔷 低 |

## 🚀 快速开始

### 1. 构建项目

```bash
# 克隆项目
git clone https://github.com/yourusername/MicroRASP.git
cd MicroRASP

# 使用 Maven 构建
mvn clean package

# 生成的 JAR 位于 target/ 目录
# MicroRASP-0.1-shaded.jar
```

### 2. 部署方式

#### 方式一：JVM 启动时加载（推荐）

```bash
java -javaagent:/path/to/MicroRASP-0.1-shaded.jar -jar your-application.jar
```

#### 方式二：动态 Attach（运行时加载）

```java
import com.sun.tools.attach.VirtualMachine;

public class AttachAgent {
    public static void main(String[] args) throws Exception {
        String pid = "12345";  // 目标 JVM 进程 PID
        String agentJar = "/path/to/MicroRASP-0.1-shaded.jar";

        VirtualMachine vm = VirtualMachine.attach(pid);
        vm.loadAgent(agentJar);
        vm.detach();
    }
}
```

### 3. 验证安装

启动应用后，控制台会输出：

```
[MicroRASP] ========================================
[MicroRASP] MicroRASP Agent Starting...
[MicroRASP] ========================================
[MicroRASP] Injecting 3 class(es) to Bootstrap ClassLoader...
[MicroRASP] Discovered 6 hook handler(s) in package com.h2tg.rasp.hooks
[MicroRASP] Registered hook: target=java.io.ObjectInputStream#resolveClass
[MicroRASP] Registered hook: target=javax.naming.spi.NamingManager#getObjectFactoryFromReference
[MicroRASP] ========================================
[MicroRASP] MicroRASP Agent Installed Successfully
[MicroRASP] ========================================
```

## 🏗️ 架构设计

### 核心组件

```
MicroRASP
├── Agent.java                      # Java Agent 入口，负责初始化和安装
├── annotation/
│   └── HookHandler.java            # Hook 注解，标记拦截点
├── core/
│   ├── HookRegistry.java           # Hook 注册中心，自动扫描和注册
│   └── HookListener.java           # 转换监听器，记录插桩日志
├── bootstrap/
│   ├── RequestContext.java         # HTTP 请求上下文（Bootstrap ClassLoader）
│   ├── SerialHelper.java           # 反序列化黑名单（Bootstrap ClassLoader）
│   └── JndiHelper.java             # JNDI 安全检查（Bootstrap ClassLoader）
├── hooks/
│   ├── SerialHook.java             # 反序列化防护
│   ├── JndiHook.java               # JNDI/RMI 注入防护
│   ├── ProcessHook.java            # 命令执行防护
│   ├── JNIHook.java                # Native 库加载防护
│   └── RequestHook.java            # HTTP 请求上下文跟踪
└── log/
    └── MicroLogger.java            # 日志工具（基于 java.util.logging）
```

### 工作流程

```
1. Agent.premain/agentmain
   ↓
2. 注入 Bootstrap 类到 Bootstrap ClassLoader
   ↓
3. HookRegistry 扫描 @HookHandler 注解
   ↓
4. 构建 AgentBuilder（Byte Buddy）
   ↓
5. 注册所有 Hook 到 AgentBuilder
   ↓
6. 安装 Agent 到目标 JVM
   ↓
7. 运行时拦截目标方法，执行安全检查
```

## 🔧 自定义 Hook

### 1. 创建 Hook 类

```java
package com.h2tg.rasp.hooks;

import com.h2tg.rasp.annotation.HookHandler;
import com.h2tg.rasp.bootstrap.RequestContext;
import net.bytebuddy.asm.Advice;

public class CustomHook {

    @HookHandler(
            hookClass = "com.example.TargetClass",
            hookMethod = "dangerousMethod",
            parameterTypes = {"java.lang.String"}
    )
    public static class DangerousMethodAdvice {

        @Advice.OnMethodEnter
        static void onEnter(@Advice.Argument(0) String input) {
            Object request = RequestContext.getCurrentRequest();
            if (request == null) {
                return;  // 非 HTTP 请求，放行
            }

            // 自定义安全检查逻辑
            if (isBlacklisted(input)) {
                System.err.println("[MicroRASP] [BLOCKED] Dangerous input: " + input);
                RequestContext.logRequestInfo(request);
                throw new SecurityException("MicroRASP blocked dangerous input: " + input);
            }
        }

        private static boolean isBlacklisted(String input) {
            // 实现黑名单检查
            return input.contains("malicious");
        }
    }
}
```

### 2. @HookHandler 注解参数

| 参数 | 类型 | 说明 | 默认值 |
|-----|------|------|--------|
| `hookClass` | String | 目标类的完全限定名 | - |
| `hookMethod` | String | 目标方法名 | - |
| `parameterTypes` | String[] | 方法参数类型（完全限定名） | `{"*"}` |
| `isConstructor` | boolean | 是否为构造方法 | `false` |
| `isNative` | boolean | 是否为 Native 方法 | `false` |

### 3. 重新构建并部署

```bash
mvn clean package
java -javaagent:target/MicroRASP-0.1-shaded.jar -jar your-app.jar
```

## 📝 配置说明

### 反序列化黑名单

编辑 `SerialHelper.java` 中的 `denyClasses` 数组：

```java
public static final String[] denyClasses = {
    "com.sun.rowset.",              // JNDI 注入
    "org.apache.commons.collections.functors.",  // Commons Collections
    "org.springframework.beans.factory.",        // Spring RCE
    "java.lang.Runtime",            // 命令执行
    // 添加自定义黑名单...
};
```

### JNDI 工厂类黑名单

编辑 `JndiHelper.java` 中的 `denyFactories` 数组：

```java
public static final String[] denyFactories = {
    "org.apache.naming.factory.",
    "com.alibaba.druid.pool.DruidDataSourceFactory",
    "org.apache.tomcat.jdbc.pool.DataSourceFactory",
    // 添加自定义黑名单...
};
```

## ⚠️ 注意事项

1. **生产环境部署前请充分测试**，确保不影响业务逻辑
2. **上下文感知防护**仅在 HTTP 请求中生效，非 Web 应用需调整逻辑
3. **黑名单规则**需根据实际业务场景调整，避免误报
4. **Native 方法 Hook** 可能影响部分 JVM 优化，建议性能测试
5. **日志输出**使用 `System.err`，生产环境建议配置日志重定向
