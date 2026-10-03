#!/usr/bin/env bash
# =====================================================================
# scripts/setup.sh — ShopSphere AI workstation prerequisite checker
#
# Purpose : Verifies that every tool needed for the training is installed
#           at a supported version, that Docker is running, that the machine
#           has enough RAM/disk, and (optionally) that AWS CLI is configured
#           with a NON-root identity.
# Safe    : Read-only. Installs nothing, changes nothing.
# Usage   : bash scripts/setup.sh   (install missing tools with: bash scripts/install-tools.sh)
# Exit    : 0 = all required checks passed, 1 = at least one FAIL
# =====================================================================
set -uo pipefail

PASS=0; WARN=0; FAIL=0

if [ -t 1 ]; then
  GREEN=$'\033[32m'; YELLOW=$'\033[33m'; RED=$'\033[31m'; BOLD=$'\033[1m'; RESET=$'\033[0m'
else
  GREEN=""; YELLOW=""; RED=""; BOLD=""; RESET=""
fi

ok()   { printf '  %s[PASS]%s %s\n' "$GREEN" "$RESET" "$*"; PASS=$((PASS+1)); }
warn() { printf '  %s[WARN]%s %s\n' "$YELLOW" "$RESET" "$*"; WARN=$((WARN+1)); }
fail() { printf '  %s[FAIL]%s %s\n' "$RED" "$RESET" "$*"; FAIL=$((FAIL+1)); }
section() { printf '\n%s== %s ==%s\n' "$BOLD" "$*" "$RESET"; }

# version_ge <actual> <minimum> : true if actual >= minimum (semantic compare)
version_ge() { [ "$(printf '%s\n%s\n' "$2" "$1" | sort -V | head -n1)" = "$2" ]; }

# check_tool <label> <command> <minimum-version> <version-extractor-command> [required|optional]
check_tool() {
  local label="$1" cmd="$2" min="$3" extractor="$4" level="${5:-required}" ver
  if ! command -v "$cmd" >/dev/null 2>&1; then
    if [ "$level" = "required" ]; then fail "$label not found (need >= $min)"; else warn "$label not found (optional, needed in a later phase)"; fi
    return
  fi
  ver="$(eval "$extractor" 2>/dev/null | head -n1 | tr -d '[:space:]')"
  if [ -z "$ver" ]; then
    warn "$label installed but version could not be detected"
  elif version_ge "$ver" "$min"; then
    ok "$label $ver (>= $min)"
  else
    if [ "$level" = "required" ]; then fail "$label $ver is older than $min"; else warn "$label $ver is older than $min"; fi
  fi
}

section "Operating system"
OS="$(uname -s)"
if grep -qi microsoft /proc/version 2>/dev/null; then ok "Linux on WSL2"; else ok "$OS"; fi

section "Core tools"
check_tool "git"       git       "2.40"   "git --version | awk '{print \$3}'"
check_tool "curl"      curl      "7.68"   "curl --version | awk 'NR==1{print \$2}'"
check_tool "jq"        jq        "1.6"    "jq --version | sed 's/jq-//'"

section "Application build tools"
check_tool "Java (JDK)" java     "21"     "java -version 2>&1 | sed -nE 's/.*version \"([0-9]+(\\.[0-9]+)*).*/\\1/p'"
if command -v java >/dev/null 2>&1; then
  JMAJOR="$(java -version 2>&1 | sed -nE 's/.*version \"([0-9]+).*/\1/p' | head -n1)"
  [ "${JMAJOR:-0}" = "21" ] || warn "Project targets Java 21 exactly; detected major version ${JMAJOR:-unknown}"
fi
check_tool "Maven"     mvn       "3.9.0"  "mvn -v 2>/dev/null | awk 'NR==1{print \$3}'"
check_tool "Node.js"   node      "22.0.0" "node -v | tr -d v"
check_tool "npm"       npm       "10.0.0" "npm -v"

section "Containers"
check_tool "Docker CLI" docker   "24.0.0" "docker version --format '{{.Client.Version}}'"
if command -v docker >/dev/null 2>&1; then
  if docker info >/dev/null 2>&1; then
    ok "Docker daemon reachable (server $(docker version --format '{{.Server.Version}}' 2>/dev/null))"
  else
    fail "Docker daemon not reachable (start Docker Desktop, or: sudo systemctl start docker; add user to 'docker' group)"
  fi
  if docker compose version >/dev/null 2>&1; then
    check_tool "Docker Compose v2" docker "2.20.0" "docker compose version --short | tr -d v"
  else
    fail "Docker Compose v2 plugin missing ('docker compose', not 'docker-compose')"
  fi
fi

section "Cloud & infrastructure tools"
check_tool "AWS CLI"   aws       "2.15.0" "aws --version 2>&1 | sed -nE 's#aws-cli/([0-9.]+).*#\\1#p'"
check_tool "Terraform" terraform "1.10.0" "terraform version | sed -nE 's/^Terraform v([0-9.]+).*/\\1/p'"
check_tool "kubectl"   kubectl   "1.30.0" "kubectl version --client -o json | jq -r .clientVersion.gitVersion | tr -d v"
check_tool "Helm"      helm      "3.14.0" "helm version --template '{{.Version}}' | tr -d v | cut -d+ -f1"

section "Security tools"
check_tool "Trivy"     trivy     "0.50.0" "trivy --version | sed -nE 's/^Version: ([0-9.]+).*/\\1/p'"

section "Optional (later phases)"
check_tool "GitHub CLI" gh       "2.40.0" "gh --version | awk 'NR==1{print \$3}'" optional
check_tool "Ansible"    ansible-playbook "2.15.0" "ansible-playbook --version | sed -nE '1s/.*core ([0-9.]+).*/\\1/p'" optional
check_tool "Ollama"     ollama   "0.3.0"  "ollama --version 2>/dev/null | grep -oE '[0-9]+\\.[0-9]+\\.[0-9]+' | head -n1" optional

section "Hardware"
if [ -r /proc/meminfo ]; then
  MEM_GB=$(( $(awk '/MemTotal/{print $2}' /proc/meminfo) / 1024 / 1024 ))
elif command -v sysctl >/dev/null 2>&1; then
  MEM_GB=$(( $(sysctl -n hw.memsize) / 1024 / 1024 / 1024 ))
else
  MEM_GB=0
fi
if   [ "$MEM_GB" -ge 30 ]; then ok "RAM ${MEM_GB} GB — can run all Compose profiles locally"
elif [ "$MEM_GB" -ge 14 ]; then warn "RAM ${MEM_GB} GB — run the 'ci' profile on a training VM or one profile at a time"
else fail "RAM ${MEM_GB} GB visible — below 16 GB minimum (on WSL2 check .wslconfig memory limit)"; fi

DISK_GB=$(( $(df -Pk "$HOME" | awk 'NR==2{print $4}') / 1024 / 1024 ))
if [ "$DISK_GB" -ge 60 ]; then ok "Free disk ${DISK_GB} GB"; else warn "Free disk ${DISK_GB} GB — 60 GB+ recommended for images and volumes"; fi

section "Git configuration"
GUSER="$(git config --global user.name 2>/dev/null || true)"
GMAIL="$(git config --global user.email 2>/dev/null || true)"
[ -n "$GUSER" ] && ok "git user.name = $GUSER" || fail "git user.name not set"
[ -n "$GMAIL" ] && ok "git user.email set" || fail "git user.email not set"

section "AWS identity (optional until Phase 11)"
if command -v aws >/dev/null 2>&1; then
  if ARN="$(aws sts get-caller-identity --query Arn --output text 2>/dev/null)"; then
    case "$ARN" in
      *":root") fail "AWS CLI is using the ROOT user — create an admin IAM identity and use that instead" ;;
      *) ok "AWS identity: ${ARN/[0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9]/<ACCOUNT_ID>}" ;;
    esac
    REGION="$(aws configure get region 2>/dev/null || true)"
    [ -n "$REGION" ] && ok "Default region: $REGION" || warn "No default AWS region configured"
  else
    warn "AWS CLI not configured yet (aws configure / aws configure sso)"
  fi
fi

printf '\n%sSummary:%s %s%d passed%s, %s%d warnings%s, %s%d failed%s\n' \
  "$BOLD" "$RESET" "$GREEN" "$PASS" "$RESET" "$YELLOW" "$WARN" "$RESET" "$RED" "$FAIL" "$RESET"
[ "$FAIL" -eq 0 ] && exit 0 || exit 1
