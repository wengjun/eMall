# 060 什么场景需要自定义 ClassLoader？

[返回按分类学习面试题](../README.md)

## 题目

什么场景需要自定义 ClassLoader？

## 先给面试官的短答案

需要自定义 ClassLoader 的场景通常是运行时扩展和隔离，例如插件系统、脚本或规则引擎、
热部署、依赖版本隔离、加密 class 加载、从非标准位置加载 class。普通业务服务不要轻易自定义，
因为它会带来类冲突、安全风险和类加载器泄漏。

面试中要强调：自定义 ClassLoader 是架构能力，不是日常炫技。

## 为什么默认 ClassLoader 不够？

默认应用类加载器适合大多数服务，因为代码和依赖在启动时就确定了。

但有些场景要求运行时改变能力：

- 运行时加载新插件。
- 同一 JVM 中隔离不同版本依赖。
- 从网络或数据库加载字节码。
- 动态卸载某个模块。
- 对 class 文件做解密或校验。

这时默认 ClassLoader 不够灵活。

## 场景一：插件系统

插件系统是最典型场景。

例如电商平台允许不同商家扩展促销规则：

```text
platform API -> merchant plugin implementation
```

平台 API 应该由父加载器加载，插件实现和插件依赖由插件 ClassLoader 加载。

这样不同插件可以使用不同依赖版本，互不影响。

## 场景二：热部署

热部署需要在不重启 JVM 的情况下加载新版本代码。

实现思路通常是：

- 每个版本使用新的 ClassLoader。
- 旧请求继续使用旧版本。
- 新请求切换到新版本。
- 旧版本没有引用后释放 ClassLoader。

注意：类本身通常不能单独卸载，类卸载依赖加载它的 ClassLoader 可被 GC。

## 场景三：依赖版本隔离

大型平台可能同时运行多个插件，而插件依赖版本不同。

例如：

```text
plugin-a uses rule-engine 1.0
plugin-b uses rule-engine 2.0
```

如果全部放到应用 classpath，版本会冲突。

使用不同 ClassLoader 可以隔离依赖，但共享 API 必须放在父加载器中。

## 场景四：从特殊来源加载类

类字节码不一定来自本地文件。

可能来自：

- 远程仓库。
- 数据库。
- 对象存储。
- 加密包。
- 动态生成的字节码。

自定义 ClassLoader 可以重写查找字节码的逻辑。

## 场景五：安全和审计

有些平台需要在加载前做安全校验：

- 校验签名。
- 检查白名单。
- 禁止危险包名。
- 限制可访问 API。
- 记录插件版本和来源。

不过安全隔离不能只靠 ClassLoader。强安全场景还需要沙箱、进程隔离或容器隔离。

## 自定义 ClassLoader 的核心方法

常见方式是继承 `ClassLoader`，重写 `findClass`，再调用 `defineClass`。

示例：

```java
public final class PluginClassLoader extends ClassLoader {
    private final PluginBytecodeRepository repository;

    public PluginClassLoader(ClassLoader parent, PluginBytecodeRepository repository) {
        super(parent);
        this.repository = repository;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] bytecode = repository.load(name)
                .orElseThrow(() -> new ClassNotFoundException(name));
        return defineClass(name, bytecode, 0, bytecode.length);
    }
}
```

真实生产实现还要处理资源加载、包密封、依赖查找、并发加载和安全校验。

## 常见风险

自定义 ClassLoader 风险很高：

- 同名类冲突。
- `ClassCastException`。
- 类加载器泄漏。
- ThreadLocal 泄漏。
- 插件线程未关闭。
- 静态缓存持有插件类。
- 依赖版本不可控。
- 安全边界不完整。

最常见线上问题是类加载器泄漏。插件卸载了，但某个线程、缓存或静态变量还持有插件类。

## 如何设计插件隔离？

生产设计要明确：

- 哪些 API 由平台提供。
- 哪些依赖允许插件自带。
- 哪些包禁止插件加载。
- 插件如何注册和卸载。
- 插件线程如何停止。
- 插件缓存如何清理。
- 插件异常如何隔离。
- 插件资源如何限流。

插件能力必须被治理，否则会变成平台稳定性风险。

## 在 eMall 项目中怎么讲？

eMall 可以在这些模块中使用插件思路：

- 营销规则插件。
- 商家定制计费规则。
- 搜索排序扩展。
- 风控策略扩展。
- 履约路由策略。

但核心交易链路不建议随意运行不受控插件。更稳妥的做法是先用配置化规则、DSL 或独立策略服务。
只有当扩展能力和隔离需求非常明确时，才引入自定义 ClassLoader。

## 深度增强：JVM 生产运行图

![Java 17 容器内 JVM 内存结构](../assets/jvm-runtime-memory.svg)

JVM 题要从运行时资源解释到业务影响。堆、直接内存、元空间、线程栈和容器 memory limit 共同决定服务稳定性；
GC、CPU throttling、线程池队列和下游超时会一起影响 P99，而不是孤立存在。

## 深度增强：Java 17 诊断模型示例

```java
record RuntimeSignal(
        double heapUsage,
        double containerMemoryUsage,
        long gcPauseMillis,
        int threadCount,
        int queuedTasks) {

    boolean requiresTriage() {
        return heapUsage > 0.85
                || containerMemoryUsage > 0.90
                || gcPauseMillis > 500
                || threadCount > 800
                || queuedTasks > 1_000;
    }
}
```

这个模型强调线上诊断要看组合信号。只看 heap 不够，只看 GC 也不够；
要把 JVM、容器、线程池和业务延迟放到同一条时间线。

## 深度增强：生产边界

JVM 调优不能靠背参数。要先明确服务目标：低延迟、吞吐、容器资源、对象分配速率和 P99 SLO。
然后通过 GC 日志、JFR、指标和压测验证。错误地调大 `-Xmx` 可能挤压堆外内存，导致容器 OOMKilled。

## 深度增强：面试高分表达

我会用证据链回答 JVM 问题：先看业务影响，再看 JVM 指标、GC 日志、线程栈、heap dump、容器事件和最近变更。
结论要能解释现象，并能给出降级、扩容、参数调整或代码优化方案。

## 专家级完整回答

```text
自定义 ClassLoader 适合插件、热部署、依赖版本隔离、加密 class 和非标准来源加载类。
普通业务服务不应该轻易使用，因为 JVM 中类身份由类名和 ClassLoader 决定，错误隔离会导致
ClassCastException、依赖冲突和类加载器泄漏。

如果我要在电商系统中设计插件能力，会把平台 API 放在父加载器，插件实现和依赖放在独立
插件加载器中，同时限制可加载包、校验签名、隔离异常、关闭插件线程并清理 ThreadLocal。
核心交易链路优先使用配置化或独立策略服务，避免把不受控插件放进主进程。
```

## 回答评分点

高分答案应该覆盖：

- 插件、热部署、依赖隔离是核心场景。
- 普通业务不要随意自定义。
- 知道 `findClass` 和 `defineClass`。
- 知道类加载器泄漏。
- 知道父加载器放公共 API。
- 能联系电商规则和风控插件。
