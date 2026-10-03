output "cluster_name" {
  value = module.eks.cluster_name
}

output "configure_kubectl" {
  description = "Run this to point kubectl at the cluster"
  value       = "aws eks update-kubeconfig --region ${var.aws_region} --name ${module.eks.cluster_name}"
}

output "ecr_registry" {
  value = module.ecr.registry
}

output "ecr_repository_urls" {
  value = module.ecr.repository_urls
}

output "vpc_id" {
  value = module.vpc.vpc_id
}

output "aws_lb_controller_role_arn" {
  value = module.iam.aws_lb_controller_role_arn
}

output "backend_bedrock_role_arn" {
  value = module.iam.backend_role_arn
}

output "jenkins_user_arn" {
  description = "Create an access key for this user in the IAM console and store it in Jenkins as 'aws-jenkins'"
  value       = module.iam.jenkins_user_arn
}

output "rds_endpoint" {
  value = var.enable_rds ? module.rds[0].endpoint : null
}

output "rds_master_secret_arn" {
  value = var.enable_rds ? module.rds[0].master_secret_arn : null
}
