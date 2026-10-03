output "public_ip" {
  value = aws_instance.ci.public_ip
}

output "ssh_command" {
  value = "ssh -i ~/.ssh/${var.key_name}.pem ubuntu@${aws_instance.ci.public_ip}"
}

output "ansible_inventory_line" {
  description = "Paste under [ci_servers] in ansible/inventory/hosts.ini"
  value       = "shopsphere-ci ansible_host=${aws_instance.ci.public_ip} ansible_user=ubuntu ansible_ssh_private_key_file=~/.ssh/${var.key_name}.pem"
}

output "urls" {
  value = {
    jenkins   = "http://${aws_instance.ci.public_ip}:8085"
    sonarqube = "http://${aws_instance.ci.public_ip}:9000"
    nexus     = "http://${aws_instance.ci.public_ip}:8081"
  }
}

output "stop_command" {
  description = "Stop the server between sessions to save cost"
  value       = "aws ec2 stop-instances --instance-ids ${aws_instance.ci.id} --region ${var.aws_region}"
}
