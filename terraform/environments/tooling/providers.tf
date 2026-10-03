provider "aws" {
  region = var.aws_region
  default_tags {
    tags = {
      Project     = "ShopSphere"
      Environment = "tooling"
      ManagedBy   = "Terraform"
      Owner       = var.owner
    }
  }
}
