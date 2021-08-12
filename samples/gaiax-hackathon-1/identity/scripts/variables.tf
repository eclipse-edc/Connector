variable "location" {
  description = "geographic location of the Azure resources"
  default     = "westeurope"
  type        = string
}

variable "aws_region" {
  description = "geographic location of the AWS resources"
  default     = "us-east-1"
  type        = string
}
locals {
  provider_cluster_name = "${var.environment}-bmw-cluster"
}

variable "environment" {
  description = "identifying string that is used in all azure resources"
}

variable "SHORT_SHA" {
  type        = string
  description = "short commit SHA of the current HEAD"
  default     = "latest"
}

variable "backend_account_name" {
  type        = string
  description = "A storage account where the Terraform state and certificates etc. are stored"
  default     = "edcstate"
}

variable "backend_account_key" {
  type        = string
  description = "Access key of the storage account that holds the terraform state and the certificate file share."
}

variable "CERTIFICATE" {
  type        = string
  description = "private key file for the primary azure app SP"
}

variable "docker_repo_password" {
  type = string
}
variable "docker_repo_username" {
  type = string
}
variable "docker_repo_url" {
  type    = string
  default = "ghcr.io"
}