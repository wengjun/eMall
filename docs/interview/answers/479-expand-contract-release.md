# 479 什么是 expand-contract 发布模式？

[返回按分类学习面试题](../README.md)

## 题目

什么是 expand-contract 发布模式？

## 先给面试官的短答案

expand-contract 是一种面向兼容性的发布模式。expand 阶段先扩展系统能力，例如新增字段、表、接口或消息字段，
并保证旧版本仍可运行；中间阶段让新旧版本并存、双写或兼容读写；contract 阶段在确认所有旧依赖消失后，
再删除旧字段、旧接口或旧逻辑。

## 为什么需要它

分布式系统里，服务不会同时升级完成。滚动发布、灰度发布和回滚都会让新旧版本共存。如果直接删除字段或接口，
旧版本可能立即失败。expand-contract 的目标是把破坏性变更拆成多个安全变更。

## 一个数据库例子

假设订单表要把 `address` 字段拆成 `province`、`city` 和 `detail`：

1. expand：先新增三个字段，旧字段继续保留。
2. migrate：新版本同时写旧字段和新字段，后台迁移历史数据。
3. switch：应用读取新字段，读不到时兼容旧字段。
4. verify：通过对账确认新字段完整。
5. contract：所有旧版本下线后，删除旧 `address` 字段。

这个过程比一次性改表慢，但能支持灰度、回滚和数据修复。

## 接口和消息也适用

expand-contract 不只用于数据库。API 新增字段时要保证老客户端能忽略；消息 schema 新增字段要有默认值；
删除接口前要先下线调用方；改变字段语义前要提供新字段，而不是复用旧字段。

## 在 eMall 项目中怎么讲？

eMall 的订单、支付、库存、促销和开放平台都应该采用 expand-contract。尤其是开放平台 API 和 Kafka 事件，
存在外部或异步消费者，不能假设所有调用方能同时升级。

## 深度增强：expand-contract 发布图

![蓝绿、金丝雀和数据库兼容发布](../assets/release-compatibility.svg)

expand-contract 的核心是把破坏性变更拆成多个非破坏性步骤。它不是只适用于数据库，
也适用于 API 字段、Kafka 事件、缓存 key、搜索索引和配置项。关键目标是让新旧版本能共存。

## 深度增强：订单地址拆分代码示例

```java
record OrderAddress(
        String province,
        String city,
        String detail,
        String legacyAddress) {
}

final class AddressCompatibilityMapper {

    String readDisplayAddress(OrderAddress address) {
        if (hasText(address.province()) && hasText(address.city()) && hasText(address.detail())) {
            return address.province() + address.city() + address.detail();
        }
        return address.legacyAddress();
    }

    OrderAddress dualWrite(String province, String city, String detail) {
        String legacy = province + city + detail;
        return new OrderAddress(province, city, detail, legacy);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
```

迁移期间新版本双写新旧字段，读取时优先读新字段，缺失时回退旧字段。等历史数据迁移完成、
所有旧版本下线、监控确认没有旧字段读写后，才进入 contract 阶段删除旧字段。

## 深度增强：生产边界

expand 阶段可以新增字段、表、索引、API 字段或消息字段，但要保证旧消费者能忽略。
migrate 阶段要有对账，确认新旧字段语义一致。contract 阶段最危险，必须有调用方下线证明、
读写流量为零证明和回滚预案。

大表加字段、建索引和回填数据不能一次性阻塞线上。要使用在线 DDL、分批迁移、限速、断点续跑、
校验任务和灰度读切换。否则 expand-contract 的设计正确，执行仍可能造成数据库事故。

## 深度增强：面试高分表达

我会把 expand-contract 定义为分布式系统处理破坏性变更的默认模式。它牺牲一次上线速度，
换取灰度、回滚和可验证。对于订单、支付、库存、开放平台和 Kafka 事件，我会坚持先兼容扩展，
中间双写和校验，最后再安全收缩，而不是一次发布里同时改代码、改表和删字段。

## 专家级完整回答

```text
expand-contract 是生产发布中处理破坏性变更的核心方法。

它先扩展兼容能力，让新旧版本可以同时运行；再通过迁移、双写、灰度和校验完成切换；
最后确认没有旧版本或旧消费者依赖后，才收缩删除旧结构。

这个模式本质上牺牲一次性变更的速度，换取可灰度、可回滚和可验证。
在高并发电商系统里，这是数据库 schema、API contract 和消息 schema 演进的基本要求。
```

## 回答评分点

高分答案应该覆盖：

- 说明 expand 是兼容扩展，contract 是安全收缩。
- 知道新旧版本共存是分布式发布的常态。
- 能用数据库字段迁移举例。
- 能扩展到 API 和消息 schema。
- 能说明它对灰度、回滚和数据修复的价值。
## 深度完善：专项验收清单

围绕「什么是 expand-contract 发布模式？」，这道题原本已经有专题深度增强；这里再补一层面向生产和 L6 面试的验收口径。
回答时要把概念、代码、数据、失败路径和指标串起来，证明自己不是只理解单点知识。

### 项目落点

- 先说明它在 eMall 哪个模块或链路中出现，例如交易、库存、支付、搜索、风控、发布或可观测性。
- 再说明它保护的核心目标：正确性、可用性、延迟、成本、安全或协作效率。
- 最后补失败场景：超时、重试、重复请求、状态不一致、热点流量、配置错误或发布回滚。

### 验收证据

- 代码证据：关键类、状态机、唯一约束、事务边界、线程池隔离或配置项。
- 测试证据：单元测试、集成测试、契约测试、压测、故障注入或回归用例。
- 运行证据：指标看板、Trace、结构化日志、告警、Runbook、对账结果或补偿记录。

### 高分收束

面试最后要回到取舍：当前方案为什么足够简单可靠，什么时候需要升级，升级时如何灰度、回滚和验证。
这样回答能体现生产系统判断力，而不是只罗列技术名词。

深度完善标记：专题增强答案已补项目落点、验收证据和取舍收束。
