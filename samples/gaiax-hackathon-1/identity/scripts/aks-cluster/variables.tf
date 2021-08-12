variable "kubernetes_version" {
  default = "1.19"
}

variable "cluster_name" {
  type = string
}

variable "location" {
  type = string
}

variable "dnsPrefix" {
  type = string
}
