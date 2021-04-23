terraform {
  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = ">= 2.42"
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

resource "tls_private_key" "atlas-ingress" {
  algorithm = "ECDSA"
}

resource "tls_self_signed_cert" "atlas-ingress" {
  allowed_uses = [
    "server_auth",
    "digital_signature"
  ]
  key_algorithm         = tls_private_key.atlas-ingress.algorithm
  private_key_pem       = tls_private_key.atlas-ingress.private_key_pem
  validity_period_hours = 72
  early_renewal_hours   = 12
  subject {
    common_name  = var.public-ip.fqdn
    organization = "Gaia-X Data Appliance"
  }
  dns_names = [
  var.public-ip.fqdn]
}

resource "kubernetes_namespace" "atlas" {
  metadata {
    name = "${var.resourcesuffix}-atlas"
  }
}

resource "helm_release" "ingress-controller" {
  chart      = "ingress-nginx"
  name       = "atlas-ingress-controller"
  namespace  = kubernetes_namespace.atlas.metadata[0].name
  repository = "https://kubernetes.github.io/ingress-nginx"

  set {
    name  = "controller.replicaCount"
    value = "2"
  }
  set {
    name  = "controller.service.loadBalancerIP"
    value = var.public-ip.ip_address
  }
  set {
    name  = "controller.service.annotations.service.beta.kubernetes.io/azure-dns-label-name"
    value = var.public-ip.domain_name_label
  }
}

resource "helm_release" "atlas" {
  name            = var.atlas_service_name
  chart           = "./atlas-chart"
  values          = []
  namespace       = kubernetes_namespace.atlas.metadata[0].name
  cleanup_on_fail = true
  set {
    name  = "service.type"
    value = "ClusterIP"
  }
  //  set {
  //    name  = "ingress.enabled"
  //    value = "false"
  //  }
  //  set {
  //    name  = "ingress.hosts[0]"
  //    value = azurerm_public_ip.aks-cluster-public-ip.fqdn
  //  }
}

resource "kubernetes_secret" "atlas-ingress-tls" {
  metadata {
    namespace = kubernetes_namespace.atlas.metadata[0].name
    name      = var.atlas_ingress_cert_name
  }
  data = {
    "tls.crt" = tls_private_key.atlas-ingress.public_key_pem
    "tls.key" = tls_private_key.atlas-ingress.private_key_pem
  }
}

resource "kubernetes_ingress" "ingress-route" {
  metadata {
    name      = "atlas-ingress"
    namespace = kubernetes_namespace.atlas.metadata[0].name
    annotations = {
      "nginx.ingress.kubernetes.io/ssl-redirect" : "false"
      "nginx.ingress.kubernetes.io/use-regex" : "true"
      //      "nginx.ingress.kubernetes.io/rewrite-target" : "/$2"
    }
  }
  spec {
    rule {
      http {
        path {
          backend {
            service_name = "${var.atlas_service_name}-atlas"
            service_port = 21000
          }
          path = "/"
        }
      }
    }
    tls {
      hosts       = [var.public-ip.fqdn]
      secret_name = kubernetes_secret.atlas-ingress-tls.metadata[0].name
    }
  }
}

resource "local_file" "kubeconfig" {
  content  = var.kubeconfig
  filename = "${path.root}/kubeconfig"
}

output "atlas-cluster-namespace" {
  value = kubernetes_namespace.atlas.metadata[0].name
}
