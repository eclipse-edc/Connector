variable "location" {
  description = "geographic location of the Azure resources"
  default     = "westeurope"
  type        = string
}
locals {
  cluster_name = "dagx-${var.resourcesuffix}-cluster"
}

variable "resourcesuffix" {
  description = "identifying string that is used in all azure resources"
}
