# 033 SPI 机制适合解决什么扩展问题？

[返回按分类学习面试题](../README.md)

## 题目

SPI 机制适合解决什么扩展问题？

## 先给面试官的短答案

SPI 是 Service Provider Interface，适合框架定义扩展接口、第三方或不同模块提供实现的场景。
它解决的是“核心流程稳定，但某些能力需要可插拔扩展”的问题。

典型场景包括支付渠道、风控规则、物流承运商、数据导出格式、加密算法、ID 生成器。

## 从零基础理解

API 通常是调用别人提供的能力；SPI 是你定义扩展点，让别人来提供能力。

例如支付服务定义接口：

```java
public interface PaymentChannel {
    String channelCode();

    PaymentResult pay(PaymentCommand command);
}
```

支付宝、微信、银行卡分别提供实现。支付服务只依赖接口，不依赖具体实现细节。

## SPI 适合的场景

### 多支付渠道

核心支付流程稳定：

```text
创建支付单 -> 调渠道 -> 接收回调 -> 入账 -> 对账
```

但渠道实现不同。适合用 SPI 或策略模式扩展。

### 风控规则

风控规则经常变：

- 黑名单规则。
- 设备风险规则。
- IP 风险规则。
- 金额阈值规则。

可以定义统一规则接口。

### 导出格式

订单报表可能导出 CSV、Excel、JSON。核心数据查询稳定，导出格式可扩展。

### 加密和签名算法

不同场景可能使用不同算法，但调用方只依赖 `FieldEncryptor` 或 `Signer` 接口。

## Java 原生 SPI

Java 提供 `ServiceLoader`。

大致流程：

```text
定义接口
实现接口
在 META-INF/services/接口全名 文件中写实现类全名
运行时 ServiceLoader 加载
```

Spring 项目里也常用 Bean 注入实现类似机制：

```java
public PaymentService(List<PaymentChannel> channels) {
    this.channels = channels.stream()
            .collect(Collectors.toMap(PaymentChannel::channelCode, Function.identity()));
}
```

## SPI 的治理难点

SPI 最大难点不是加载实现，而是扩展点治理：

- 接口要稳定。
- 输入输出要明确。
- 异常和超时要统一。
- 插件权限要受控。
- 插件指标要独立。
- 插件升级要可灰度和回滚。

## 不适合 SPI 的场景

- 核心业务规则还没稳定。
- 扩展点边界不清楚。
- 只是为了复用几行代码。
- 插件需要随意访问核心数据库。
- 每个实现差异过大，抽象不成立。

## 专家级完整回答

```text
SPI 适合核心流程稳定、局部能力可插拔的场景。比如支付系统的渠道实现、风控规则、
物流承运商、数据导出格式。核心服务定义稳定接口，实现方提供具体实现。

但 SPI 的关键是治理。扩展点要小而稳定，输入输出、异常、超时、指标和权限都要明确。
否则插件机制会变成绕过核心规则的后门。
```

## 回答评分点

高分答案应该覆盖：

- 能解释 SPI 和 API 的差异。
- 能举支付、风控、导出、加密例子。
- 能说明 Java ServiceLoader 或 Spring Bean 列表。
- 能强调接口稳定和治理。
- 能指出不适合未稳定核心业务。
