variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "ap-south-1"
}

variable "environment" {
  description = "Environment name"
  type        = string
  default     = "dev"
  validation {
    condition     = contains(["dev", "staging", "prod"], var.environment)
    error_message = "environment must be dev, staging or prod."
  }
}

variable "owner" {
  type    = string
  default = "rahmanuddinmd"
}

variable "vpc_cidr" {
  type    = string
  default = "10.0.0.0/16"
}

variable "kubernetes_version" {
  description = "EKS version in standard support (check: aws eks describe-cluster-versions)"
  type        = string
  default     = "1.35"
}

variable "node_instance_types" {
  type    = list(string)
  default = ["t3.medium"]
}

variable "node_capacity_type" {
  description = "ON_DEMAND or SPOT"
  type        = string
  default     = "ON_DEMAND"
}

variable "node_desired_size" {
  type    = number
  default = 2
}

variable "public_access_cidrs" {
  description = "Who may reach the Kubernetes API. Best practice: [\"<YOUR_PUBLIC_IP>/32\"]"
  type        = list(string)
  default     = ["0.0.0.0/0"]
}

variable "enable_rds" {
  description = "Create Amazon RDS PostgreSQL (extra cost). DEV default: in-cluster PostgreSQL."
  type        = bool
  default     = false
}

variable "enable_container_insights" {
  description = "Install CloudWatch Container Insights add-on (extra CloudWatch cost)"
  type        = bool
  default     = false
}

variable "create_jenkins_user" {
  type    = bool
  default = true
}

variable "monthly_budget_usd" {
  type    = number
  default = 15
}

variable "budget_emails" {
  type    = list(string)
  default = []
}
