# 532 设计支付渠道对账系统

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

支付渠道对账系统负责核对第三方支付渠道账单和本地支付、退款、手续费、结算数据。核心是下载账单、解析标准化、
按渠道交易号和本地单号匹配，识别渠道成功本地失败、本地成功渠道失败、金额不一致和退款差异，并触发补单或人工处理。

## 核心流程

系统定时从支付渠道下载账单，校验文件完整性和签名。然后解析为统一账单模型，包含渠道交易号、商户单号、金额、
状态、手续费、退款和结算时间。

本地支付流水和退款流水按时间窗口抽取，与渠道账单做双向匹配。

## 差异类型

渠道成功本地失败：通常要补单，更新支付单和订单状态，但要先校验金额和订单合法性。

本地成功渠道失败：可能是本地状态错误，需要冲正或人工处理。

金额不一致：必须进入人工或财务审核，不能自动改金额。

渠道有退款本地无退款，或本地退款渠道未成功，都要生成退款差异单。

## 可靠性设计

账单下载、解析和匹配要幂等，支持重跑。原始账单文件要归档，解析结果和差异结果要可审计。

对账任务要处理渠道账单延迟、跨天交易、部分退款、多币种和手续费。

## 在 eMall 项目中怎么讲？

eMall 的 `payment` 负责渠道流水和退款流水，`finance` 负责对账任务和差异单，`operations` 提供人工处理后台。
支付对账结果可以反向驱动订单补偿。

## 深度增强：支付对账数据流图

![支付渠道对账系统数据流](../assets/payment-reconciliation.svg)

支付对账是资金链路的兜底校验。它不能只比较“支付成功数量”，而要比较渠道交易号、商户单号、金额、
手续费、退款、结算时间和状态。

## 深度增强：统一账单模型

```java
public enum BillDirection {
    PAYMENT,
    REFUND
}

public record ChannelBillEntry(
        String channel,
        String channelTradeNo,
        String merchantOrderNo,
        BillDirection direction,
        BigDecimal amount,
        BigDecimal fee,
        String status,
        Instant settledAt) {
}

public enum PaymentDifferenceType {
    CHANNEL_SUCCESS_LOCAL_MISSING,
    LOCAL_SUCCESS_CHANNEL_MISSING,
    AMOUNT_MISMATCH,
    REFUND_MISMATCH,
    FEE_MISMATCH
}
```

差异生成要保留证据，方便财务、客服和技术一起追踪：

```java
public record PaymentDifference(
        String differenceId,
        PaymentDifferenceType type,
        String merchantOrderNo,
        String channelTradeNo,
        String evidence,
        Instant detectedAt) {
}
```

## 深度增强：自动处理边界

- 渠道成功本地缺失：可自动补支付流水，但必须校验金额、订单和签名证据。
- 本地成功渠道缺失：高风险，通常需要人工审核或渠道查询确认。
- 金额不一致：不能自动覆盖金额，要进入财务审核。
- 退款差异：要关联原支付单、退款单和渠道退款号。
- 原始账单文件必须归档，方便审计和监管。

## 深度增强：面试高分表达

```text
支付渠道对账是资金系统的最后一道防线。我会先下载并验签渠道账单，标准化成统一模型，
再和本地支付、退款、手续费流水做双向匹配。差异单要保留原始账单、匹配证据和处理记录。
渠道成功本地缺失可以补单，但金额不一致必须人工或财务审核，不能自动覆盖。
```
