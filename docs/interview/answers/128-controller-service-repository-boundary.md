# 128 Controller、Service、Repository 的职责边界是什么？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

Controller 负责协议适配和参数校验，Service 负责编排业务用例和事务边界，Repository 负责数据访问和持久化抽象。
Controller 不应写业务规则，Repository 不应编排业务流程，Service 不应堆成上帝类。

清晰边界能提高可测试性、可维护性和模块演进能力。

## Controller

Controller 职责：

- 接收 HTTP 请求。
- 解析参数。
- 基础格式校验。
- 调用应用服务。
- 转换响应。
- 处理协议状态码。

不应该：

- 写复杂业务逻辑。
- 直接操作数据库。
- 调用多个 Repository 编排事务。

## Service

Service 职责：

- 表达业务用例。
- 编排领域对象和外部依赖。
- 控制事务边界。
- 做权限和业务校验。
- 发布领域事件或应用事件。

Service 应该面向业务动作，例如 `createOrder`、`payOrder`、`cancelOrder`。

## Repository

Repository 职责：

- 封装数据访问。
- 隐藏 SQL 或 ORM 细节。
- 提供领域语义查询。
- 保存和加载聚合或实体。

不应该：

- 调用远程服务。
- 编排业务流程。
- 处理 HTTP 参数。

## DTO 和领域对象

Controller 接收的是 request DTO。

Service 使用命令对象或领域对象。

Repository 返回实体或持久化对象。

不要让前端 DTO 穿透到所有层，否则协议变化会污染业务层。

## 在 eMall 项目中怎么讲？

订单创建：

- Controller 接收 `CreateOrderRequest`。
- Service 执行创建订单用例，校验用户、库存、价格和幂等。
- Repository 保存订单和查询订单。

如果 Controller 直接扣库存、算价格和写订单，就会变成胖 Controller，难测试也难复用。
