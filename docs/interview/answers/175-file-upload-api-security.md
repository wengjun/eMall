# 175 文件上传接口如何做安全限制？

[返回按分类学习面试题](../README.md)

## 题目

文件上传接口如何做安全限制？

## 先给面试官的短答案

文件上传是高风险入口，不能只检查扩展名。生产系统要限制文件大小、类型、数量、频率、存储路径、访问权限，
并做内容嗅探、病毒扫描、异步处理和审计。

上传文件不要直接落到业务服务本地磁盘，更不能原样公开访问。

## 常见风险

风险包括：

- 上传可执行脚本。
- 伪造扩展名和 MIME 类型。
- 超大文件耗尽带宽和磁盘。
- 压缩包炸弹。
- 图片携带恶意内容。
- 文件名路径穿越。
- 私有文件被公开访问。
- 重复上传导致存储成本失控。

只依赖前端校验没有任何安全意义。

## 服务端限制

服务端应做：

- 限制最大文件大小。
- 限制单次上传数量。
- 限制用户上传频率。
- 使用白名单类型。
- 校验文件魔数。
- 重命名文件，禁止使用原始文件名作为路径。
- 存储到对象存储。
- 私有文件使用签名 URL。
- 对高风险文件做病毒扫描。
- 记录上传审计日志。

业务服务只保存文件元数据和对象存储地址。

## 处理流程

典型流程：

```text
客户端申请上传凭证
-> 服务端校验权限和配额
-> 返回对象存储预签名地址
-> 客户端直传对象存储
-> 对象存储回调或事件通知
-> 文件扫描和元数据入库
-> 业务状态变为可用
```

这样可以减少业务服务带宽压力。

## 在 eMall 项目中怎么讲？

商家上传商品图片时，商品服务不直接接收大文件。

它先生成上传凭证，文件进入对象存储后由媒体处理任务做格式校验、图片压缩、风险扫描和 CDN 刷新，
最后商品只引用审核通过的图片资源。

## 深度增强：Spring 服务治理图

![Spring 微服务调用栈和治理边界](../assets/spring-service-stack.svg)

Spring 题要从框架机制讲到业务边界。Controller 负责协议适配，Service 负责业务事务，Repository 负责数据访问；
事务、AOP、校验、错误码、配置和观测都是为了让微服务在复杂调用中保持稳定。

## 深度增强：Java 17 分层示例

```java
record CreateOrderCommand(long userId, long skuId, int quantity) {
}

record CreateOrderResult(long orderId, String status) {
}

interface OrderApplicationService {
    CreateOrderResult create(CreateOrderCommand command);
}

final class OrderControllerAdapter {
    private final OrderApplicationService service;

    OrderControllerAdapter(OrderApplicationService service) {
        this.service = service;
    }

    CreateOrderResult submit(CreateOrderCommand command) {
        return service.create(command);
    }
}
```

这个示例表达分层边界：接口层不堆业务逻辑，业务层不依赖 Web 协议，命令和结果对象形成稳定契约。

## 深度增强：生产边界

框架默认值不能替代设计。事务传播、异常回滚、异步线程池、连接池、序列化、超时和重试都要显式治理。
尤其在订单、支付、库存链路中，要避免长事务、隐式重试和跨服务事务误用。

## 深度增强：面试高分表达

我会先解释框架原理，再说明在电商系统里怎么落地。高分回答要能把自动配置、AOP、事务、MVC、WebFlux、
校验和错误处理，连接到可维护性、可观测性、稳定性和故障恢复。

## 专家级完整回答

```text
文件上传要按安全入口治理。不能只看扩展名，要限制大小、数量、频率和类型，并用文件魔数校验真实格式。
文件名要重命名，禁止路径穿越，文件应存对象存储，私有文件通过签名 URL 访问。

大文件最好用预签名直传，上传完成后触发异步扫描、转码、压缩和元数据入库。业务服务只保存元数据和引用地址。
```

## 回答评分点

高分答案应该覆盖：

- 扩展名和 MIME 类型不可信。
- 要限制大小、数量、频率和类型。
- 使用对象存储和预签名直传。
- 私有文件使用签名 URL。
- 扫描、审计和异步处理很重要。
