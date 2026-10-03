# CI server: Terraform creates it, Ansible configures it

Jenkins + SonarQube + Nexus need ~6 GB RAM. Run them on one EC2 VM instead of your laptop.
Diagram: `diagrams/21-ci-server-provisioning.md`.

> **COST:** t3.large ~$0.09/hour + 50 GB disk (~$4/month) + public IP. **Stop** it between sessions
> (`terraform output stop_command`); stopped = only disk cost. `terraform destroy` when the course ends.

## 1. Create an EC2 key pair (once)
EC2 console → Key Pairs → Create → name `shopsphere-ci` → download `shopsphere-ci.pem` →
```bash
mv ~/Downloads/shopsphere-ci.pem ~/.ssh/ && chmod 600 ~/.ssh/shopsphere-ci.pem
```

## 2. Terraform - create the VM
```bash
cd terraform/environments/tooling
cp backend.hcl.example backend.hcl                 # same state bucket as dev
cp terraform.tfvars.example terraform.tfvars       # key_name = "shopsphere-ci", allowed_cidrs = ["<your-ip>/32"]
terraform init -backend-config=backend.hcl
terraform plan -out tfplan                         # 1 instance, 1 SG + rules, IAM role/profile
terraform apply tfplan
terraform output ansible_inventory_line
```
`allowed_cidrs` refuses `0.0.0.0/0`: Jenkins, SonarQube and Nexus must never be open to the internet.

## 3. Ansible - configure the VM
```bash
cd ../../../ansible
cp inventory/hosts.ini.example inventory/hosts.ini   # paste the inventory line under [ci_servers]
ansible all -m ping                                  # expect "pong"
ansible-playbook playbooks/ci-server.yml --syntax-check
ansible-playbook playbooks/ci-server.yml --check     # dry run (some tasks report "skipped" in check mode)
ansible-playbook playbooks/ci-server.yml             # ~10-15 minutes on the first run
```
What the roles do:

| Role | Tasks | Why |
|---|---|---|
| `common` | wait for cloud-init, base packages, timezone, `vm.max_map_count=524288`, 4 GB swap, unattended security upgrades | SonarQube's Elasticsearch requires the sysctl; swap prevents OOM on 8 GB |
| `docker` | Docker apt repo + Engine + Compose v2, log rotation (20 MB × 3), docker group | Disks don't fill with container logs |
| `shopsphere_ci` | clone repo to `/opt/shopsphere`, generate `.env` with random secrets (first run only, mode 600), `docker compose --profile ci up -d --build`, wait until all three UIs answer | Repeatable, idempotent setup - run it again any time |

Re-running the playbook is safe (idempotent): it updates the repository and restarts only what changed.
It never overwrites the generated `.env`.

## 4. First login on each tool
```bash
ssh -i ~/.ssh/shopsphere-ci.pem ubuntu@<ip>
cd /opt/shopsphere
docker compose exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
docker compose exec nexus cat /nexus-data/admin.password
```
Then follow `docs/devops/jenkins.md`, `docs/devops/sonarqube.md`, `docs/devops/nexus.md`.

## 5. Optional: Jenkins Configuration-as-Code
Set `shopsphere_ci_enable_jcasc: true` in `ansible/group_vars/ci_servers.yml` **before the first run**,
then on the server fill the empty values in `/opt/shopsphere/.env` (GitHub PAT, Sonar token, Nexus
user, AWS keys of `shopsphere-dev-jenkins`, NVD key) and run
`docker compose --profile ci up -d jenkins`. Jenkins starts with the admin user, all five
credentials and the `sonarqube` server already configured (`jenkins/casc/enabled/jenkins.yaml`).

## 6. Webhooks from GitHub
Add GitHub's hook IP ranges to `github_webhook_cidrs` (`curl -s https://api.github.com/meta | jq .hooks`),
`terraform apply`, then add the webhook `http://<ip>:8085/github-webhook/` in GitHub.
Alternative without opening a port: Multibranch "Scan periodically - 5 minutes".
