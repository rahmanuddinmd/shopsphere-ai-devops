# Git & GitHub Guide

## 1. Branching model

| Branch | Purpose | Created from | Merges into | Lifetime |
|---|---|---|---|---|
| `main` | Production-ready, tagged releases | — | — | Permanent |
| `develop` | Integration of finished features; deploys to DEV | `main` | — | Permanent |
| `feature/<name>` | One feature or fix | `develop` | `develop` via PR | Hours to days |
| `release/<x.y.z>` | Stabilise a release; deploys to STAGING | `develop` | `main` **and** `develop` | Days |
| `hotfix/<x.y.z>` | Urgent production fix | `main` | `main` **and** `develop` | Hours |

Examples: `feature/ai-assistant`, `feature/product-search`, `feature/cart`,
`feature/monitoring`, `feature/terraform`, `release/1.0.0`, `hotfix/1.0.1`.

**Trade-off:** this Gitflow-style model fits versioned releases and approval gates.
Trunk-based development (short-lived branches straight to `main`, feature flags) is
faster and favoured by high-frequency deployers. Know both for interviews.

## 2. Commit messages — Conventional Commits

```text
<type>(optional scope): <short imperative description>
```

| Type | Use for | Example |
|---|---|---|
| `feat` | New feature | `feat(cart): add update-quantity endpoint` |
| `fix` | Bug fix | `fix(order): correct total with multiple items` |
| `ci` | Jenkins / Actions | `ci: add Trivy image scan stage` |
| `infra` | Terraform / Kubernetes / Helm | `infra(eks): add managed node group` |
| `docs` | Documentation | `docs: add EKS deployment guide` |
| `test` | Tests only | `test(product): add repository tests` |
| `refactor` | No behaviour change | `refactor(ai): extract prompt builder` |
| `chore` | Tooling, deps, versions | `chore: bump Spring Boot` |

Breaking change: add `!` after the type (`feat!: ...`) and a `BREAKING CHANGE:` footer.

## 3. Branch protection (GitHub → Settings → Rules → Rulesets)

Free GitHub accounts get full branch protection on **public** repositories. Keep this repo
public (it contains no secrets by design) or use GitHub Pro for a private repo.

**Ruleset `protect-main`** — target `main`:
- Restrict deletions; block force pushes
- Require a pull request before merging (1 approval; as a solo learner add yourself to the
  bypass list but still use PRs)
- Require conversation resolution
- Require status checks (added in Phase 7/16): `Jenkins` and `PR Validation`
- Require linear history (optional)

**Ruleset `protect-develop`** — target `develop`:
- Restrict deletions; block force pushes; require a pull request

Set the default branch to `develop` (Settings → General) so PRs target integration.

## 4. Authentication options

GitHub no longer accepts your account password for Git operations. Choose one:

### Option A — SSH key (recommended for daily work)
```bash
ssh-keygen -t ed25519 -C "your-github-email@example.com"
eval "$(ssh-agent -s)"
ssh-add ~/.ssh/id_ed25519
cat ~/.ssh/id_ed25519.pub
```
GitHub → Settings → SSH and GPG keys → New SSH key → paste the public key. Then:
```bash
ssh -T git@github.com
```
Expected: `Hi rahmanuddinmd! You've successfully authenticated, but GitHub does not provide shell access.`
Remote URL format: `git@github.com:rahmanuddinmd/shopsphere-ai-devops.git`

### Option B — Personal Access Token (HTTPS; also what Jenkins uses)
GitHub → Settings → Developer settings → Personal access tokens → **Fine-grained tokens** →
repository access: only `shopsphere-ai-devops`; permissions: Contents (read/write),
Metadata (read), Commit statuses (read/write), Webhooks (read/write).
Use the token as the password when Git prompts. Store it in a credential manager, never in files.
Remote URL format: `https://github.com/rahmanuddinmd/shopsphere-ai-devops.git`

### Option C — GitHub CLI
```bash
gh auth login
gh repo create rahmanuddinmd/shopsphere-ai-devops --public --source=. --remote=origin --push
```
`gh` handles authentication and can create the repository and push in one step.

| | SSH | PAT | GitHub CLI |
|---|---|---|---|
| Best for | Your workstation | CI systems (Jenkins) | Fast setup, PR work from terminal |
| Expiry | None (rotate yearly) | Set an expiry (e.g. 90 days) | Token managed by `gh` |
| Scope control | Account-wide | Per repository (fine-grained) | Scopes chosen at login |

## 5. Releases and tags
```bash
git checkout main
git pull
git tag -a v1.0.0 -m "ShopSphere AI 1.0.0"
git push origin v1.0.0
```
Annotated tags (`-a`) store author, date and message — use them for releases.
