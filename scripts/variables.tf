variable "location" {
  description = "geographic location of the Azure resources"
  default     = "westeurope"
  type        = string
}
locals {
  cluster_name_nifi  = "${var.resourcesuffix}-nifi-cluster"
  cluster_name_atlas = "${var.resourcesuffix}-atlas-cluster"
}

variable "resourcesuffix" {
  description = "identifying string that is used in all azure resources"
}

variable "aws-key-id"{
  description = "The Access Key ID of your AWS IAM user"
}
variable "aws-secret-key" {
  description = "The Secret Key for your AWS IAM user"
}

variable "SHORT_SHA" {
  type        = string
  description = "short commit SHA of the current HEAD"
  default     = "latest"
}
