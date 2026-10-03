provider "aws" {
  region = var.aws_region

  # Every resource gets these tags -> easy cost tracking and cleanup verification
  default_tags {
    tags = {
      Project     = "ShopSphere"
      Environment = var.environment
      ManagedBy   = "Terraform"
      Owner       = var.owner
    }
  }
}
