# 容量验证说明

[文档索引](README.md) | [分布式压测部署](../ops/loadtest/README.md) | [容量报告模板](../ops/loadtest/p2-capacity-baseline-template.md)

工程按照 10 亿注册用户、1 亿 DAU、100 万峰值在线并发设计，但这些数字不是代码属性。容量结论必须由固定 Git
版本、确定的数据规模、单 Cell 实测结果、各层饱和指标和多 Cell 扩展效率共同证明。开发机或单 JVM 结果只能作为
功能基线，不能标记为生产容量已验证。

## 工具架构

`loadtest` 是 Java 17 分布式压测工具，包含三种角色：

- `standalone`：单进程调试，执行请求并立即生成单次容量报告。
- `worker`：按 `workerIndex/workerCount` 拆分全局 QPS 和请求序号，可在不同机器或 Kubernetes Pod 运行。
- `coordinator`：读取共享目录中的 worker JSON，合并 HdrHistogram，并生成容量报告和证据套件结论。

worker 使用 `Semaphore` 限制真实在途 HTTP 请求，使用 `Phaser` 等待已发送请求结束。请求完成后立即释放许可，不保存
全量 `CompletableFuture`、结果或延迟列表。延迟通过 HdrHistogram `Recorder` 流式记录，因此进程内存由在途上限和
固定精度直方图决定，不随请求总量或浸泡时间线性增长。

每个 worker 的第 `n` 个本地请求对应全局序号：

```text
globalSequence = (localSequence - 1) * workerCount + workerIndex + 1
```

该序号用于生成请求 ID、用户、SKU 和设备，保证不同 worker 不重复发送同一个逻辑请求。全局 QPS 也按余数公平拆分，
所有 worker 的本地 QPS 之和严格等于目标 QPS。

## 构建与本地基线

打包后的 JAR 已包含 Jackson 和 HdrHistogram，可直接运行：

```powershell
mvn -pl loadtest -am package -DskipTests
java -jar loadtest/target/loadtest-0.1.0-SNAPSHOT-all.jar http://localhost:8080 100 60 200 checkout
```

五个位置参数依次是 `baseUrl`、全局 `ratePerSecond`、`durationSeconds`、单 worker
`maxInflight`、`scenario`。本地默认角色是 `standalone`，报告写入 `target/loadtest-reports`。

生产压测不能让 worker 自动造数。数据应在压测前通过专用数据准备任务批量生成，并设置：

```powershell
$env:EMALL_LOAD_BOOTSTRAP_DATA='false'
$env:EMALL_LOAD_USER_CARDINALITY='1000000'
$env:EMALL_LOAD_SKU_CARDINALITY='100000'
$env:EMALL_LOAD_HOT_SKU_PERCENT='20'
```

## 流量和数据模型

业务场景包括：

- `checkout`：普通下单。
- `read-heavy`：商品、搜索、库存和价格读取。
- `hot-sku`：热点 SKU 下单。
- `payment-callbacks`：支付创建及回调。
- `mq-backlog`：商品变更及异步消费积压。
- `flash-sale-hotspot`：秒杀热点排队。
- `production-mix`：按权重混合以上场景。

默认混合比例为：

```text
read-heavy:65,checkout:15,hot-sku:10,payment-callbacks:5,mq-backlog:3,flash-sale-hotspot:2
```

使用 `EMALL_LOAD_TRAFFIC_MIX` 修改比例。`EMALL_LOAD_USER_CARDINALITY`、`EMALL_LOAD_SKU_CARDINALITY` 和
`EMALL_LOAD_HOT_SKU_PERCENT` 控制活动数据基数与热点流量。报告同时记录实际数据集总规模，避免用十个用户和一个 SKU
得出的缓存命中率冒充生产结果。

支付回调不能使用对外 API 返回的脱敏渠道交易号。数据准备任务应预建待回调支付及测试支付渠道记录，并按
`EMALL_LOAD_PAYMENT_ID_BASE + globalSequence` 和 `EMALL_LOAD_PAYMENT_TRADE_NO_PREFIX + globalSequence`
生成确定性标识。压测端通过 Secret 中的 `EMALL_LOAD_PAYMENT_CALLBACK_SECRET` 生成时间戳、nonce 和 HMAC-SHA256
签名；密钥不足 32 字节时工具拒绝启动支付回调场景。

多用户下单和秒杀不能共用一个 JWT。每个 worker 使用独立的 `userId,token` CSV，路径通过
`EMALL_LOAD_IDENTITY_FIXTURE_FILE=/fixtures/worker-{worker}.csv` 配置；文件逐行读取，不会把百万令牌驻留内存或写入
报告。预生产多用户写场景未使用逐用户凭据时，coordinator 会拒绝把该轮标记为有效证据。

## 负载模式

通过 `EMALL_LOAD_PATTERN` 选择：

- `constant`：固定负载，用于单 Cell 基线和重复性验证。
- `step`：25%、50%、75%、100% 阶梯升压。
- `spike`：基线、瞬时尖峰、尖峰后观察。
- `soak`：长时间稳定流量，用于发现内存泄漏、连接泄漏和缓慢积压。
- `fault-recovery`：基线、故障窗口、恢复窗口；外部混沌平台必须按同一 run ID 注入故障。
- `breakpoint`：20% 到 100% 分段升压，达到错误率或 P95 阈值后停止继续放量。

`fault-recovery` 只负责标记故障窗口，不在压测进程内执行任意系统命令。使用 Chaos Mesh、云故障演练平台或工程的
`chaos` 模块注入故障，并通过 `EMALL_LOAD_FAULT_EXPERIMENT` 记录实验 ID。恢复阶段必须重新满足错误率和 P95 阈值。

## 分布式执行

生产执行使用 [ops/loadtest](../ops/loadtest/README.md) 中独立的 Helm Chart。worker 以 Kubernetes Indexed Job
运行，Pod 按主机和可用区分散，并通过 RWX 卷写入报告。多个模式和重复轮次必须使用不同 run ID，但使用同一 Git
commit、预生产环境和共享报告卷。

手动运行 worker 时至少设置：

```powershell
$env:EMALL_LOAD_ROLE='worker'
$env:EMALL_LOAD_RUN_ID='checkout-constant-001'
$env:EMALL_LOAD_WORKER_INDEX='0'
$env:EMALL_LOAD_WORKER_COUNT='8'
$env:EMALL_LOAD_BOOTSTRAP_DATA='false'
java -jar loadtest/target/loadtest-0.1.0-SNAPSHOT-all.jar
```

所有 worker 完成后，再运行 coordinator。`EMALL_LOAD_EVIDENCE_RUN_IDS` 是逗号分隔的 run ID，
`EMALL_LOAD_REQUIRE_VERIFIED_EVIDENCE=true` 会在证据不完整时以失败状态退出，适合作为发布门禁。

## 饱和指标

压测窗口结束后，从 Prometheus、数据库和消息平台导出一个扁平 JSON 文件。利用率统一为 `0.0-1.0`，Kafka lag 使用
消息数。以下字段是可验证报告的最小集合：

```json
{
    "gateway.cpu.utilization": 0.62,
    "application.cpu.utilization": 0.71,
    "mysql.cpu.utilization": 0.68,
    "mysql.connection.utilization": 0.54,
    "redis.cpu.utilization": 0.48,
    "kafka.consumer.lag": 1200,
    "generator.network.utilization": 0.43
}
```

实际报告还应增加 MySQL 慢 SQL、锁等待、复制延迟，Redis 内存、热 key、命中率，Kafka 生产/消费速率和 DLT，
JVM GC/堆/线程池队列，Outbox、补偿、对账和 Saga 积压。coordinator 通过
`EMALL_LOAD_SATURATION_FILE` 读取文件，缺少最小字段时只能生成 `BASELINE_ONLY`。

## 报告状态

- `INVALID`：worker 不完整、索引重复、压测端背压拒绝、生成器 CPU/堆或调度滞后成为瓶颈。
- `BASELINE_ONLY`：请求可执行，但缺少预生产、完整 Git SHA、资源清单、数据规模、饱和指标或场景验收证据。
- `PREPRODUCTION_RUN_ELIGIBLE`：单轮预生产测试通过，可以进入重复证据套件。
- `VERIFIED`：六种负载模式均达到配置的重复次数、使用同一 Git 和环境、基线波动不超过 10%，且容量模型达到目标。

工具不会因为配置了 `1000000` 就输出 `VERIFIED`。默认本地运行必然是 `UNVERIFIED`，这是防止错误容量声明的保护机制。

## 单 Cell 与水平扩展模型

报告使用以下公式：

```text
safeCellQps = measuredQps * headroom
projectedQps = safeCellQps * cells * scalingEfficiency
projectedPeakConcurrency = projectedQps * sessionThinkSeconds
```

`measuredQps` 必须来自压测端不饱和、SLO 达标的单 Cell 实测；`headroom` 默认 70%；`scalingEfficiency` 必须通过
2、4、8 Cell 实测校准，不能固定假设线性扩展。最后一个公式来自 Little's Law，表示给定用户平均思考时间时可支撑的
在线会话数，不等同于同时在途 HTTP 请求数。百万在线并发与百万同时下单是完全不同的容量目标，报告必须写明口径。

## 完成判定

一次可信容量验收至少包含：环境名称、完整 Git SHA、服务实例和资源、数据集规模、场景比例、目标和实测 QPS、峰值
在途、P50/P95/P99、错误率、429/5xx/超时、生成器资源、各层饱和指标、单 Cell 安全容量、扩展效率和故障恢复结果。
只有 `capacity-evidence.json` 为 `VERIFIED` 时，才能对对应 Git 版本和部署规格声明目标容量已验证。
