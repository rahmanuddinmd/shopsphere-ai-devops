# OPTIONAL Amazon RDS PostgreSQL (enable_rds = true). DEV uses the in-cluster StatefulSet instead.
# The master password is generated and stored by AWS in Secrets Manager (manage_master_user_password),
# so it never appears in Terraform code or state.
# COST: db.t4g.micro ~$0.016/h + storage -> LOW COST, but destroy it after training.
resource "aws_db_subnet_group" "this" {
  name       = "${var.name}-db"
  subnet_ids = var.private_subnet_ids
  tags       = var.tags
}

resource "aws_db_instance" "this" {
  identifier     = "${var.name}-postgres"
  engine         = "postgres"
  engine_version = var.engine_version
  instance_class = var.instance_class

  allocated_storage     = 20
  max_allocated_storage = 50
  storage_type          = "gp3"
  storage_encrypted     = true

  db_name                     = "shopsphere"
  username                    = "shopsphere"
  manage_master_user_password = true

  db_subnet_group_name   = aws_db_subnet_group.this.name
  vpc_security_group_ids = [var.security_group_id]
  publicly_accessible    = false
  multi_az               = false # production: true

  backup_retention_period    = 1    # production: 7+
  skip_final_snapshot        = true # production: false
  deletion_protection        = false
  auto_minor_version_upgrade = true
  apply_immediately          = true

  tags = var.tags
}
