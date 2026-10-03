output "state_bucket" {
  description = "Put this value in terraform/environments/<env>/backend.hcl"
  value       = aws_s3_bucket.state.bucket
}
