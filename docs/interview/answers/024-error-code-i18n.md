# 024 错误码如何兼容多语言和多端？

[返回按分类学习面试题](../README.md)

## 题目

错误码如何兼容多语言和多端？

## 先给面试官的短答案

错误码应该是稳定的机器可读标识，展示文案应该和错误码解耦。
多语言、多端场景下，服务端返回稳定 `code`、必要参数和 traceId；
客户端或国际化服务根据 locale 把 code 映射成对应语言文案。

## 为什么不能只返回 message？

如果服务端直接返回中文：

```json
{
  "message": "库存不足"
}
```

英文端、移动端、开放平台都无法稳定处理。

如果后端把文案改成：

```text
商品库存不够了
```

前端如果依赖 message 判断逻辑，就会出错。

## 推荐响应结构

```json
{
  "success": false,
  "code": "INSUFFICIENT_STOCK",
  "message": "Insufficient stock",
  "args": {
    "skuId": "10001"
  },
  "traceId": "..."
}
```

其中：

- `code` 稳定。
- `message` 是默认文案。
- `args` 提供文案模板参数。
- `traceId` 用于排障。

## 多语言方案

### 客户端映射

客户端维护：

```text
INSUFFICIENT_STOCK -> 库存不足
INSUFFICIENT_STOCK -> Insufficient stock
```

优点：

- 客户端展示灵活。
- 离线也能展示。

缺点：

- 多端文案要同步。
- 新错误码客户端不升级可能不认识。

### 服务端按 locale 返回

请求带：

```text
Accept-Language: zh-CN
```

服务端返回对应语言。

优点：

- 文案统一。

缺点：

- 服务端国际化复杂度增加。
- 多端展示差异不好控制。

### 混合方案

服务端返回 code 和默认 message，客户端优先本地映射，不认识时展示默认 message。

这是比较稳妥的方案。

## 多端兼容

多端包括：

- Web。
- iOS。
- Android。
- 小程序。
- 开放平台调用方。
- 内部运营后台。

要保证：

- 老客户端不认识新 code 时有默认展示。
- code 不随便删除或改名。
- 需要客户端特殊处理的错误码进入契约文档。
- 开放平台错误码要长期稳定。

## 在 eMall 项目中怎么讲？

eMall 可以返回统一 `ApiResponse`：

```json
{
  "code": "ORDER_STATUS_CONFLICT",
  "message": "Order status conflict",
  "traceId": "..."
}
```

前端按 code 映射中文学习文案，开放平台按 code 做程序判断。

系统错误统一返回通用文案，不暴露堆栈。

## 专家级完整回答

```text
错误码要作为稳定机器契约，文案要和 code 解耦。
服务端返回 code、默认 message、args 和 traceId。客户端或国际化服务根据 locale 映射文案。
老客户端遇到未知 code 时要有兜底展示，开放平台错误码不能随意改名或删除。
监控和日志聚合也应该按 code，而不是按翻译后的 message。
```

## 回答评分点

高分答案应该覆盖：

- code 和 message 分离。
- 能说明客户端映射、服务端映射、混合方案。
- 能考虑老客户端未知错误码。
- 能考虑开放平台稳定契约。
- 能说明监控按 code 聚合。
