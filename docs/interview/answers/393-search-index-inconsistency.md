# 393 搜索索引和商品库不一致怎么办？

[返回按分类学习面试题](../README.md)

## 先给面试官的短答案

搜索索引和商品库不一致是最终一致读模型的常见问题。处理思路是先明确商品库是事实来源，然后用
事件重试、死信回放、定时对账、单文档重建和全量重建修复索引。

交易正确性不能依赖搜索索引。

## 不一致类型

类型：

- 商品库已更新，索引未更新。
- 商品库已下架，索引仍可搜。
- 索引文档字段缺失。
- 价格或库存展示滞后。
- 索引中存在脏文档。

不同字段的不一致影响不同。

## 处理方式

方式：

- 消费失败重试。
- 死信修复后回放。
- 按商品 ID 单独重建。
- 定时抽样对账。
- 增量事件加全量校准。
- 查询详情页时回源校验。

核心是可发现、可修复、可重建。

## 用户侧保护

保护：

- 搜索结果只做导流。
- 商品详情页校验上下架。
- 下单校验价格和库存。
- 高风险字段不完全信任索引。

这样索引短暂不一致不会变成交易错误。

## 电商系统实践

大型电商系统搜索结果中如果出现已下架商品，商品详情服务应返回已下架，并触发单商品索引修复。

同时搜索同步任务记录死信和 lag，定时对比商品库 `updated_at` 与索引 `version`，发现落后后重建。

## 深度增强：搜索读模型图

![搜索索引作为可重建读模型](../assets/search-read-model.svg)

搜索索引必须被当成可重建读模型，而不是事实来源。商品上下架、价格和库存这类影响交易正确性的字段，
在详情页和下单链路必须回源校验。

## 深度增强：版本化索引文档

```java
public record ProductSearchDocument(
        long skuId,
        String title,
        String brand,
        String category,
        boolean onShelf,
        long sourceVersion,
        Instant sourceUpdatedAt) {
}
```

同步事件要带版本，避免旧事件覆盖新索引：

```java
public final class ProductIndexUpdater {

    private final ProductRepository productRepository;
    private final SearchIndexGateway searchIndexGateway;

    public void update(ProductChangedEvent event) {
        Product product = productRepository.findBySkuId(event.skuId());
        ProductSearchDocument current = searchIndexGateway.get(event.skuId());
        if (current != null && current.sourceVersion() >= product.version()) {
            return;
        }
        searchIndexGateway.upsert(ProductSearchDocumentFactory.from(product));
    }
}
```

## 深度增强：修复路径

- 单商品修复：详情页发现索引异常时，触发单 SKU 重建。
- 死信回放：修复 schema 或数据后回放失败事件。
- 定时对账：比较商品库 `updated_at/version` 和索引文档版本。
- 全量重建：mapping 变更或大面积不一致时重建新索引。
