variable "name" {
  type = string
}

variable "cluster_name" {
  type = string
}

variable "app_namespaces" {
  description = "Namespaces whose shopsphere-backend ServiceAccount may call Bedrock"
  type        = list(string)
  default     = ["shopsphere-dev", "shopsphere-staging"]
}

variable "ecr_repository_arns" {
  type = list(string)
}

variable "create_jenkins_user" {
  type    = bool
  default = true
}

variable "tags" {
  type    = map(string)
  default = {}
}
