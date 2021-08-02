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
  cluster_name_nifi      = "${var.environment}-nifi-cluster"
  cluster_name_atlas     = "${var.environment}-atlas-cluster"
  cluster_name_connector = "${var.environment}-connector-cluster"
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
  default     = "dagxtstate"
}

variable "backend_account_key" {
  type        = string
  description = "Access key of the storage account that holds the terraform state and the certificate file share."
}

variable "CERTIFICATE" {
  type        = string
  description = "private key file for the primary azure app SP"
}