# 分布式容量压测部署

[项目首页](../../README.md) | [运维索引](../README.md) | [容量验证说明](../../docs/capacity-verification.md)

本目录是 `loadtest` 的独立 Helm Chart 和容量人工复核模板。在线服务仍只由 `ops/helm/emall` 部署；压测 Job
不会混入线上服务 Chart。

## 前置条件

- 将 `loadtest` 胖 JAR 构建成不可变镜像，并使用 Git SHA 或镜像 digest，不使用浮动标签。
- 在预生产准备与报告声明一致的数据规模，分布式 worker 必须关闭自动造数。
- `production-mix` 和 `payment-callbacks` 需要预建确定性支付回调数据，并在 `auth.existingSecret` 的
  `payment-callback-secret` 键保存与支付服务一致的 HMAC 密钥。
- 多用户下单和秒杀使用按 worker 分片的 `userId,token` CSV，通过 `identityFixtures.existingClaim` 只读挂载；
  文件按 `worker-{index}.csv` 命名并按请求顺序流式读取，令牌不会写入报告。
- 提供支持 `ReadWriteMany` 的报告 PVC；所有轮次和 coordinator 必须挂载同一个卷。
- 压测机与被测集群分离，并监控压测机 CPU、堆、网络和调度滞后。

构建胖 JAR 和镜像：

```powershell
mvn -pl loadtest -am package -DskipTests
docker build -f Dockerfile.loadtest -t registry.example.com/emall/loadtest:$env:GIT_COMMIT .
```

## 启动 worker

为每个模式和重复轮次使用唯一 run ID，并保存一份 values 覆盖文件：

```powershell
helm upgrade --install capacity-constant-001 ops/loadtest `
  --namespace emall-loadtest --create-namespace `
  --set mode=worker `
  --set runId=checkout-constant-001 `
  --set image.repository=registry.example.com/emall/loadtest `
  --set image.tag=$env:GIT_COMMIT `
  --set evidence.gitCommit=$env:GIT_COMMIT `
  --set evidence.scope=preproduction `
  --set reports.existingClaim=emall-capacity-reports `
  --set auth.existingSecret=emall-loadtest-auth `
  --set identityFixtures.existingClaim=emall-loadtest-identities
```

等待 Indexed Job 的全部索引完成：

```powershell
kubectl wait --for=condition=complete job/capacity-constant-001-checkout-constant-001-workers `
  --namespace emall-loadtest --timeout=30m
```

分别执行 `constant`、`step`、`spike`、`soak`、`fault-recovery` 和 `breakpoint`，默认每种至少三轮。
`fault-recovery` 必须通过 `evidence.faultExperiment` 写入真实混沌实验 ID。

## 收集饱和指标

worker 结束后，根据准确的开始/结束时间从 Prometheus 和各中间件导出指标，写入共享卷的
`/reports/saturation-metrics.json`。字段与单位见[容量验证说明](../../docs/capacity-verification.md#饱和指标)。

## 启动 coordinator

coordinator 必须在所有 worker 和指标采集结束后单独启动。将所有 run ID 以逗号连接：

```powershell
helm upgrade --install capacity-evidence ops/loadtest `
  --namespace emall-loadtest `
  --set mode=coordinator `
  --set runId=evidence-001 `
  --set reports.existingClaim=emall-capacity-reports `
  --set saturationMetrics.enabled=true `
  --set evidence.runIds='checkout-constant-001,checkout-step-001' `
  --set evidence.requireVerified=true
```

输出文件包括每个 worker 的 JSON、每轮 `.capacity.json/.capacity.md`，以及总门禁
`capacity-evidence.json/.md`。证据不完整时 coordinator 退出失败，不得绕过后手工把状态改成 `VERIFIED`。

## Chart 校验

```powershell
helm lint ops/loadtest --strict
helm template capacity ops/loadtest --set mode=worker
helm template capacity ops/loadtest --set mode=coordinator
```
