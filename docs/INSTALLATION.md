# Installation - every tool, every machine

## 1. Which machine needs what

| Package | Your laptop (WSL/Ubuntu/macOS) | CI server (EC2, via Ansible) | Inside containers (automatic) |
|---|---|---|---|
| Git | ✅ | ✅ (Ansible) | Jenkins image |
| Docker Engine/Desktop + Compose v2 | ✅ | ✅ (Ansible role `docker`) | - |
| JDK 21 (Temurin) | ✅ (backend dev) | ❌ not needed | backend build image, Jenkins image |
| Maven | ❌ use `backend/mvnw` (downloads Maven 3.9.11) | ❌ | wrapper inside builds |
| Node.js 22 + npm | ✅ (frontend dev) | ❌ | frontend build image, Jenkins image |
| AWS CLI v2 | ✅ | ❌ | Jenkins image |
| Terraform ≥ 1.10 | ✅ | ❌ | Jenkins image (validate stage) |
| kubectl | ✅ | ❌ | Jenkins image |
| Helm 3 | ✅ (cluster add-ons) | ❌ | - |
| Trivy | ✅ (optional local scans) | ❌ | Jenkins image |
| Ansible | ✅ (to configure the CI server) | ❌ (agentless - only Python, preinstalled on Ubuntu) | - |
| PostgreSQL, Jenkins, SonarQube, Nexus, Prometheus, Grafana, Ollama | ❌ never install directly | run as containers | Docker images |

Everything server-side runs in containers, so the only things you install by hand are the CLIs above.

## 2. Windows 10/11 (recommended path)
1. PowerShell as Administrator: `wsl --install -d Ubuntu-24.04`, reboot, create a Linux user.
2. Install **Docker Desktop** → Settings → General: *Use the WSL 2 based engine* → Resources →
   WSL integration: enable *Ubuntu-24.04* → Resources → Advanced: memory ≥ 8 GB (12-16 GB if you run the `ci` profile).
3. Optional `%UserProfile%\.wslconfig`:
   ```ini
   [wsl2]
   memory=12GB
   processors=4
   ```
   then `wsl --shutdown`.
4. In the Ubuntu terminal:
   ```bash
   mkdir -p ~/projects && cd ~/projects
   git clone https://github.com/rahmanuddinmd/shopsphere-ai-devops.git && cd shopsphere-ai-devops
   bash scripts/install-tools.sh        # skips Docker Engine on WSL (Docker Desktop provides it)
   exec $SHELL -l                       # reload PATH
   bash scripts/setup.sh                # must end with 0 failed
   ```
Keep the repository under `~/projects` (Linux filesystem), not `/mnt/c/...` - faster and no CRLF/permission problems.

## 3. Ubuntu 22.04 / 24.04 (desktop or server)
```bash
sudo apt-get update && sudo apt-get install -y git
git clone https://github.com/rahmanuddinmd/shopsphere-ai-devops.git && cd shopsphere-ai-devops
bash scripts/install-tools.sh            # Docker Engine, JDK 21, Node 22, AWS CLI, Terraform, kubectl, Helm, Trivy, Ansible
newgrp docker                            # or log out/in so docker works without sudo
bash scripts/setup.sh
```
Install a subset: `bash scripts/install-tools.sh docker java node`.

## 4. macOS (Homebrew)
```bash
brew install --cask docker temurin@21
brew install node@22 awscli kubectl helm trivy ansible jq
brew tap hashicorp/tap && brew install hashicorp/tap/terraform
open -a Docker && bash scripts/setup.sh
```

## 5. What `scripts/install-tools.sh` does (Ubuntu)
| Tool | Source | Why this source |
|---|---|---|
| Docker Engine, Buildx, Compose v2 | download.docker.com apt repo | Official, current versions; Ubuntu's `docker.io` lags |
| Temurin JDK 21 | packages.adoptium.net apt repo | TCK-certified OpenJDK builds |
| Node.js 22 | deb.nodesource.com | Ubuntu's packaged Node is too old for Vite |
| AWS CLI v2 | awscli.amazonaws.com installer | v2 is only distributed this way |
| Terraform | apt.releases.hashicorp.com | Official signed packages |
| kubectl | pkgs.k8s.io (v1.35 channel) | Official community repo; stay within ±1 minor of EKS |
| Helm | official `get-helm-3` script | Upstream installer |
| Trivy | aquasecurity apt repo | Official |
| Ansible | `pipx install ansible` | Isolated from system Python |

Every apt repository is added with a signed keyring in `/etc/apt/keyrings` (no deprecated `apt-key`).

## 6. Accounts you need
GitHub (repository + fine-grained PAT) · AWS (admin IAM identity, MFA, budget - `docs/deployment/aws.md`) ·
optional NVD API key (OWASP) · optional Bedrock model access.
