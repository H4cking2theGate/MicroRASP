# MicroRASP

轻量级 Java RASP Agent，基于 Byte Buddy，聚焦运行时拦截和实时阻断。支持以 Java Agent 方式零侵入部署，并自动扫描 `com.h2tg.rasp.hooks` 包中的 @HookHandler 进行织入。

## 亮点
- Java Agent 零侵入：支持 `premain` / `agentmain`，可随 JVM 启动或动态 attach。
- 上下文感知：Servlet/Jakarta Servlet 入口处记录请求上下文，部分 Hook 仅在 HTTP 请求中生效以降低误报。
- 覆盖核心攻击面：反序列化、JNDI 注入、RMI 远程加载、命令执行、Native 库加载等。
- 跨版本兼容：目标编译级别 Java 8；同时覆盖 `javax.servlet` 与 `jakarta.servlet`，并支持 JDK 8/11/17 的 native Hook。
- 内置日志：`rasp-logs/microrasp.log`（可通过 `-Drasp.log.path` 修改），同时输出到控制台。

## 已实现的 Hook 与行为
| 攻击面 | Hook 点 | 触发条件 | 处置 | 备注 |
| --- | --- | --- | --- | --- |
| 请求上下文跟踪 | `javax.servlet.http.HttpServlet#service`<br>`jakarta.servlet.http.HttpServlet#service` | 所有 Servlet/JSP 请求 | 记录 ThreadLocal 请求对象 | 为其他 Hook 提供上下文 |
| 命令执行 | `java.lang.ProcessImpl#create` (Win)<br>`ProcessImpl#forkAndExec` (JDK9+ Linux)<br>`java.lang.UNIXProcess#forkAndExec` (JDK8 Linux) | HTTP 请求上下文存在 | 抛出 `SecurityException` 阻断 | 非 Web 场景放行 |
| Java 反序列化 | `java.io.ObjectInputStream#readClassDesc` | 解析类名命中 `SerialHelper.denyClasses` | 抛出 `SecurityException` 阻断 | 全场景拦截 |
| JNDI 注入 | `javax.naming.spi.NamingManager#getObjectFactoryFromReference` | 存在远程 `codebase` 或命中 `JndiHelper.denyFactories` | 抛出 `SecurityException` 阻断 | |
| RMI 远程加载 | `sun.rmi.server.LoaderHandler#lookupLoader` | 请求的 codebase 非空 | 抛出 `SecurityException` 阻断 | |
| Native 库加载 | `jdk.internal.loader.NativeLibraries#load` (JDK9+)<br>`java.lang.ClassLoader.NativeLibrary#load` (JDK8) | 调用即触发 | 抛出 `SecurityException` 阻断 | 无白名单 |
| 文件读写 | （代码存在于 `FileHook.java` 但已整体注释） | - | - | 需手动启用/完善 |
| SQLi | `SqliHook` 占位 | - | - | 尚未实现 |

> 重要：JNDI/RMI/反序列化/Native Hook 默认全量阻断，可能影响依赖相关特性的业务；命令执行 Hook 仅在检测到 HTTP 请求上下文后阻断。

## 快速开始
### 构建
```bash
mvn clean package
# 产物：target/MicroRASP-0.1-shaded.jar
```

### 以 Java Agent 启动（推荐）
```bash
java -javaagent:/path/to/MicroRASP-0.1-shaded.jar -jar your-app.jar
```

### 动态 Attach 示例
```java
import com.sun.tools.attach.VirtualMachine;

public class AttachAgent {
    public static void main(String[] args) throws Exception {
        VirtualMachine vm = VirtualMachine.attach("12345"); // 目标 JVM PID
        vm.loadAgent("/path/to/MicroRASP-0.1-shaded.jar");
        vm.detach();
    }
}
```

启动后日志类似：
```
[MicroRASP] MicroRASP Agent Starting...
[MicroRASP] Injecting 4 class(es) to Bootstrap ClassLoader...
[MicroRASP] Discovered X hook handler(s) in package com.h2tg.rasp.hooks
[MicroRASP] Registered hook: target=java.io.ObjectInputStream#readClassDesc isNative=false ...
[MicroRASP] MicroRASP Agent Installed Successfully
```

## 工作原理
1. `premain/agentmain` 入口调用 `Agent.install`。
2. 将 `RequestContext`/`SerialHelper`/`JndiHelper`/`FileHelper` 注入 Bootstrap ClassLoader，解决跨 ClassLoader 访问。
3. `HookRegistry` 使用 Reflections 扫描 `com.h2tg.rasp.hooks` 中的 `@HookHandler`，逐个注册到 Byte Buddy。
4. Byte Buddy `AgentBuilder` 采用 `RETRANSFORMATION` 策略，忽略自身/依赖包并添加 `HookListener` 记录织入日志。
5. 安装到目标 JVM 后，Advice 在运行时拦截方法并执行阻断/记录逻辑。

### 代码结构
```
src/main/java/com/h2tg/rasp
├── Agent.java                # Agent 安装流程，Bootstrap 注入与 Hook 注册
├── Main.java                 # 占位 main，提示使用 -javaagent
├── annotation/HookHandler.java
├── bootstrap/                # 注入到 Bootstrap 的共享工具
│   ├── RequestContext.java
│   ├── SerialHelper.java
│   ├── JndiHelper.java
│   └── FileHelper.java
├── core/
│   ├── HookRegistry.java     # 扫描并注册 Advice
│   └── HookListener.java     # Byte Buddy 织入日志
├── hooks/                    # 具体 Hook（多数阻断逻辑内联）
└── log/MicroLogger.java      # 控制台 + 文件日志，`-Drasp.log.path` 可重定向
```

## 配置要点
- 反序列化黑名单：编辑 `src/main/java/com/h2tg/rasp/bootstrap/SerialHelper.java` 的 `denyClasses`。
- JNDI 工厂黑名单：编辑 `src/main/java/com/h2tg/rasp/bootstrap/JndiHelper.java` 的 `denyFactories`。
- 日志路径：`-Drasp.log.path=/var/log/rasp`（默认相对路径 `rasp-logs`）。
- FileHelper 黑名单：`FileHelper` 定义了敏感路径/后缀（用于未来文件 Hook），当前未生效。

## 兼容性与限制
- 依赖 Byte Buddy 1.14.12，编译级别 Java 8；Native Hook 已适配 JDK8 与 JDK9+ 的不同类名。
- JNDI/RMI/反序列化/Native Hook 默认强阻断，需在生产前验证第三方组件依赖。
- 命令执行 Hook 仅在 HTTP 请求上下文中阻断；非 Web 应用默认放行。
- 文件读写 Hook 代码目前整文件注释，SqliHook 为占位，尚未提供 SQL/文件防护。
- 暂无开关化配置与白名单，若需灰度/放行策略需自行扩展。

## 开发与验证建议
- 构建：`mvn clean package`，产物 `target/MicroRASP-0.1-shaded.jar` 已带 Manifest（Premain/Agent-Class）。
- 最小验证：在 Web 应用中访问 Servlet/JSP，触发 `RequestHook` 后再执行 `Runtime.exec` 观察阻断；尝试反序列化/恶意 JNDI URL 验证拦截效果。
- 性能/兼容性：在目标 JDK 版本、容器（Tomcat/Spring Boot）上分别验证是否有误报或启动冲突，必要时调整黑名单或放宽策略。
