variable "cluster_name" {
  type = string
}

variable "kubernetes_version" {
  description = "Use a version in EKS STANDARD support (extended support costs 6x). Check: aws eks describe-cluster-versions"
  type        = string
  default     = "1.35"
}

variable "vpc_id" {
  type = string
}

variable "private_subnet_ids" {
  type = list(string)
}

variable "public_access_cidrs" {
  description = "CIDRs allowed to reach the public Kubernetes API (set to your IP/32 for better security)"
  type        = list(string)
  default     = ["0.0.0.0/0"]
}

variable "node_instance_types" {
  type    = list(string)
  default = ["t3.medium"]
}

variable "node_capacity_type" {
  description = "ON_DEMAND or SPOT (SPOT is ~60-70% cheaper but can be interrupted)"
  type        = string
  default     = "ON_DEMAND"
}

variable "node_min_size" {
  type    = number
  default = 1
}

variable "node_max_size" {
  type    = number
  default = 3
}

variable "node_desired_size" {
  type    = number
  default = 2
}

variable "ebs_csi_role_arn" {
  description = "IAM role for the EBS CSI driver (from modules/iam)"
  type        = string
}

variable "enable_container_insights" {
  description = "Install the CloudWatch Observability add-on (Container Insights). Adds CloudWatch cost."
  type        = bool
  default     = false
}

variable "cloudwatch_role_arn" {
  description = "IAM role for the CloudWatch agent (from modules/iam)"
  type        = string
  default     = ""
}

variable "access_entries" {
  description = "Extra EKS access entries (e.g. the Jenkins IAM principal)"
  type        = any
  default     = {}
}

variable "tags" {
  type    = map(string)
  default = {}
}
