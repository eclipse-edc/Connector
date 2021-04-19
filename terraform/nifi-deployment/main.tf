terraform {
  required_providers {
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = ">= 2.0.3"
    }
    helm = {
      source  = "hashicorp/helm"
      version = ">= 2.1.0"
    }
  }
}

resource "kubernetes_namespace" "nifi" {
  metadata {
    name = var.kubernetes-namespace
    labels = {
      "environment" = "test"
    }
  }
}

# App registration for the loadbalancer
resource "azuread_application" "dagx-terraform-nifi-app" {
  display_name               = "Dagx-${var.resourcesuffix}-Nifi"
  available_to_other_tenants = false
  reply_urls                 = ["https://dagx-${var.resourcesuffix}.${var.location}.cloudapp.azure.com/nifi-api/access/oidc/callback"]
}

resource "random_password" "password" {
  length           = 16
  special          = true
  override_special = "_%@"
}

resource "azuread_application_password" "dagx-terraform-nifi-app-secret" {
  application_object_id = azuread_application.dagx-terraform-nifi-app.id
  end_date              = "2099-01-01T01:02:03Z"
  value                 = random_password.password.result
}


resource "azuread_service_principal" "dagx-terraform-nifi-app-sp" {
  application_id               = azuread_application.dagx-terraform-nifi-app.application_id
  app_role_assignment_required = false
  tags                         = ["terraform"]
}


resource "helm_release" "nifi" {
  name  = "dagx-nifi-release"
  chart = var.chart-dir
  values = [
    file("nifi-chart/openid-values.yaml"),
    file("nifi-chart/secured-values-with-nifi-toolkit.yaml")]
  namespace       = kubernetes_namespace.nifi.metadata[0].name
  cleanup_on_fail = true
  set {
    name  = "nifi.authentication.openid.clientId"
    value = azuread_application.dagx-terraform-nifi-app.application_id
    type  = "string"
  }
  set {
    name  = "nifi.authentication.openid.clientSecret"
    value = azuread_application_password.dagx-terraform-nifi-app-secret.value
  }
  set {
    name  = "nifi.authentication.openid.discoveryUrl"
    value = "https://login.microsoftonline.com/${var.tenant_id}/v2.0/.well-known/openid-configuration"
  }
  set {
    name  = "nifi.bootstrapConf.jvmMinMemory"
    value = "3g"
  }
  set {
    name  = "nifi.bootstrapConf.jvmMaxMemory"
    value = "3g"
  }
  set {
    name  = "resources.requests.memory"
    value = "1Gi"
  }
  set {
    name  = "ingress.enabled"
    value = true
  }
  set {
    name  = "nifi.properties.webProxyHost"
    value = "dagx-${var.resourcesuffix}.${var.location}.cloudapp.azure.com"
  }
  set {
    name  = "initUsers.enabled"
    value = true
  }
  set {
    name  = "admins"
    value = "paul.latzelsperger@beardyinc.com"
  }
  set {
    name  = "uiUsers"
    value = "paul.latzelsperger@beardyinc.com"
  }
}

resource "local_file" "kubeconfig" {
  content  = var.kubeconfig
  filename = "${path.root}/kubeconfig"
}
