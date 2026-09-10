# 510 Maven 如何管理和排查 Java 依赖版本？

[返回按分类学习面试题](../README.md)

## 只记 Maven 机制

dependencyManagement 管理版本和相关约束，**不会自动把依赖加入模块**；
模块仍需声明 dependency。导入 BOM 同理。
pluginManagement 管理插件默认配置，也不表示任意插件都会自动执行。

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>${spring.boot.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

这里版本属性由父 POM 定义。继承父 POM 和导入 BOM 不完全等价，尤其不要假设 BOM 也管理构建插件。

## 三条排查命令

```text
mvn dependency:tree
mvn help:effective-pom
mvn help:active-profiles
```

依赖树看传递依赖来源，effective-pom 看合并后的实际模型，active-profiles 看当前启用了什么配置。
Maven 解析冲突不是简单“永远取最高版本”；依赖管理与依赖路径都会影响最终选择。

NoSuchMethodError 常是编译期与运行期实际加载版本不一致，先查依赖树、打包内容和 ClassLoader，
不要直接归因于业务方法写错。多模块复用看 [149](149-reuse-common-config-in-multi-module.md)。
