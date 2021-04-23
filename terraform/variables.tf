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
