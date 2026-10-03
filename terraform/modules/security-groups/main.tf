# Security group for the optional RDS database: PostgreSQL reachable ONLY from EKS nodes.
resource "aws_security_group" "rds" {
  name        = "${var.name}-rds"
  description = "PostgreSQL access from EKS worker nodes only"
  vpc_id      = var.vpc_id
  tags        = merge(var.tags, { Name = "${var.name}-rds" })
}

resource "aws_vpc_security_group_ingress_rule" "rds_from_nodes" {
  security_group_id            = aws_security_group.rds.id
  description                  = "PostgreSQL from EKS nodes"
  ip_protocol                  = "tcp"
  from_port                    = 5432
  to_port                      = 5432
  referenced_security_group_id = var.eks_node_security_group_id
}
