# Terraform environments

| Folder | Purpose | When to apply |
|---|---|---|
| `dev` | Training cluster `shopsphere-dev` (DEV + STAGING namespaces share it) | Every AWS session, destroy afterwards |
| `staging` | Optional separate cluster (same code, own state key) | Not needed for training - STAGING is a namespace in DEV |
| `prod` | Separate production cluster `shopsphere-prod` | Only for the capstone, then destroy |

Same modules, different variables and state keys = environment parity without copy-paste drift.
See `docs/deployment/eks.md` for the full step-by-step run.
