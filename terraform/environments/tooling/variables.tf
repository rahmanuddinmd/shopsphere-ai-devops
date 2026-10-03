variable "aws_region" {
  type    = string
  default = "ap-south-1"
}

variable "owner" {
  type    = string
  default = "rahmanuddinmd"
}

variable "instance_type" {
  description = "t3.large (8 GB) runs Jenkins + SonarQube + Nexus with swap; t3.xlarge (16 GB) also fits monitoring"
  type        = string
  default     = "t3.large"
}

variable "root_volume_gb" {
  type    = number
  default = 50
}

variable "key_name" {
  description = "Existing EC2 key pair name for SSH (EC2 console > Key Pairs). Ansible connects with it."
  type        = string
}

variable "allowed_cidrs" {
  description = "Who may reach SSH and the tool UIs. Use your public IP: [\"<YOUR_PUBLIC_IP>/32\"]"
  type        = list(string)
  validation {
    condition     = length(var.allowed_cidrs) > 0 && !contains(var.allowed_cidrs, "0.0.0.0/0")
    error_message = "Do not expose Jenkins/SonarQube/Nexus to the whole internet. Use your IP/32."
  }
}

variable "github_webhook_cidrs" {
  description = "Optional: GitHub hook source ranges (curl https://api.github.com/meta | jq .hooks) to allow webhooks to Jenkins"
  type        = list(string)
  default     = []
}
