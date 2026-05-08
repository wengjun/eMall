# 237 对账和补偿有什么区别？

[返回按分类学习面试题](../README.md)

## 题目

对账和补偿有什么区别？

## 先给面试官的短答案

对账是发现差异，补偿是修复差异。
对账通过比较多个系统的数据或流水，识别订单、支付、库存、资金等状态是否一致；补偿根据差异执行重试、状态修正、退款、释放库存等动作。

对账负责“看见问题”，补偿负责“把问题收敛”。

## 对账做什么？

对账包括：

- 拉取双方数据。
- 按业务键匹配。
- 比较金额、状态和数量。
- 识别多单、少单和状态不一致。
- 生成差异单。
- 分级告警。

对账不一定直接改数据。

## 补偿做什么？

补偿包括：

- 重发消息。
- 推进状态。
- 释放资源。
- 发起退款。
- 修复读模型。
- 人工审核后处理。

补偿必须有幂等、审计和回滚预案。

## 关系

关系：

```text
业务流程 -> 可能出现差异 -> 对账发现差异 -> 补偿修复差异 -> 再次验证
```

对账和补偿形成闭环。

## 在 eMall 项目中怎么讲？

支付对账发现支付渠道显示成功，但订单仍是待支付。

对账生成差异单，补偿任务根据支付单把订单推进为已支付，并触发后续库存确认和履约流程。

## 深度增强：一致性闭环图

![对账和补偿的一致性闭环](../assets/consistency-compensation-loop.svg)

这道题要避免只解释定义。专家级回答要说明两者怎么形成生产闭环：对账负责发现，补偿负责修复，
修复后还要再次验证，并保留审计链路。

## 深度增强：Java 17 差异单模型

```java
public enum ReconciliationDifferenceType {
    CHANNEL_SUCCESS_LOCAL_PENDING,
    LOCAL_SUCCESS_CHANNEL_MISSING,
    AMOUNT_MISMATCH,
    STATUS_MISMATCH
}

public enum DifferenceStatus {
    NEW,
    COMPENSATING,
    RESOLVED,
    NEED_MANUAL_REVIEW
}

public record ReconciliationDifference(
        String differenceId,
        String businessKey,
        ReconciliationDifferenceType type,
        DifferenceStatus status,
        String evidence,
        Instant detectedAt) {
}
```

对账任务只负责生成差异单，不建议直接在扫描逻辑里修改核心业务状态：

```java
public final class PaymentReconciliationJob {

    private final ChannelBillReader channelBillReader;
    private final PaymentRepository paymentRepository;
    private final DifferenceRepository differenceRepository;

    public void reconcile(LocalDate billDate) {
        for (ChannelBill bill : channelBillReader.read(billDate)) {
            Payment payment = paymentRepository.findByChannelTradeNo(bill.channelTradeNo());
            if (payment == null || !payment.sameAmountAs(bill)) {
                differenceRepository.save(ReconciliationDifferenceFactory.from(bill, payment));
            }
        }
    }
}
```

## 深度增强：面试高分表达

```text
对账和补偿要拆开。对账是只读比对和生成证据，发现渠道、本地、订单、库存之间的差异；
补偿是根据差异类型做幂等修复，比如推进订单状态、释放库存、重发事件或退款。
我不会让对账扫描直接改核心表，因为这样难审计、难回滚，也容易把扫描任务做成高风险任务。
```

## 专家级完整回答

```text
对账和补偿是两个阶段。对账负责发现多系统之间的数据差异，例如支付成功但订单未更新；
补偿负责根据差异修复业务状态，例如重发事件、更新状态、释放库存或发起退款。

生产系统要把两者做成闭环，并保证补偿幂等、可审计、可追踪。
```

## 回答评分点

高分答案应该覆盖：

- 对账是发现差异。
- 补偿是修复差异。
- 对账不一定直接改数据。
- 补偿要幂等和审计。
- 两者共同形成一致性闭环。
## 深度完善：专项验收清单

围绕「对账和补偿有什么区别？」，这道题原本已经有专题深度增强；这里再补一层面向生产和 L6 面试的验收口径。
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
