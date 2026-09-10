# 118 构造函数注入、字段注入、Setter 注入如何取舍？

[返回按分类学习面试题](../README.md)

## 重点是 Spring 如何创建对象

必需依赖用构造函数，允许后续替换的可选依赖才考虑 Setter。
只有一个构造函数时，Spring 不要求再标 @Autowired。

```java
import org.springframework.stereotype.Service;

interface PriceClient {
    long priceInCents(long skuId);
}

@Service
final class PriceService {
    private final PriceClient client;

    PriceService(PriceClient client) {
        this.client = client;
    }

    long quote(long skuId) {
        return client.priceInCents(skuId);
    }
}
```

测试可直接 `new PriceService(skuId -> 100L)`，不需要启动 Spring。
字段注入并非不能工作，只是直接 new 的对象不会自动得到依赖，测试和初始化约束不够显式。

同一接口有多个 Bean 时用 @Qualifier 或 @Primary 解决选择问题，和注入写在字段还是构造器无关。
Lombok @RequiredArgsConstructor 可以减少构造器样板，但生成了什么依赖参数仍需心中有数。
