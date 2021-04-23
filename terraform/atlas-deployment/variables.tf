variable "kubernetes-namespace" {
  description = "The namespace for the kubernetes deployment"
  default     = "dagx"
}

variable "chart-dir" {
  description = "The directory where the local atlas helm chart is located"
  default     = "atlas-chart"
}

variable "cluster_name" {
  type = string
}

variable "kubeconfig" {
  type = string
}

variable "resourcesuffix" {
  type = string
}

variable "location" {
  type    = string
  default = "westeurope"
}

variable "tenant_id" {
  type = string
}

variable "atlas_service_name" {
  type    = string
  default = "atlas"
}

variable "atlas_ingress_cert_name" {
  default = "atlas-ingress-tls"
}

variable "public-ip" {
  type = object({
    ip_address = string
    fqdn = string
    domain_name_label = string })
}
