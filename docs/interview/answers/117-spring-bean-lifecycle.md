# 117 Bean 的生命周期是什么？

[返回按分类学习面试题](../README.md)

## 题目

Bean 的生命周期是什么？

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

## 深度增强：Spring 服务治理图

![Spring 微服务调用栈和治理边界](../assets/spring-service-stack.svg)

Spring 题要从框架机制讲到业务边界。Controller 负责协议适配，Service 负责业务事务，Repository 负责数据访问；
事务、AOP、校验、错误码、配置和观测都是为了让微服务在复杂调用中保持稳定。

## 深度增强：Java 17 分层示例

```java
record CreateOrderCommand(long userId, long skuId, int quantity) {
}

record CreateOrderResult(long orderId, String status) {
}

interface OrderApplicationService {
    CreateOrderResult create(CreateOrderCommand command);
}

final class OrderControllerAdapter {
    private final OrderApplicationService service;

    OrderControllerAdapter(OrderApplicationService service) {
        this.service = service;
    }

    CreateOrderResult submit(CreateOrderCommand command) {
        return service.create(command);
    }
}
```

这个示例表达分层边界：接口层不堆业务逻辑，业务层不依赖 Web 协议，命令和结果对象形成稳定契约。

## 深度增强：生产边界

框架默认值不能替代设计。事务传播、异常回滚、异步线程池、连接池、序列化、超时和重试都要显式治理。
尤其在订单、支付、库存链路中，要避免长事务、隐式重试和跨服务事务误用。

## 深度增强：面试高分表达

我会先解释框架原理，再说明在电商系统里怎么落地。高分回答要能把自动配置、AOP、事务、MVC、WebFlux、
校验和错误处理，连接到可维护性、可观测性、稳定性和故障恢复。

## 专家级完整回答

```text
Spring Bean 生命周期包括 BeanDefinition 加载、实例化、依赖注入、Aware 回调、BeanPostProcessor 前置处理、
初始化、BeanPostProcessor 后置处理、使用和销毁。AOP 代理通常在后置处理阶段生成，所以最终 Bean 可能是代理对象。

生产中我会避免在初始化阶段做重 IO，并在销毁阶段关闭线程池、连接和后台任务，保证服务优雅启动和停止。
```

## 回答评分点

高分答案应该覆盖：

- 能说出主要阶段。
- BeanPostProcessor 和 AOP 代理。
- 初始化不要做重 IO。
- 销毁要释放资源。
