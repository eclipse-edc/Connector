terraform {
  required_providers {
    kubernetes = {
      source = "hashicorp/kubernetes"
      version = ">= 2.0.3"
    }
    helm = {
      source = "hashicorp/helm"
      version = ">= 2.1.0"
    }
  }
}

resource "kubernetes_namespace" "nifi" {
  metadata {
    name = "${var.resourcesuffix}-nifi"
  }
}

# the ingress + ingress route for the nifi cluster
resource "helm_release" "ingress-controller" {
  chart      = "ingress-nginx"
  name       = "nifi-ingress-controller"
  namespace  = kubernetes_namespace.nifi.metadata[0].name
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
resource "kubernetes_ingress" "ingress-route" {
  metadata {
    name      = "nifi-ingress"
    namespace = kubernetes_namespace.nifi.metadata[0].name
    annotations = {
      "kubernetes.io/ingress.class": "nginx"
      "meta.helm.sh/release-name": "nifi-test"
      "meta.helm.sh/release-namespace": kubernetes_namespace.nifi.metadata[0].name
      "nginx.ingress.kubernetes.io/affinity": "cookie"
      "nginx.ingress.kubernetes.io/affinity-mode": "affinityMode"
      "nginx.ingress.kubernetes.io/backend-protocol": "HTTPS"
      "nginx.ingress.kubernetes.io/proxy-ssl-server-name": "on"
    }
  }
  spec {
    rule {
      host = var.public-ip.fqdn
      http {
        path {
          backend {
            service_name = var.nifi_service_name
            service_port = 443
          }
          path = "/"
        }
        path {
          backend {
            service_name = var.nifi_service_name
            service_port = 8888
          }
          path = "/contentListener"
        }
      }
    }
//    tls {
//      hosts       = [var.public-ip.fqdn]
//      secret_name = kubernetes_secret.atlas-ingress-tls.metadata[0].name
//    }
  }
}

# App registration for the loadbalancer
resource "azuread_application" "dagx-terraform-nifi-app" {
  display_name = "Dagx-${var.resourcesuffix}-Nifi"
  available_to_other_tenants = false
  reply_urls = [
    "https://${var.public-ip.fqdn}/nifi-api/access/oidc/callback"]
}

resource "random_password" "password" {
  length = 16
  special = true
  override_special = "_%@"
}

resource "azuread_application_password" "dagx-terraform-nifi-app-secret" {
  application_object_id = azuread_application.dagx-terraform-nifi-app.id
  end_date = "2099-01-01T01:02:03Z"
  value = random_password.password.result
}

resource "azuread_service_principal" "dagx-terraform-nifi-app-sp" {
  application_id = azuread_application.dagx-terraform-nifi-app.application_id
  app_role_assignment_required = false
  tags = [
    "terraform"]
}

resource "helm_release" "nifi" {
  name = var.nifi_service_name
  chart = var.chart-dir
  values = [
    file("nifi-chart/openid-values.yaml"),
    file("nifi-chart/secured-values-with-nifi-toolkit.yaml")]
  namespace = kubernetes_namespace.nifi.metadata[0].name
  cleanup_on_fail = true
  set {
    name = "nifi.authentication.openid.clientId"
    value = azuread_application.dagx-terraform-nifi-app.application_id
  }
  set {
    name = "nifi.authentication.openid.clientSecret"
    value = azuread_application_password.dagx-terraform-nifi-app-secret.value
  }
  set {
    name = "nifi.authentication.openid.discoveryUrl"
    value = "https://login.microsoftonline.com/${var.tenant_id}/v2.0/.well-known/openid-configuration"
  }
  set {
    name = "nifi.bootstrapConf.jvmMinMemory"
    value = "3g"
  }
  set {
    name = "nifi.bootstrapConf.jvmMaxMemory"
    value = "3g"
  }
  set {
    name = "resources.requests.memory"
    value = "1Gi"
  }
//  set {
//    name = "ingress.enabled"
//    value = false
//  }
  set {
    name = "nifi.properties.webProxyHost"
    value = var.public-ip.fqdn
  }
  set {
    name = "initUsers.enabled"
    value = true
  }
  set {
    name = "admins"
    value = "paul.latzelsperger@beardyinc.com"
  }
  set {
    name = "uiUsers"
    value = "paul.latzelsperger@beardyinc.com"
  }
}

resource "local_file" "kubeconfig" {
  content = var.kubeconfig
  filename = "${path.root}/kubeconfig"
}

output "nifi-cluster-namespace" {
  value = kubernetes_namespace.nifi.metadata[0].name
}
