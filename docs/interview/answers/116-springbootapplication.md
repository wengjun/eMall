# 116 @SpringBootApplication 包含哪些注解？

[返回按分类学习面试题](../README.md)

## 一张对应表足够

| 组成 | 用途 |
| --- | --- |
| @SpringBootConfiguration | 标记主配置，本质上基于 @Configuration |
| @EnableAutoConfiguration | 根据 classpath、Bean 和属性导入自动配置 |
| @ComponentScan | 扫描启动类所在包及其子包中的组件 |

```java
package example.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OrderApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }
}
```

`example.order` 下的组件默认可被扫描，旁边的 `example.shared` 不会自动包含。
需要显式 import 或指定扫描边界，不要把启动类放在默认包导致全 classpath 扫描。

`scanBasePackages` 调整的是组件扫描，不能想当然地代替 MyBatis 的 @MapperScan 或实体扫描。
自动配置的加载机制看 [115](115-spring-boot-auto-configuration.md)，本题不用重复背一遍。
