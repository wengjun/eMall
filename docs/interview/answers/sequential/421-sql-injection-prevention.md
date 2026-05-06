# 421 SQL 注入如何防止？

[返回逐题精讲目录](README.md) | [返回答案手册](../README.md)

完成标记：已完成

深度完善标记：已完成

## 题目

SQL 注入如何防止？

## 先给面试官的短答案

SQL 注入的根因是把用户输入当成 SQL 代码拼接执行。防护核心是使用参数化查询、预编译语句、ORM
安全绑定、输入校验、最小数据库权限和安全审计。

不要靠简单转义或黑名单作为主要防线。

## 主要防线

防线包括：

- 使用 PreparedStatement。
- MyBatis 使用 `#{}` 而不是 `${}`。
- 动态排序字段使用白名单。
- 输入格式校验。
- 数据库账号最小权限。
- 错误信息不暴露 SQL 细节。

参数绑定是最关键的防线。

## 危险写法

危险点：

- 字符串拼接 SQL。
- MyBatis `${}` 拼接用户输入。
- 动态表名和字段名不校验。
- order by 字段直接来自请求。
- 拼接 in 条件不做参数化。

这些都可能变成注入入口。

## 纵深防御

还需要：

- 代码扫描。
- SQL 审计。
- WAF 辅助拦截。
- 慢 SQL 和异常 SQL 告警。
- 数据库权限隔离。
- 敏感操作审计。

安全不能只靠某一层。

## 在 eMall 项目中怎么讲？

eMall 搜索或订单后台查询如果支持按字段排序，排序字段必须是服务端白名单，例如 `created_at`、
`amount` 和 `status`。

用户输入的订单号、手机号 hash 和状态条件都必须通过参数绑定传入 SQL。

## 深度增强：开放平台安全图

![开放平台 API 安全链路](../../assets/openapi-security.svg)

SQL 注入不是只发生在登录框。开放平台、商家后台、运营查询、数据导出和搜索筛选都可能把外部输入
带入数据库。安全设计要把“可变值”和“SQL 结构”分开：值用参数绑定，结构只能来自服务端白名单。

## 深度增强：Java 17 白名单排序示例

```java
import java.util.Map;
import java.util.Optional;

record OrderQuery(String status, String orderNo, String sortBy, boolean desc) {
}

final class OrderSqlBuilder {
    private static final Map<String, String> SORT_COLUMNS = Map.of(
            "createdAt", "created_at",
            "amount", "pay_amount",
            "status", "order_status");

    String buildOrderBy(OrderQuery query) {
        String column = Optional.ofNullable(SORT_COLUMNS.get(query.sortBy()))
                .orElse("created_at");
        String direction = query.desc() ? "DESC" : "ASC";
        return " ORDER BY " + column + " " + direction;
    }
}
```

MyBatis 中普通条件应该使用 `#{}`：

```xml
<select id="selectOrders" resultType="OrderEntity">
    SELECT order_id, user_id, order_status, pay_amount
    FROM orders
    WHERE user_id = #{userId}
      AND order_status = #{status}
</select>
```

如果确实需要动态列名或排序，不能直接写 `${sortBy}` 接收用户输入，而是像上面的 Java 代码一样
把外部字段映射到固定 SQL 片段。这样 SQL 结构仍由服务端控制。

## 深度增强：生产边界

参数化查询是第一防线，但不是唯一防线。数据库账号要最小权限，订单查询账号不应该有删表权限；
错误响应不能暴露 SQL；导出、模糊查询和后台组合查询要限制范围，避免注入和拖垮数据库同时发生。

安全扫描可以发现常见拼接，但无法替代代码评审。对于 `${}`、动态表名、动态字段、分表路由和报表 SQL，
应建立专项审查规则，因为这些地方最容易被“业务灵活性”绕开安全边界。

## 深度增强：面试高分表达

我会先说明根因是用户输入改变 SQL 结构，再给出核心解法：值参数化，结构白名单。
然后补充最小权限、审计、错误隐藏和代码扫描，体现纵深防御。对于 MyBatis，我会明确说 `#{}`
用于参数绑定，`${}` 只能用于受控 SQL 片段，不能直接接收外部输入。

## 专家级完整回答

```text
SQL 注入是用户输入被拼接成 SQL 代码执行。核心防护是参数化查询和预编译语句，让输入只作为值，
不能改变 SQL 结构。

在 MyBatis 中应使用 #{} 绑定参数，谨慎使用 ${}。动态字段、排序和表名必须白名单。再配合输入
校验、最小数据库权限、代码扫描和 SQL 审计形成纵深防御。
```

## 回答评分点

高分答案应该覆盖：

- 根因是拼接用户输入。
- 参数化查询是核心。
- MyBatis `#{}` 和 `${}` 区别。
- 动态字段要白名单。
- 最小权限和审计。
## 深度完善：专项验收清单

围绕「SQL 注入如何防止？」，这道题原本已经有专题深度增强；这里再补一层面向生产和 L6 面试的验收口径。
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
