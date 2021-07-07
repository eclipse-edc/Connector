variable "kubernetes-namespace" {
  description = "The namespace for the kubernetes deployment"
  default = "dagx"
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
  type = string
  default = "westeurope"
}

variable "tenant_id" {
  type = string
}

variable "connector_service_name" {
  type = string
  default = "connector-demo"
}

variable "connector_ingress_cert_name" {
  default = "connector-ingress-tls"
}
variable "container_environment" {
  type = object({
    clientId = string
    tenantId = string
    vaultName = string
    atlasUrl = string
    nifiUrl = string
    nifiFlowUrl = string
    cosmosAccount = string
    cosmosDb = string
  })
}

variable "certificate_mount_config" {
  type = object({
    accountName = string
    accountKey = string
  })

}

variable "public-ip" {
  type = object({
    ip_address = string
    fqdn = string
    domain_name_label = string
  })
}

variable "events" {
  type = object({
    topic_name = string
    topic_endpoint = string
  })
}
