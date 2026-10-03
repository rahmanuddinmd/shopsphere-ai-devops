# AWS account preparation

1. **Root user**: enable MFA, delete any root access keys, never use it day-to-day.
2. **Admin identity**: IAM Identity Center user (preferred, `aws configure sso`) or an IAM user with MFA.
3. **Budget**: Billing → Budgets → monthly cost budget (e.g. $15) with 50/80/100% email alerts.
   Terraform also creates one when `budget_emails` is set.
4. **Region**: `ap-south-1` (Mumbai). `aws configure set region ap-south-1`
5. **Verify**:
   ```bash
   aws sts get-caller-identity      # Arn must NOT end in :root
   aws eks describe-cluster-versions --query 'clusterVersions[?status==`STANDARD_SUPPORT`].clusterVersion'
   ```
   Use a Kubernetes version from that list in `terraform.tfvars` (extended support costs 6x).
6. **Bedrock (optional)**: Bedrock console → Model access → enable one text model in your region,
   then set `BEDROCK_MODEL_ID` to its model or inference-profile ID.

Cost overview: see `docs/aws/AWS_COST_GUIDE.md`. Full deployment: `docs/deployment/eks.md`.
