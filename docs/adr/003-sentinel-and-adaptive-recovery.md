# ADR 003：Sentinel 动态治理和平滑恢复

[文档索引](../README.md) | [容量验证说明](../capacity-verification.md)

## 背景

大促流量下，下游服务可能出现慢调用、错误率升高、线程池耗尽或恢复后瞬时被打满。只靠固定代码阈值，
生产运行时无法快速调整限流、熔断和降级策略。

## 决策

核心入口使用 Sentinel 规则，配置优先从 Nacos 动态刷新，本地默认值兜底；库存下游调用使用自适应恢复控制器，
避免熔断恢复瞬间把下游再次打满。

- 网关使用 Redis RateLimiter，按用户、IP、设备、渠道、SKU 和活动维度生成限流 key。
- 订单和支付服务使用 Sentinel 资源保护核心方法。
- Nacos 配置变更后触发规则重载。
- Nacos 不可用时继续使用本地默认配置。
- 下游恢复阶段按成功率和恢复窗口逐步放量。

## 备选方案

- 只用网关限流：能保护入口，但保护不了服务内部热点和下游抖动。
- 只用 Resilience4j：适合通用熔断限流，但国内面试和生产栈里 Sentinel/Nacos 更常见。
- 手工重启改阈值：响应慢，不适合线上事故处理。

## 优点

- 运行时可以调整阈值，不需要重新发布。
- 熔断、降级、限流和恢复策略统一可解释。
- 入口限流和服务级保护可以分层生效。
- 适合国内互联网公司的技术栈表达。

## 代价

- Sentinel 规则必须有配置治理和变更审计。
- 动态配置错误可能造成误限流或保护不足。
- 需要通过压测确定初始阈值，而不是凭经验拍脑袋。

## 落地位置

- `gateway/src/main/java/com/emall/gateway/config/RateLimitConfig.java`
- `order/src/main/java/com/emall/order/config/OrderSentinelRuleConfiguration.java`
- `payment/src/main/java/com/emall/payment/config/PaymentSentinelRuleConfiguration.java`
- `governance/src/main/java/com/emall/governance/recovery/AdaptiveRecoveryController.java`
- `docs/capacity-verification.md`
