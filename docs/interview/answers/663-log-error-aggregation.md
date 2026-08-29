# 663 手写 Java 代码解析并聚合日志错误码。

[返回按分类学习面试题](../README.md)

## 实现、测试和生产化边界

### 参考实现或关键片段

```java
import java.util.HashMap;
import java.util.List;
import java.util.Map;

static Map<String, Long> countErrorCodes(List<String> lines) {
    Map<String, Long> result = new HashMap<>();
    for (String line : lines) {
        int index = line.indexOf("errorCode=");
        if (index < 0) {
            continue;
        }
        String code = line.substring(index + "errorCode=".length()).split("\\s+")[0];
        result.merge(code, 1L, Long::sum);
    }
    return result;
}
```

### 必测用例

- 覆盖合法日志、缺失错误码、畸形行、不同编码以及同一错误码多次出现。
- 输入为空和文件很大时仍能流式处理，不把全部日志读入内存。
- 错误码种类异常增多时要有基数或内存上限。

### 生产化差异

- 生产日志聚合应交给 ELK 或可观测平台，并使用结构化字段而不是正则解析自由文本。
- 本地工具还需处理轮转、压缩文件、背压、脱敏和读取权限。
