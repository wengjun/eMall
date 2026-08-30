# 039 为什么大型项目要限制循环依赖？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

循环依赖说明模块边界不清晰。A 依赖 B，B 又依赖 A，会导致构建复杂、测试困难、
变更半径扩大、模块无法独立演进，长期会形成“大泥球”。

大型项目必须限制循环依赖，保证依赖方向清晰。

## 从零基础理解

正常依赖：

```text
order -> common
payment -> common
```

循环依赖：

```text
order -> payment
payment -> order
```

如果订单和支付互相依赖内部实现，任何一个模块变化都可能影响另一个模块。

## 循环依赖的危害

### 构建复杂

模块无法明确先后编译。

### 测试困难

想测试订单模块，却必须带上支付模块。

### 变更半径扩大

改支付逻辑可能导致订单编译失败。

### 无法独立部署

微服务拆分失去意义。

### 边界腐化

循环依赖通常说明数据所有权和职责没有拆清。

## 如何解决循环依赖？

### 抽取公共抽象

如果 A 和 B 都需要某个基础能力，抽到 `common`。

但不要把具体业务抽进 common。

### 使用接口反转依赖

定义接口，让实现依赖接口。

### 使用事件解耦

支付成功后不一定直接依赖订单内部实现，可以发事件或调用订单公开 API。

### 重新划分边界

如果两个模块总是互相依赖，可能它们本来就属于同一个边界，或者职责拆错了。

## 电商系统实践

订单、库存、支付之间不能互相依赖内部实现。

推荐：

```text
payment -> OrderClient -> order API
order -> InventoryClient -> inventory API
```

或通过事件协作：

```text
payment publishes PAYMENT_SUCCEEDED
order consumes or handles confirmation
```

不要：

```text
payment 直接引用 order.repository
order 直接修改 payment table
```
