# terraform init -backend-config=backend.hcl   (same bucket as the other environments)
terraform {
  backend "s3" {
    key          = "tooling/terraform.tfstate"
    encrypt      = true
    use_lockfile = true
  }
}
