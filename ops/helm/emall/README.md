# eMall 唯一生产部署 Chart

[项目首页](../../../README.md) | [运维配置索引](../../README.md) | [Kubernetes 辅助资产](../../k8s/README.md)

`ops/helm/emall` 是 38 个在线 Java 服务的唯一生产部署事实源。仓库不提交手工同步的渲染结果，也不再维护另一套服务级
Kubernetes 清单。

Chart 统一生成以下对象：

- Argo Rollout、Service、HPA、PDB、ServiceAccount 和最小权限控制面 RBAC。
- ConfigMap、NetworkPolicy、拓扑分散、Pod 反亲和、安全上下文、探针、优雅关闭和资源规格。
- Gateway API HTTPS 入口、发布 AnalysisTemplate，以及可选 Istio mTLS 策略。
- 38 个服务的端口、数据库地址、服务地址、容量等级和分片/RPC 差异配置。
- 37 个数据库服务各自独立的迁移 Job、迁移镜像、ExternalSecret、ServiceAccount 和应用启动门禁。

## 前置条件

- Kubernetes 1.30 或更高版本。
- Helm 4.2.0 或经兼容验证的后续版本。
- Argo Rollouts 1.8.3 CRD 和控制器。
- Gateway API CRD、可用的 `GatewayClass`、Metrics Server 和 Prometheus。
- 平台预先创建 `emall-runtime-secret` 与 TLS Secret，并安装 External Secrets Operator；仓库不保存生产密钥。
- 密钥系统按 `emall/migrations/<service>` 提供服务级 `username` 和 `password`，账号只能访问对应业务库及其分片。

## 数据库迁移

`migration-runner` 只包含通用 Java 执行器，不再打包任何业务 SQL。流水线必须为每个服务构建独立且不可变的镜像：

```powershell
docker build --file Dockerfile.migration `
    --build-arg MODULE=order `
    --build-arg VERSION=0.1.0-SNAPSHOT `
    --tag emall/order-migration:0.1.0 .
```

每个 Job 只接收一个服务的凭据，先迁移一个金丝雀分片，再按有限并发分批执行并在批次间保留观测窗口。应用 Rollout 的
init container 仅有读取自身 Job 状态的 RBAC 权限，因此某个服务迁移失败只会阻止该服务新 Pod 就绪，不会阻塞其他服务。
修复后删除失败 Job 并使用同一不可变制品重建即可恢复；Flyway 历史表保证已成功分片不会重复执行。

默认只允许向后兼容的 `EXPAND` 迁移。删除表、删除字段、重命名和截断必须在旧版本完全下线后切换到 `CONTRACT`，同时
设置 `allowDestructiveChanges=true`、`minimumCompatibleVersion` 和审批单号；缺少任一门禁时执行器会拒绝运行。

## 本地验证

```powershell
helm lint ops/helm/emall --strict
helm template emall ops/helm/emall --namespace emall > target/emall-rendered.yml
kubeconform -strict -summary -ignore-missing-schemas target/emall-rendered.yml
```

`kubeconform` 的 `-ignore-missing-schemas` 只用于 Argo Rollouts、Gateway API 等 CRD；CI 还会在安装固定版本 Rollout CRD 的
Kind 集群执行 `kubectl apply --server-side --dry-run=server`，标准资源或 CRD 资源任一不合法都会失败。

## 生产发布

环境差异通过独立且受审计的 values 文件覆盖，不修改模板，也不提交渲染产物：

```powershell
helm upgrade --install emall ops/helm/emall `
    --namespace emall `
    --create-namespace `
    --values values-production.yml
```

不要对包含全部服务的发布使用全局 `--atomic --wait`，否则单服务迁移故障会人为扩大成整个平台回滚。发布控制器应分别
跟踪 37 个 Job 和 38 个 Rollout 的状态，只重试或回滚失败服务。镜像 tag 不能是 `latest`；生产流水线应进一步使用
不可变 digest，并在预生产环境完成 Server-Side Dry Run、策略检查、容量门禁和渐进式发布后再提升版本。
