variable "kubernetes-namespace" {
  description = "The namespace for the kubernetes deployment"
  default     = "edc-demo"
}

variable "cluster_name" {
  type = string
}

variable "kubeconfig" {
  type = string
}

variable "environment" {
  type = string
}

variable "location" {
  type    = string
  default = "westeurope"
}

variable "tenant_id" {
  type = string
}

variable "connector_service_name" {
  type    = string
  default = "provider-connector"
}

variable "connector_ingress_cert_name" {
  default = "connector-ingress-tls"
}
variable "container_environment" {
  type = object({
    clientId      = string
    tenantId      = string
    vaultName     = string
    cosmosAccount = string
    cosmosDb      = string
  })
}

variable "certificate_mount_config" {
  type = object({
    accountName = string
    accountKey  = string
  })

}

variable "public-ip" {
  type = object({
    ip_address        = string
    fqdn              = string
    domain_name_label = string
  })
}

variable "events" {
  type = object({
    topic_name     = string
    topic_endpoint = string
  })
}

variable "num_replicas" {
  default     = 1
  description = "The number of connector replicas needed"
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