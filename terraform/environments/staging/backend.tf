# Remote state in S3 with native locking (no DynamoDB table needed with Terraform >= 1.10).
# The bucket name contains your account ID, so it is supplied at init time:
#   cp backend.hcl.example backend.hcl   # set your bucket name (no secrets in this file)
#   terraform init -backend-config=backend.hcl
terraform {
  backend "s3" {
    key          = "staging/terraform.tfstate"
    encrypt      = true
    use_lockfile = true
  }
}
