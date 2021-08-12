//resource "tls_private_key" "connector-ingress-pk" {
//  algorithm = "ECDSA"
//}
//
//resource "tls_self_signed_cert" "connector-ingress-cert" {
//  allowed_uses = [
//    "server_auth",
//    "digital_signature"
//  ]
//  key_algorithm         = tls_private_key.connector-ingress-pk.algorithm
//  private_key_pem       = tls_private_key.connector-ingress-pk.private_key_pem
//  validity_period_hours = 72
//  early_renewal_hours   = 12
//  subject {
//    common_name  = var.public-ip.fqdn
//    organization = "Gaia-X Data Appliance"
//  }
//  dns_names = [
//    var.public-ip.fqdn]
//}
//
//resource "kubernetes_secret" "connector-ingress-secret" {
//  metadata {
//    namespace = kubernetes_namespace.connector.metadata[0].name
//    name      = var.connector_ingress_cert_name
//  }
//  data = {
//    "tls.crt" = tls_private_key.connector-ingress-pk.public_key_pem
//    "tls.key" = tls_private_key.connector-ingress-pk.private_key_pem
//  }
//}
//
resource "kubernetes_ingress" "connector-ingress-route" {
  metadata {
    name      = "connector-ingress"
    namespace = kubernetes_namespace.connector.metadata[0].name
    annotations = {
      "kubernetes.io/ingress.class" : "nginx"
      "nginx.ingress.kubernetes.io/ssl-redirect" : "false"
      "nginx.ingress.kubernetes.io/use-regex" : "true"
      "nginx.ingress.kubernetes.io/rewrite-target" : "/$1"
    }
  }
  spec {
    rule {
      http {
        path {
          path = "/(.*)"
          backend {
            service_name = var.connector_service_name
            service_port = 8181
          }
        }
      }
    }
    //    tls {
    //      hosts       = [var.public-ip.fqdn]
    //      secret_name = kubernetes_secret.connector-ingress-secret.metadata[0].name
    //    }
  }
}

resource "helm_release" "ingress-controller" {
  chart      = "ingress-nginx"
  name       = "connector-ingress-controller"
  namespace  = kubernetes_namespace.connector.metadata[0].name
  repository = "https://kubernetes.github.io/ingress-nginx"

  set {
    name  = "controller.replicaCount"
    value = "1"
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