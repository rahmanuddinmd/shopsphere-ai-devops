# 06 — Git Workflow

```mermaid
gitGraph
    commit id: "chore: init"
    branch develop
    checkout develop
    commit id: "docs: phase 0"
    branch feature/cart
    checkout feature/cart
    commit id: "feat: cart api"
    checkout develop
    merge feature/cart
    branch release/1.0.0
    checkout release/1.0.0
    commit id: "chore: v1.0.0"
    checkout main
    merge release/1.0.0 tag: "v1.0.0"
    checkout develop
    merge release/1.0.0
    checkout main
    branch hotfix/1.0.1
    checkout hotfix/1.0.1
    commit id: "fix: order total"
    checkout main
    merge hotfix/1.0.1 tag: "v1.0.1"
    checkout develop
    merge hotfix/1.0.1
```

| Branch | Created from | Merged into | Deploys to |
|---|---|---|---|
| `feature/*` | develop | develop (PR) | — |
| `develop` | main | — | DEV (automatic) |
| `release/*` | develop | main + develop | STAGING |
| `main` | — | — | PROD (approval) |
| `hotfix/*` | main | main + develop | PROD (approval) |
