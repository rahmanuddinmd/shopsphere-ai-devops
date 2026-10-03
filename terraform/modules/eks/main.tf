# Amazon EKS cluster + managed node group + core add-ons.
# Wraps terraform-aws-modules/eks/aws (v21 input names).
# COST: control plane ~$0.10/h + nodes (t3.medium ~$0.045/h each) -> POTENTIALLY EXPENSIVE.
module "eks" {
  source  = "terraform-aws-modules/eks/aws"
  version = "~> 21.0"

  name               = var.cluster_name
  kubernetes_version = var.kubernetes_version

  endpoint_public_access       = true
  endpoint_public_access_cidrs = var.public_access_cidrs
  endpoint_private_access      = true

  # Control-plane logs to CloudWatch (keep retention short: CloudWatch Logs bill per GB)
  enabled_log_types                      = ["api", "audit", "authenticator"]
  cloudwatch_log_group_retention_in_days = 7

  # EKS access entries (current API) instead of the legacy aws-auth ConfigMap
  authentication_mode                      = "API"
  enable_cluster_creator_admin_permissions = true
  access_entries                           = var.access_entries

  vpc_id     = var.vpc_id
  subnet_ids = var.private_subnet_ids

  addons = merge({
    vpc-cni = {
      before_compute = true
    }
    eks-pod-identity-agent = {
      before_compute = true
    }
    kube-proxy = {}
    coredns    = {}
    aws-ebs-csi-driver = {
      pod_identity_association = [{
        role_arn        = var.ebs_csi_role_arn
        service_account = "ebs-csi-controller-sa"
      }]
    }
    }, var.enable_container_insights ? {
    amazon-cloudwatch-observability = {
      pod_identity_association = [{
        role_arn        = var.cloudwatch_role_arn
        service_account = "cloudwatch-agent"
      }]
    }
  } : {})

  eks_managed_node_groups = {
    general = {
      ami_type       = "AL2023_x86_64_STANDARD"
      instance_types = var.node_instance_types
      capacity_type  = var.node_capacity_type
      min_size       = var.node_min_size
      max_size       = var.node_max_size
      desired_size   = var.node_desired_size
      disk_size      = 30
      labels = {
        workload = "general"
      }
    }
  }

  tags = var.tags
}
