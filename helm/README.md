# Helm

ShopSphere deploys its **own** application with **kustomize** (`kubernetes/`), and uses **Helm**
for third-party cluster components (`scripts/install-cluster-addons.sh`): AWS Load Balancer
Controller, metrics-server, kube-prometheus-stack, and optionally Loki.

Why both: kustomize patches plain YAML (no templating language, easy to read in reviews);
Helm packages reusable software with versioned charts and values.

**Phase 15 challenge:** convert `kubernetes/base` into a chart in `helm/shopsphere/`
(`helm create shopsphere`, move the manifests to `templates/`, template `image.repository`,
`image.tag`, `replicaCount` and the ConfigMap values), then compare
`helm template` output with `kubectl kustomize kubernetes/overlays/dev`.
