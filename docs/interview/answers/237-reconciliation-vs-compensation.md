# 237 对账和补偿有什么区别？

[返回按分类学习面试题](../README.md)

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

对账还应独立于在线主链路运行，用来发现没有异常日志和告警的静默差异。需要监控对账覆盖率、差异率、
未决金额、自动修复成功率和最长未处理时间，避免任务显示成功但实际漏掉数据。

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
