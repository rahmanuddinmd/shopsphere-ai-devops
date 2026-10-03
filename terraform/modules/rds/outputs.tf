output "endpoint" {
  description = "Use as DB_HOST (without the :5432 suffix)"
  value       = aws_db_instance.this.address
}

output "master_secret_arn" {
  description = "Secrets Manager secret holding the generated master password"
  value       = aws_db_instance.this.master_user_secret[0].secret_arn
}
