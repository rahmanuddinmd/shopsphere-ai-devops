# syntax=docker/dockerfile:1
# =====================================================================
# Jenkins controller image with every CLI the ShopSphere pipeline needs:
# JDK 21 (base image), Node 22, Docker CLI, Trivy, AWS CLI v2, kubectl, kustomize, Terraform.
# Training setup: builds run on the controller and use the host Docker socket.
# Production: use dedicated agents (Kubernetes/EC2 agents) and never build on the controller.
# =====================================================================
FROM jenkins/jenkins:lts-jdk21

USER 0
ARG NODE_MAJOR=22
ARG KUBECTL_VERSION=v1.35.0
ARG TERRAFORM_VERSION=1.13.4
ARG KUSTOMIZE_VERSION=v5.7.1

RUN apt-get update && apt-get install -y --no-install-recommends \
      ca-certificates curl gnupg unzip jq git lsb-release \
    && install -m 0755 -d /etc/apt/keyrings \
    # Docker CLI
    && curl -fsSL https://download.docker.com/linux/debian/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg \
    && echo "deb [signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/debian $(lsb_release -cs) stable" > /etc/apt/sources.list.d/docker.list \
    # Trivy
    && curl -fsSL https://aquasecurity.github.io/trivy-repo/deb/public.key | gpg --dearmor -o /etc/apt/keyrings/trivy.gpg \
    && echo "deb [signed-by=/etc/apt/keyrings/trivy.gpg] https://aquasecurity.github.io/trivy-repo/deb generic main" > /etc/apt/sources.list.d/trivy.list \
    # Node.js
    && curl -fsSL https://deb.nodesource.com/gpgkey/nodesource-repo.gpg.key | gpg --dearmor -o /etc/apt/keyrings/nodesource.gpg \
    && echo "deb [signed-by=/etc/apt/keyrings/nodesource.gpg] https://deb.nodesource.com/node_${NODE_MAJOR}.x nodistro main" > /etc/apt/sources.list.d/nodesource.list \
    && apt-get update && apt-get install -y --no-install-recommends docker-ce-cli docker-buildx-plugin trivy nodejs \
    && rm -rf /var/lib/apt/lists/*

# AWS CLI v2, kubectl, kustomize, Terraform
RUN curl -fsSL "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o /tmp/awscli.zip \
    && unzip -q /tmp/awscli.zip -d /tmp && /tmp/aws/install && rm -rf /tmp/aws /tmp/awscli.zip \
    && curl -fsSL "https://dl.k8s.io/release/${KUBECTL_VERSION}/bin/linux/amd64/kubectl" -o /usr/local/bin/kubectl \
    && chmod +x /usr/local/bin/kubectl \
    && curl -fsSL "https://github.com/kubernetes-sigs/kustomize/releases/download/kustomize%2F${KUSTOMIZE_VERSION}/kustomize_${KUSTOMIZE_VERSION}_linux_amd64.tar.gz" \
       | tar -xz -C /usr/local/bin kustomize \
    && curl -fsSL "https://releases.hashicorp.com/terraform/${TERRAFORM_VERSION}/terraform_${TERRAFORM_VERSION}_linux_amd64.zip" -o /tmp/tf.zip \
    && unzip -q /tmp/tf.zip -d /usr/local/bin terraform && rm /tmp/tf.zip

# 1000 = the jenkins user of the base image
USER 1000
COPY --chown=jenkins:jenkins plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN jenkins-plugin-cli --plugin-file /usr/share/jenkins/ref/plugins.txt
