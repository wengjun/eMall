# Kubernetes 辅助资产

[项目首页](../../README.md) | [文档索引](../../docs/README.md) | [唯一生产部署入口](../helm/emall/README.md)

在线服务不再在本目录维护手写 Deployment、Service、HPA、PDB、ServiceAccount、NetworkPolicy 或 Gateway API。
这些对象全部由 [`ops/helm/emall`](../helm/emall/README.md) 唯一生成，避免原生清单与 Helm 参数发生漂移。

本目录仅保留不与在线服务 Chart 竞争的辅助资产：

- `external-secrets/runtime-secret.yml`：从平台 SecretStore 投影运行凭据，Chart 只引用生成后的 Secret。
- 数据库迁移不再维护集中式原始清单。权威 Helm Chart 会为每个数据库服务生成独立迁移 Job、ServiceAccount 和
  ExternalSecret，迁移制品由 `Dockerfile.migration` 按模块构建。
- `chaos`：仅用于隔离测试环境的 Chaos Mesh 演练，禁止直接在生产执行。

不要对整个 `ops/k8s` 目录执行 `kubectl apply -f`。在线服务必须通过 Helm 发布，辅助资产则由平台流水线按职责和环境单独审批。
