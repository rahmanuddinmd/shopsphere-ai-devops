# IAM for ShopSphere - least privilege, no long-lived keys in the cluster.
#  * EKS Pod Identity roles: AWS Load Balancer Controller, EBS CSI driver, CloudWatch agent,
#    and the ShopSphere backend (Bedrock only).
#  * A Jenkins IAM user with ECR push + EKS describe permissions. Its access key is created
#    manually in the console (never by Terraform, so it never lands in the state file).

module "aws_lb_controller_pod_identity" {
  source  = "terraform-aws-modules/eks-pod-identity/aws"
  version = "~> 2.0"

  name                            = "${var.name}-aws-lbc"
  attach_aws_lb_controller_policy = true

  associations = {
    lbc = {
      cluster_name    = var.cluster_name
      namespace       = "kube-system"
      service_account = "aws-load-balancer-controller"
    }
  }
  tags = var.tags
}

module "ebs_csi_pod_identity" {
  source  = "terraform-aws-modules/eks-pod-identity/aws"
  version = "~> 2.0"

  name                      = "${var.name}-ebs-csi"
  attach_aws_ebs_csi_policy = true
  tags                      = var.tags
}

module "cloudwatch_pod_identity" {
  source  = "terraform-aws-modules/eks-pod-identity/aws"
  version = "~> 2.0"

  name                                       = "${var.name}-cloudwatch"
  attach_aws_cloudwatch_observability_policy = true
  tags                                       = var.tags
}

# Backend pods may ONLY invoke Bedrock models - nothing else.
module "backend_pod_identity" {
  source  = "terraform-aws-modules/eks-pod-identity/aws"
  version = "~> 2.0"

  name                 = "${var.name}-backend-bedrock"
  attach_custom_policy = true
  policy_statements = [{
    sid       = "InvokeBedrockModels"
    actions   = ["bedrock:InvokeModel", "bedrock:InvokeModelWithResponseStream"]
    resources = ["arn:aws:bedrock:*::foundation-model/*", "arn:aws:bedrock:*:*:inference-profile/*"]
  }]

  associations = {
    for ns in var.app_namespaces : ns => {
      cluster_name    = var.cluster_name
      namespace       = ns
      service_account = "shopsphere-backend"
    }
  }
  tags = var.tags
}

# ---------------- Jenkins CI/CD identity ----------------
resource "aws_iam_user" "jenkins" {
  count = var.create_jenkins_user ? 1 : 0
  name  = "${var.name}-jenkins"
  tags  = var.tags
}

data "aws_iam_policy_document" "jenkins" {
  statement {
    sid       = "EcrLogin"
    actions   = ["ecr:GetAuthorizationToken"]
    resources = ["*"]
  }
  statement {
    sid = "EcrPushPull"
    actions = [
      "ecr:BatchCheckLayerAvailability", "ecr:BatchGetImage", "ecr:CompleteLayerUpload",
      "ecr:DescribeImages", "ecr:DescribeRepositories", "ecr:GetDownloadUrlForLayer",
      "ecr:InitiateLayerUpload", "ecr:PutImage", "ecr:UploadLayerPart"
    ]
    resources = var.ecr_repository_arns
  }
  statement {
    sid       = "EksDescribeForKubeconfig"
    actions   = ["eks:DescribeCluster", "eks:ListClusters"]
    resources = ["*"]
  }
  statement {
    sid       = "WhoAmI"
    actions   = ["sts:GetCallerIdentity"]
    resources = ["*"]
  }
}

resource "aws_iam_user_policy" "jenkins" {
  count  = var.create_jenkins_user ? 1 : 0
  name   = "${var.name}-jenkins-cicd"
  user   = aws_iam_user.jenkins[0].name
  policy = data.aws_iam_policy_document.jenkins.json
}
