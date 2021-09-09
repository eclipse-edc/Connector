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

variable "environment" {
  description = "identifying string that is used in all azure resources"
}

variable "CERTIFICATE" {
  type        = string
  description = "private key file for the primary azure app SP"
}