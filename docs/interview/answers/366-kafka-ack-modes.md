# 366 ack=0、ack=1、ack=all 有什么区别？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

`ack=0` 表示 Producer 不等待 Broker 确认，吞吐高但最容易丢消息。`ack=1` 表示 leader 写入成功
就确认，leader 宕机时仍可能丢。`ack=all` 表示 ISR 中足够副本写入后才确认，可靠性最高但延迟更高。

核心交易事件通常选择 `ack=all` 配合 `min.insync.replicas`。

## ack=0

特点：

- 不等待服务端确认。
- 延迟最低。
- 吞吐高。
- 可能丢消息且生产者不知道。

适合低价值日志，不适合核心业务。

## ack=1

特点：

- leader 写入后确认。
- 性能和可靠性折中。
- follower 还没复制时 leader 宕机可能丢。

适合部分可容忍少量丢失的场景。

## ack=all

特点：

- 等待 ISR 满足确认条件。
- 可靠性最高。
- 延迟更高。
- 需要配置 `min.insync.replicas`。

它是核心事件的常用选择。

## 在 eMall 项目中怎么讲？

eMall 的 `order-created`、`payment-succeeded`、`refund-created` 这类核心事件应使用 `ack=all`。

用户行为日志、曝光日志可以根据成本和重要性选择较低可靠性配置，但要明确丢失影响。
