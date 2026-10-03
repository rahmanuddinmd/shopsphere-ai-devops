# =====================================================================
# ShopSphere - DEV environment on AWS
# Creates: VPC (2 AZ, 1 NAT), ECR repos, EKS (+ add-ons, 2 x t3.medium), IAM / Pod Identity,
#          optional RDS, budget alerts.
# COST WARNING: EKS + NAT + nodes ~ $0.30-0.35 per hour. Destroy after every session:
#   kubectl delete ingress --all -A   (removes the ALB first)  then  terraform destroy
# =====================================================================
locals {
  name = "shopsphere-${var.environment}"
}

module "vpc" {
  source             = "../../modules/vpc"
  name               = local.name
  cidr               = var.vpc_cidr
  az_count           = 2
  single_nat_gateway = true
}

module "ecr" {
  source       = "../../modules/ecr"
  repositories = ["shopsphere-backend", "shopsphere-frontend"]
  force_delete = true
}

module "iam" {
  source              = "../../modules/iam"
  name                = local.name
  cluster_name        = module.eks.cluster_name
  app_namespaces      = ["shopsphere-prod"]
  ecr_repository_arns = module.ecr.repository_arns
  create_jenkins_user = var.create_jenkins_user
}

module "eks" {
  source                    = "../../modules/eks"
  cluster_name              = local.name
  kubernetes_version        = var.kubernetes_version
  vpc_id                    = module.vpc.vpc_id
  private_subnet_ids        = module.vpc.private_subnet_ids
  public_access_cidrs       = var.public_access_cidrs
  node_instance_types       = var.node_instance_types
  node_capacity_type        = var.node_capacity_type
  node_desired_size         = var.node_desired_size
  ebs_csi_role_arn          = module.iam.ebs_csi_role_arn
  enable_container_insights = var.enable_container_insights
  cloudwatch_role_arn       = module.iam.cloudwatch_role_arn

  # Jenkins may deploy to the cluster (training: cluster-admin; production: namespace-scoped)
  access_entries = var.create_jenkins_user ? {
    jenkins = {
      principal_arn = module.iam.jenkins_user_arn
      policy_associations = {
        admin = {
          policy_arn   = "arn:aws:eks::aws:cluster-access-policy/AmazonEKSClusterAdminPolicy"
          access_scope = { type = "cluster" }
        }
      }
    }
  } : {}
}

module "security_groups" {
  source                     = "../../modules/security-groups"
  name                       = local.name
  vpc_id                     = module.vpc.vpc_id
  eks_node_security_group_id = module.eks.node_security_group_id
}

module "rds" {
  count              = var.enable_rds ? 1 : 0
  source             = "../../modules/rds"
  name               = local.name
  private_subnet_ids = module.vpc.private_subnet_ids
  security_group_id  = module.security_groups.rds_security_group_id
}

module "monitoring" {
  source             = "../../modules/monitoring"
  name               = local.name
  monthly_budget_usd = var.monthly_budget_usd
  budget_emails      = var.budget_emails
}
