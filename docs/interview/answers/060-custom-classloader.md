# 060 什么场景需要自定义 ClassLoader？

[返回按分类学习面试题](../README.md)

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
