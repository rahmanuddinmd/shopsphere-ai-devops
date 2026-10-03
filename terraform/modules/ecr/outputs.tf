output "repository_urls" {
  description = "Map of repository name => URL"
  value       = { for k, r in aws_ecr_repository.this : k => r.repository_url }
}

output "registry" {
  description = "<account>.dkr.ecr.<region>.amazonaws.com"
  value       = split("/", values(aws_ecr_repository.this)[0].repository_url)[0]
}

output "repository_arns" {
  value = [for r in aws_ecr_repository.this : r.arn]
}
