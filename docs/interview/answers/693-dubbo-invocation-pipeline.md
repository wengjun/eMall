# 693 Dubbo 从代理调用到 Provider 执行经历什么过程？

[返回按分类学习面试题](../README.md)

## 与自研 RPC 的对应关系

已有自研 RPC 经验时，不必重复学习代理、编解码和网络收发原理；本题只记 Dubbo 对应的类型、扩展层和执行顺序。

Dubbo 没有改变 RPC 的基本物理过程，区别在于它把代理、注册发现、路由、集群容错、协议和扩展点标准化。Consumer 侧主链路可以概括为：

```text
业务接口代理
 -> Consumer Filter
 -> ClusterInvoker
 -> Directory 获取 Invoker 列表
 -> Router 过滤
 -> LoadBalance 选择
 -> Protocol Invoker
 -> 编码、连接复用、网络发送
```

Provider 侧反向经过解码、线程派发、Provider Filter、代理调用真实 Bean，再编码响应。

## Invoker 为什么是关键抽象

`Invoker<T>` 把“可调用对象”统一起来：远程 Provider、本地服务导出和集群都可表现为 Invoker。
`Directory` 给出动态 Invoker 集合，`Router` 根据标签、条件或脚本过滤，`Cluster` 把多个 Invoker
伪装成一个并实现容错。

这种分层让注册中心变化不直接侵入业务代理，也让不同协议、序列化和负载均衡通过 SPI 替换。

## 注册和引用阶段

- Provider 导出服务，创建协议监听端点并向注册中心发布 URL/元数据。
- Consumer 引用接口，订阅地址与配置变更，建立代理和集群 Invoker。
- 地址推送只改变候选视图；真正调用时还要经过路由、健康状态和负载均衡。

本地缓存让注册中心短暂不可用时可继续调用已知 Provider，但缓存地址可能陈旧，因此连接失败、路由为空和注册中心不可用必须分开观测。

## 线程和上下文边界

网络 IO 线程不能执行阻塞数据库或长业务逻辑，应尽快派发到业务线程池。Consumer 线程中的 trace、认证和 deadline
通过显式 attachment/context 传播；普通 `ThreadLocal` 不会自动跨 RPC，也可能在池化线程间泄漏。

异步调用返回 `CompletableFuture` 时，取消本地 Future 不一定能中止已经到达 Provider 的业务动作；超时后仍需幂等查询。

## 序列化和接口演进

RPC 接口是跨进程协议，不应把数据库 Entity、异常堆栈或 Java 实现类型直接暴露出去。字段采用向后兼容演进，新增可选字段、保留旧语义；发布时先让 Provider 兼容新旧请求，再升级 Consumer。

## 电商系统排障定位

一次调用慢，要按代理/filter、地址目录、路由与选择、连接池/排队、网络、Provider 线程池、业务执行和序列化拆解。只看 Provider 方法耗时会漏掉 Consumer 排队和重试放大。

参考：

- [Dubbo 官方文档](https://dubbo.apache.org/en/overview/mannual/java-sdk/reference-manual/architecture/code-architecture/)
