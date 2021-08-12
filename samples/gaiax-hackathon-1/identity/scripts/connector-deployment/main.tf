terraform {
  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = ">= 2.66.0"
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = ">= 2.0.3"
    }
    helm = {
      source  = "hashicorp/helm"
      version = ">= 2.1.0"
    }
    tls = {

    }
  }
}

resource "tls_private_key" "connector-ingress-key" {
  algorithm = "ECDSA"
}

resource "kubernetes_namespace" "connector" {
  metadata {
    name = "${var.environment}-ns"
  }
}

resource "local_file" "kubeconfig" {
  content  = var.kubeconfig
  filename = "${path.root}/kubeconfig"
}

resource "kubernetes_secret" "connector-cert-secret" {
  metadata {
    name      = "blobstore-key"
    namespace = kubernetes_namespace.connector.metadata[0].name
  }
  type = "Opaque"
  data = {
    azurestorageaccountname = var.certificate_mount_config.accountName
    azurestorageaccountkey  = var.certificate_mount_config.accountKey
  }
}

resource "kubernetes_secret" "docker_repo_secret" {
  metadata {
    name      = "docker-cfg"
    namespace = kubernetes_namespace.connector.metadata[0].name
  }

  data = {
    ".dockerconfigjson" = <<DOCKER
{
  "auths": {
    "${var.docker_repo_url}": {
      "auth": "${base64encode("${var.docker_repo_username}:${var.docker_repo_password}")}"
    }
  }
}
DOCKER
  }

  type = "kubernetes.io/dockerconfigjson"
}

resource "kubernetes_deployment" "connector-deployment" {
  metadata {
    name      = var.connector_service_name
    namespace = kubernetes_namespace.connector.id
  }
  spec {
    replicas = var.num_replicas
    selector {
      match_labels = {
        app : var.connector_service_name
      }
    }
    template {
      metadata {
        labels = {
          app : var.connector_service_name
        }
      }
      spec {
        image_pull_secrets {
          name = kubernetes_secret.docker_repo_secret.metadata[0].name
        }
        container {
          name              = "connector"
          image             = "ghcr.io/microsoft/edc-connector-demo/edc-demo-provider:latest"
          image_pull_policy = "Always"
          env {
            name  = "CLIENTID"
            value = var.container_environment.clientId
          }
          env {
            name  = "TENANTID"
            value = var.container_environment.tenantId
          }
          env {
            name  = "VAULTNAME"
            value = var.container_environment.vaultName
          }
          env {
            name  = "COSMOS_ACCOUNT"
            value = var.container_environment.cosmosAccount
          }
          env {
            name  = "COSMOS_DB"
            value = var.container_environment.cosmosDb
          }
          env {
            name  = "TOPIC_NAME"
            value = var.events.topic_name
          }
          env {
            name  = "TOPIC_ENDPOINT"
            value = var.events.topic_endpoint
          }
          port {
            container_port = 8181
            host_port      = 8181
            protocol       = "TCP"
          }
          volume_mount {
            mount_path = "/cert"
            name       = "certificates"
            read_only  = true
          }
        }
        volume {
          name = "certificates"
          azure_file {
            secret_name = kubernetes_secret.connector-cert-secret.metadata[0].name
            share_name  = "certificates"
          }
        }
      }
    }
  }
}

resource "kubernetes_service" "connector-cluster-ip" {
  metadata {
    name      = var.connector_service_name
    namespace = kubernetes_namespace.connector.id
  }
  spec {
    type = "ClusterIP"
    port {
      port = 8181
    }
    selector = {
      app : var.connector_service_name
    }
  }
}

output "connector-cluster-namespace" {
  value = kubernetes_namespace.connector.metadata[0].name
}
