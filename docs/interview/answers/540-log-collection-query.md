# 540 设计日志采集和查询平台

[返回按分类学习面试题](../README.md)

## 题目

设计日志采集和查询平台。

## 先给面试官的短答案

日志平台负责采集、传输、解析、存储、索引、查询和归档应用日志。核心要求是低侵入、可靠采集、结构化、
可按 traceId 和业务字段查询、成本可控、敏感信息脱敏，并且日志系统故障不能影响交易链路。

## 架构设计

应用输出结构化日志到 stdout 或文件，节点 agent 采集日志，发送到 Kafka 或日志管道，再由消费端解析、清洗、脱敏、
索引到 Elasticsearch、OpenSearch 或 ClickHouse。

冷数据可以归档到对象存储，降低成本。

## 日志内容

生产日志应包含 timestamp、level、service、instance、traceId、spanId、userId hash、orderId、errorCode、
latency、dependency 和关键业务状态。

不能记录密码、token、身份证、银行卡、手机号明文和支付敏感信息。

## 查询能力

平台要支持按 traceId、订单号、用户 hash、错误码、服务、时间范围和关键字段查询。还要支持上下文日志、
聚合统计和异常模式发现。

高基数字段和全文索引会带来成本，要做索引策略和保留周期管理。

## 可用性

日志采集失败不能阻塞业务线程。应用应异步写日志并有丢弃策略。日志管道积压时要告警，但不能反压核心交易。

## 在 eMall 项目中怎么讲？

eMall 的订单、支付、库存日志要包含 traceId、orderId、paymentId、skuId 和业务状态。日志脱敏由 `common` 或
`governance` 提供，`operations` 提供查询和审计。

## 深度增强：可观测平台图

![日志、指标和告警平台](../assets/observability-platform.svg)

日志平台解决“发生了什么”。指标平台解决“影响有多大”。Trace 解决“请求经过了哪里”。
日志系统必须和交易链路解耦，不能因为日志管道故障导致下单或支付阻塞。

## 深度增强：结构化日志模型

```java
public record BusinessLogEvent(
        Instant timestamp,
        String level,
        String service,
        String traceId,
        String orderId,
        String userHash,
        String eventName,
        String errorCode,
        long latencyMillis) {
}
```

日志脱敏应该在统一组件中做：

```java
public final class LogMasker {

    public String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return "***";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    public String maskToken(String token) {
        return token == null ? null : "***";
    }
}
```

## 深度增强：生产边界

- 日志写入要异步，满队列时可丢弃低等级日志。
- 错误日志要保留上下文，但不能记录密码、token、银行卡、身份证明文。
- 高基数字段要谨慎建索引，避免日志平台成本失控。
- 核心交易日志要包含 traceId、orderId、paymentId、skuId、errorCode。
- 日志平台故障只能影响排障能力，不能影响交易链路。

## 深度增强：面试高分表达

```text
我会要求应用输出结构化日志，由 agent 采集到日志管道，再做清洗、脱敏、索引和冷热归档。
查询要支持 traceId、订单号、错误码和服务维度。日志平台不能反压业务线程，
否则一次日志系统故障会变成交易系统故障。
```

## 专家级完整回答

```text
日志平台的价值是让故障排查有证据链。

我会要求应用输出结构化日志，由 agent 采集到消息管道，再清洗、脱敏、索引和归档。
查询上支持 traceId、订单号、错误码和业务字段。

日志平台必须和交易链路解耦。日志写入失败不能影响下单和支付，同时要避免敏感信息泄露。
成本方面要通过索引策略、采样和冷热分层控制。
```

## 回答评分点

高分答案应该覆盖：

- 覆盖采集、传输、解析、存储、索引、查询和归档。
- 强调结构化日志和 traceId。
- 知道敏感信息脱敏。
- 能说明日志系统故障不能影响业务。
- 能提到索引成本、采样和冷热分层。
## 深度完善：专项验收清单

围绕「设计日志采集和查询平台」，这道题原本已经有专题深度增强；这里再补一层面向生产和 L6 面试的验收口径。
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
