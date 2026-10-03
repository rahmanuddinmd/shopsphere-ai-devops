# =====================================================================
# ShopSphere CI/tooling server: one Ubuntu 24.04 EC2 instance in the default VPC for
# Jenkins (8085), SonarQube (9000) and Nexus (8081). Configured afterwards by Ansible
# (ansible/playbooks/ci-server.yml).
# COST: t3.large ~$0.09/h + 50 GB gp3 (~$4/month) + public IPv4. STOP the instance when not
# training (stopped = only disk cost); terraform destroy when the course ends.
# =====================================================================
data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
  filter {
    name   = "default-for-az"
    values = ["true"]
  }
}

# Latest official Ubuntu 24.04 LTS AMI published by Canonical in SSM Parameter Store
data "aws_ssm_parameter" "ubuntu" {
  name = "/aws/service/canonical/ubuntu/server/24.04/stable/current/amd64/hvm/ebs-gp3/ami-id"
}

locals {
  tool_ports = {
    jenkins   = 8085
    sonarqube = 9000
    nexus     = 8081
  }
}

resource "aws_security_group" "ci" {
  name        = "shopsphere-ci-server"
  description = "ShopSphere CI server: SSH and tool UIs from allowed CIDRs only"
  vpc_id      = data.aws_vpc.default.id
  tags        = { Name = "shopsphere-ci-server" }
}

resource "aws_vpc_security_group_ingress_rule" "ssh" {
  for_each          = toset(var.allowed_cidrs)
  security_group_id = aws_security_group.ci.id
  description       = "SSH (Ansible)"
  ip_protocol       = "tcp"
  from_port         = 22
  to_port           = 22
  cidr_ipv4         = each.value
}

resource "aws_vpc_security_group_ingress_rule" "tools" {
  for_each = {
    for pair in setproduct(keys(local.tool_ports), var.allowed_cidrs) : "${pair[0]}-${pair[1]}" => {
      port = local.tool_ports[pair[0]]
      cidr = pair[1]
    }
  }
  security_group_id = aws_security_group.ci.id
  description       = "Tool UI ${each.key}"
  ip_protocol       = "tcp"
  from_port         = each.value.port
  to_port           = each.value.port
  cidr_ipv4         = each.value.cidr
}

resource "aws_vpc_security_group_ingress_rule" "github_webhooks" {
  for_each          = toset(var.github_webhook_cidrs)
  security_group_id = aws_security_group.ci.id
  description       = "GitHub webhooks to Jenkins"
  ip_protocol       = "tcp"
  from_port         = 8085
  to_port           = 8085
  cidr_ipv4         = each.value
}

resource "aws_vpc_security_group_egress_rule" "all" {
  security_group_id = aws_security_group.ci.id
  description       = "Outbound: package repos, GitHub, AWS APIs"
  ip_protocol       = "-1"
  cidr_ipv4         = "0.0.0.0/0"
}

# Instance role: SSM Session Manager (shell access without opening more ports)
resource "aws_iam_role" "ci" {
  name = "shopsphere-ci-server"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "ec2.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })
}

resource "aws_iam_role_policy_attachment" "ssm" {
  role       = aws_iam_role.ci.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

resource "aws_iam_instance_profile" "ci" {
  name = "shopsphere-ci-server"
  role = aws_iam_role.ci.name
}

resource "aws_instance" "ci" {
  ami                    = data.aws_ssm_parameter.ubuntu.value
  instance_type          = var.instance_type
  key_name               = var.key_name
  subnet_id              = data.aws_subnets.default.ids[0]
  vpc_security_group_ids = [aws_security_group.ci.id]
  iam_instance_profile   = aws_iam_instance_profile.ci.name

  metadata_options {
    http_tokens                 = "required" # IMDSv2 only
    http_put_response_hop_limit = 2          # lets containers reach IMDS if you later use an instance role
  }

  root_block_device {
    volume_size = var.root_volume_gb
    volume_type = "gp3"
    encrypted   = true
  }

  tags = { Name = "shopsphere-ci-server" }

  lifecycle {
    ignore_changes = [ami] # a new Ubuntu AMI must not recreate a configured server
  }
}
