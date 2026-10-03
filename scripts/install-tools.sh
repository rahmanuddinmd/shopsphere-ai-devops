#!/usr/bin/env bash
# =====================================================================
# scripts/install-tools.sh - installs every tool ShopSphere needs on Ubuntu 22.04/24.04 or WSL2 Ubuntu.
#
#   bash scripts/install-tools.sh            # install everything that is missing
#   bash scripts/install-tools.sh docker     # install only selected tools
# Tools: base docker java maven-wrapper(no install needed) node awscli terraform kubectl helm trivy ansible
#
# Idempotent: tools that are already installed are skipped. Needs sudo. macOS/Windows: see docs/INSTALLATION.md.
# WSL2 + Docker Desktop: Docker Engine is NOT installed inside WSL; enable WSL integration in Docker Desktop instead.
# =====================================================================
set -euo pipefail

[[ -r /etc/os-release ]] && . /etc/os-release
if [[ "${ID:-}" != "ubuntu" ]]; then
  echo "This script supports Ubuntu (including WSL2 Ubuntu). For other systems follow docs/INSTALLATION.md."
  exit 1
fi
CODENAME="${VERSION_CODENAME:-noble}"
ARCH="$(dpkg --print-architecture)"   # amd64 / arm64
IS_WSL=false; grep -qi microsoft /proc/version 2>/dev/null && IS_WSL=true

KUBECTL_MINOR="${KUBECTL_MINOR:-v1.35}"
NODE_MAJOR="${NODE_MAJOR:-22}"
KEYRINGS=/etc/apt/keyrings

have() { command -v "$1" >/dev/null 2>&1; }
log()  { printf '\n\033[1m==> %s\033[0m\n' "$*"; }
skip() { printf '    %s already installed - skipping\n' "$1"; }

apt_repo() {   # apt_repo <name> <key-url> <repo-line>
  local name="$1" key="$2" line="$3"
  sudo install -m 0755 -d "$KEYRINGS"
  curl -fsSL "$key" | sudo gpg --dearmor --yes -o "$KEYRINGS/$name.gpg"
  echo "$line" | sudo tee "/etc/apt/sources.list.d/$name.list" >/dev/null
}

install_base() {
  log "Base packages"
  sudo apt-get update -y
  sudo apt-get install -y ca-certificates curl gnupg lsb-release unzip zip jq git tree make apt-transport-https software-properties-common
}

install_docker() {
  log "Docker Engine + Compose v2"
  if have docker && docker compose version >/dev/null 2>&1; then skip docker; return; fi
  if $IS_WSL; then
    echo "    WSL detected: install Docker Desktop on Windows and enable"
    echo "    Settings > Resources > WSL integration > Ubuntu. Then reopen this terminal."
    return
  fi
  apt_repo docker https://download.docker.com/linux/ubuntu/gpg \
    "deb [arch=$ARCH signed-by=$KEYRINGS/docker.gpg] https://download.docker.com/linux/ubuntu $CODENAME stable"
  sudo apt-get update -y
  sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
  sudo systemctl enable --now docker
  sudo usermod -aG docker "$USER"
  echo "    Added $USER to the docker group - log out and back in (or run: newgrp docker)."
}

install_java() {
  log "Eclipse Temurin JDK 21"
  if have java && java -version 2>&1 | grep -q 'version "21'; then skip "java 21"; return; fi
  apt_repo adoptium https://packages.adoptium.net/artifactory/api/gpg/key/public \
    "deb [signed-by=$KEYRINGS/adoptium.gpg] https://packages.adoptium.net/artifactory/deb $CODENAME main"
  sudo apt-get update -y
  sudo apt-get install -y temurin-21-jdk
  echo "    Maven is not installed globally: the project uses the Maven wrapper (backend/mvnw)."
}

install_node() {
  log "Node.js ${NODE_MAJOR} LTS"
  if have node && [[ "$(node -v | cut -d. -f1 | tr -d v)" -ge "$NODE_MAJOR" ]]; then skip node; return; fi
  apt_repo nodesource https://deb.nodesource.com/gpgkey/nodesource-repo.gpg.key \
    "deb [signed-by=$KEYRINGS/nodesource.gpg] https://deb.nodesource.com/node_${NODE_MAJOR}.x nodistro main"
  sudo apt-get update -y
  sudo apt-get install -y nodejs
}

install_awscli() {
  log "AWS CLI v2"
  if have aws && aws --version 2>&1 | grep -q 'aws-cli/2'; then skip "aws cli v2"; return; fi
  local zip="awscli-exe-linux-x86_64.zip"; [[ "$ARCH" == "arm64" ]] && zip="awscli-exe-linux-aarch64.zip"
  tmp="$(mktemp -d)"
  curl -fsSL "https://awscli.amazonaws.com/$zip" -o "$tmp/awscli.zip"
  unzip -q "$tmp/awscli.zip" -d "$tmp"
  sudo "$tmp/aws/install" --update
  rm -rf "$tmp"
}

install_terraform() {
  log "Terraform (HashiCorp apt repository)"
  if have terraform; then skip terraform; return; fi
  apt_repo hashicorp https://apt.releases.hashicorp.com/gpg \
    "deb [arch=$ARCH signed-by=$KEYRINGS/hashicorp.gpg] https://apt.releases.hashicorp.com $CODENAME main"
  sudo apt-get update -y
  sudo apt-get install -y terraform
}

install_kubectl() {
  log "kubectl (${KUBECTL_MINOR} channel - keep within one minor version of your EKS cluster)"
  if have kubectl; then skip kubectl; return; fi
  apt_repo kubernetes "https://pkgs.k8s.io/core:/stable:/${KUBECTL_MINOR}/deb/Release.key" \
    "deb [signed-by=$KEYRINGS/kubernetes.gpg] https://pkgs.k8s.io/core:/stable:/${KUBECTL_MINOR}/deb/ /"
  sudo apt-get update -y
  sudo apt-get install -y kubectl
}

install_helm() {
  log "Helm"
  if have helm; then skip helm; return; fi
  curl -fsSL https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash
}

install_trivy() {
  log "Trivy"
  if have trivy; then skip trivy; return; fi
  apt_repo trivy https://aquasecurity.github.io/trivy-repo/deb/public.key \
    "deb [signed-by=$KEYRINGS/trivy.gpg] https://aquasecurity.github.io/trivy-repo/deb generic main"
  sudo apt-get update -y
  sudo apt-get install -y trivy
}

install_ansible() {
  log "Ansible (via pipx, isolated from system Python)"
  if have ansible-playbook; then skip ansible; return; fi
  sudo apt-get install -y pipx
  pipx ensurepath
  pipx install --include-deps ansible
  pipx inject ansible boto3 botocore >/dev/null 2>&1 || true
  echo "    Open a new terminal (PATH update) before running ansible-playbook."
}

ALL=(base docker java node awscli terraform kubectl helm trivy ansible)
TARGETS=("$@"); [[ ${#TARGETS[@]} -eq 0 ]] && TARGETS=("${ALL[@]}")
for t in "${TARGETS[@]}"; do
  case "$t" in
    base|docker|java|node|awscli|terraform|kubectl|helm|trivy|ansible) "install_$t" ;;
    *) echo "Unknown tool '$t'. Choose from: ${ALL[*]}"; exit 1 ;;
  esac
done

log "Done. Verify with: bash scripts/setup.sh"
