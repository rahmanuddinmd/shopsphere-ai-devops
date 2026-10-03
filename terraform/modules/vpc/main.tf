# VPC with public subnets (ALB, NAT) and private subnets (EKS nodes, RDS) across 2 AZs.
# Wraps the community module terraform-aws-modules/vpc/aws.
data "aws_availability_zones" "available" {
  state = "available"
}

locals {
  azs = slice(data.aws_availability_zones.available.names, 0, var.az_count)
}

module "vpc" {
  source  = "terraform-aws-modules/vpc/aws"
  version = "~> 6.0"

  name = "${var.name}-vpc"
  cidr = var.cidr
  azs  = local.azs

  public_subnets  = [for i in range(var.az_count) : cidrsubnet(var.cidr, 8, i)]
  private_subnets = [for i in range(var.az_count) : cidrsubnet(var.cidr, 8, i + 10)]

  # COST: a NAT Gateway bills ~$0.056/h + data. single_nat_gateway = true is the cost-safe
  # training choice; production uses one NAT per AZ (single_nat_gateway = false).
  enable_nat_gateway   = true
  single_nat_gateway   = var.single_nat_gateway
  enable_dns_hostnames = true
  enable_dns_support   = true

  # Tags required by the AWS Load Balancer Controller to discover subnets
  public_subnet_tags = {
    "kubernetes.io/role/elb" = "1"
  }
  private_subnet_tags = {
    "kubernetes.io/role/internal-elb" = "1"
  }

  tags = var.tags
}
