output "ebs_csi_role_arn" {
  value = module.ebs_csi_pod_identity.iam_role_arn
}

output "cloudwatch_role_arn" {
  value = module.cloudwatch_pod_identity.iam_role_arn
}

output "aws_lb_controller_role_arn" {
  value = module.aws_lb_controller_pod_identity.iam_role_arn
}

output "backend_role_arn" {
  value = module.backend_pod_identity.iam_role_arn
}

output "jenkins_user_arn" {
  value = var.create_jenkins_user ? aws_iam_user.jenkins[0].arn : null
}
