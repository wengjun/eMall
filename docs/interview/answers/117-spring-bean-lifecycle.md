# 117 Bean 的生命周期是什么？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

Spring Bean 生命周期大致包括 BeanDefinition 加载、实例化、依赖注入、Aware 回调、BeanPostProcessor 前置处理、
初始化方法、BeanPostProcessor 后置处理、使用、销毁。理解生命周期能帮助排查依赖注入、代理、AOP 和初始化顺序问题。

## 主要阶段

主要流程：

```text
BeanDefinition -> instantiate -> populate -> aware -> postProcessBeforeInitialization
-> init -> postProcessAfterInitialization -> ready -> destroy
```

不同 Bean 类型和作用域会有细节差异。

## BeanDefinition

Spring 先扫描或读取配置，得到 BeanDefinition。

BeanDefinition 描述：

- Bean class。
- scope。
- 构造参数。
- 依赖属性。
- 初始化方法。
- 销毁方法。

它是创建 Bean 的蓝图。

## 实例化

实例化是创建对象本身。

可能通过：

- 构造函数。
- 工厂方法。
- FactoryBean。

构造函数注入发生在这个阶段。

## 依赖注入

Spring 给 Bean 填充依赖。

例如：

- 字段注入。
- Setter 注入。
- 配置属性绑定。

构造函数注入的依赖更早完成。

## Aware 回调

如果 Bean 实现了某些 Aware 接口，Spring 会注入容器相关对象。

例如：

- BeanNameAware。
- ApplicationContextAware。
- EnvironmentAware。

业务代码不应过度依赖容器 API，否则会增加耦合。

## 初始化

初始化阶段可能执行：

- `@PostConstruct`。
- `InitializingBean.afterPropertiesSet()`。
- 自定义 init method。

注意不要在初始化中做过重远程调用，否则服务启动会变慢或失败。

## BeanPostProcessor

BeanPostProcessor 可以在初始化前后处理 Bean。

AOP 代理常在后置处理阶段生成。

所以你最终注入的对象可能不是原始对象，而是代理对象。

这也是事务和切面生效的基础。

## 销毁

容器关闭时会调用销毁逻辑。

例如：

- `@PreDestroy`。
- `DisposableBean.destroy()`。
- 自定义 destroy method。

用于关闭线程池、连接、文件和后台任务。

## 在 eMall 项目中怎么讲？

eMall 服务中，HTTP 客户端、线程池、缓存预热器都需要正确初始化和销毁。

但不能在 Bean 初始化阶段强依赖所有下游可用。非核心预热应异步执行，并通过 readiness 控制接流量。
