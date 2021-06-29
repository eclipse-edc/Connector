# Configure the Azure provider
terraform {
  backend "azurerm" {
    resource_group_name  = "dagx-infrastructure"
    storage_account_name = "dagxtstate"
    container_name       = "terraform-state"
    key                  = "terraform.state"
  }
  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = ">= 2.62.1"
    }
    azuread = {
      source  = "hashicorp/azuread"
      version = ">=1.5.0"
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = ">=2.0.3"
      configuration_aliases = [
        kubernetes.nifi,
      kubernetes.atlas]
    }
    helm = {
      source  = "hashicorp/helm"
      version = ">= 2.1.0"
      configuration_aliases = [
        helm.nifi,
      helm.atlas]
    }
    aws = {
      source  = "hashicorp/aws"
      version = "3.45.0"
    }
  }
}


provider "azurerm" {
  features {
    key_vault {
      purge_soft_delete_on_destroy    = true
      recover_soft_deleted_key_vaults = true
    }
  }
}

provider "azuread" {
  # Configuration options
}

//provider "kubernetes" {
//  alias                  = "nifi"
//  host                   = data.azurerm_kubernetes_cluster.nifi.kube_config.0.host
//  client_certificate     = base64decode(data.azurerm_kubernetes_cluster.nifi.kube_config.0.client_certificate)
//  client_key             = base64decode(data.azurerm_kubernetes_cluster.nifi.kube_config.0.client_key)
//  cluster_ca_certificate = base64decode(data.azurerm_kubernetes_cluster.nifi.kube_config.0.cluster_ca_certificate)
//}

provider "kubernetes" {
  alias                  = "atlas"
  host                   = data.azurerm_kubernetes_cluster.atlas.kube_config.0.host
  client_certificate     = base64decode(data.azurerm_kubernetes_cluster.atlas.kube_config.0.client_certificate)
  client_key             = base64decode(data.azurerm_kubernetes_cluster.atlas.kube_config.0.client_key)
  cluster_ca_certificate = base64decode(data.azurerm_kubernetes_cluster.atlas.kube_config.0.cluster_ca_certificate)
}

//provider "helm" {
//  alias = "nifi"
//  kubernetes {
//    host                   = data.azurerm_kubernetes_cluster.nifi.kube_config.0.host
//    client_certificate     = base64decode(data.azurerm_kubernetes_cluster.nifi.kube_config.0.client_certificate)
//    client_key             = base64decode(data.azurerm_kubernetes_cluster.nifi.kube_config.0.client_key)
//    cluster_ca_certificate = base64decode(data.azurerm_kubernetes_cluster.nifi.kube_config.0.cluster_ca_certificate)
//  }
//}

provider "helm" {
  alias = "atlas"
  kubernetes {
    host                   = data.azurerm_kubernetes_cluster.atlas.kube_config.0.host
    client_certificate     = base64decode(data.azurerm_kubernetes_cluster.atlas.kube_config.0.client_certificate)
    client_key             = base64decode(data.azurerm_kubernetes_cluster.atlas.kube_config.0.client_key)
    cluster_ca_certificate = base64decode(data.azurerm_kubernetes_cluster.atlas.kube_config.0.cluster_ca_certificate)
  }
}

//data "azurerm_kubernetes_cluster" "nifi" {
//  depends_on = [
//  module.nifi-cluster]
//  # refresh cluster state before reading
//  name                = local.cluster_name_nifi
//  resource_group_name = local.cluster_name_nifi
//}

data "azurerm_kubernetes_cluster" "atlas" {
  depends_on = [
  module.atlas-cluster]
  name                = local.cluster_name_atlas
  resource_group_name = local.cluster_name_atlas
}

data "azurerm_client_config" "current" {}

resource "azurerm_resource_group" "rg" {
  name     = "${var.resourcesuffix}-resources"
  location = var.location
}

# App registration for the primary identity
resource "azuread_application" "dagx-terraform-app" {
  display_name               = "PrimaryIdentity-${var.resourcesuffix}"
  available_to_other_tenants = false
}

resource "azuread_application_certificate" "dagx-terraform-app-cert" {
  type                  = "AsymmetricX509Cert"
  application_object_id = azuread_application.dagx-terraform-app.id
  value                 = var.CERTIFICATE
  end_date_relative     = "2400h"
}

resource "azuread_service_principal" "dagx-terraform-app-sp" {
  application_id               = azuread_application.dagx-terraform-app.application_id
  app_role_assignment_required = false
  tags = [
  "terraform"]
}


# Keyvault
resource "azurerm_key_vault" "dagx-terraform-vault" {
  name                        = "dagx-${var.resourcesuffix}-vault"
  location                    = azurerm_resource_group.rg.location
  resource_group_name         = azurerm_resource_group.rg.name
  enabled_for_disk_encryption = false
  tenant_id                   = data.azurerm_client_config.current.tenant_id
  soft_delete_retention_days  = 7
  purge_protection_enabled    = false

  sku_name                  = "standard"
  enable_rbac_authorization = true

}

# Role assignment so that the primary identity may access the vault
resource "azurerm_role_assignment" "primary-id" {
  scope                = azurerm_key_vault.dagx-terraform-vault.id
  role_definition_name = "Key Vault Secrets Officer"
  principal_id         = azuread_service_principal.dagx-terraform-app-sp.object_id
}

#Role assignment so that the currently logged in user may access the vault, needed to add secrets
resource "azurerm_role_assignment" "current-user" {
  scope                = azurerm_key_vault.dagx-terraform-vault.id
  role_definition_name = "Key Vault Secrets Officer"
  principal_id         = data.azurerm_client_config.current.object_id
}

#storage account
resource "azurerm_storage_account" "dagxblobstore" {
  name                     = "dagxtfblob"
  resource_group_name      = azurerm_resource_group.rg.name
  location                 = azurerm_resource_group.rg.location
  account_tier             = "Standard"
  account_replication_type = "GRS"
  account_kind             = "BlobStorage"
}

resource "azurerm_storage_container" "src-container" {
  name                  = "src-container"
  storage_account_name  = azurerm_storage_account.dagxblobstore.name
  container_access_type = "private"

}

## KEYVAULT SECRETS
resource "azurerm_key_vault_secret" "atlas-user" {
  name         = "atlas-username"
  value        = "admin"
  key_vault_id = azurerm_key_vault.dagx-terraform-vault.id
  depends_on   = [azurerm_role_assignment.current-user]
}

resource "azurerm_key_vault_secret" "atlas-password" {
  name         = "atlas-password"
  value        = "admin"
  key_vault_id = azurerm_key_vault.dagx-terraform-vault.id
  depends_on   = [azurerm_role_assignment.current-user]
}

resource "azurerm_key_vault_secret" "aws-keyid" {
  name         = "dagx-aws-access-key"
  value        = aws_iam_access_key.dagx_access_key.id
  key_vault_id = azurerm_key_vault.dagx-terraform-vault.id
  depends_on   = [azurerm_role_assignment.current-user]
}

resource "azurerm_key_vault_secret" "aws-secret" {
  name         = "dagx-aws-secret-access-key"
  value        = aws_iam_access_key.dagx_access_key.secret
  key_vault_id = azurerm_key_vault.dagx-terraform-vault.id
  depends_on   = [azurerm_role_assignment.current-user]
}

resource "azurerm_key_vault_secret" "aws-credentials" {
  key_vault_id = azurerm_key_vault.dagx-terraform-vault.id
  name         = "aws-credentials"
  value        = jsonencode({ "accessKeyId" = aws_iam_access_key.dagx_access_key.id, "secretAccessKey" = aws_iam_access_key.dagx_access_key.secret })
  depends_on   = [azurerm_role_assignment.current-user]
}

resource "azurerm_key_vault_secret" "blobstorekey" {
  name         = "${azurerm_storage_account.dagxblobstore.name}-key1"
  value        = azurerm_storage_account.dagxblobstore.primary_access_key
  key_vault_id = azurerm_key_vault.dagx-terraform-vault.id
  depends_on   = [azurerm_role_assignment.current-user]
}

resource "azurerm_key_vault_secret" "nifi-credentials" {
  name         = "nifi-credentials"
  value        = "Basic dGVzdHVzZXJAZ2FpYXguY29tOmdYcHdkIzIwMiE="
  key_vault_id = azurerm_key_vault.dagx-terraform-vault.id
  depends_on   = [azurerm_role_assignment.current-user]
}

# temporarily deploy nifi in a container as well as K8s was unstable
resource "azurerm_container_group" "connector-instance" {
  name                = "dagx-demo-continst"
  location            = azurerm_resource_group.rg.location
  resource_group_name = azurerm_resource_group.rg.name
  os_type             = "Linux"
  ip_address_type     = "public"
  dns_name_label      = "${var.resourcesuffix}-dagx"
  container {
    cpu    = 2
    image  = "ghcr.io/microsoft/data-appliance-gx/dagx-demo:${var.SHORT_SHA}"
    memory = "2"
    name   = "dagx-demo"

    ports {
      port     = 8181
      protocol = "TCP"
    }

    secure_environment_variables = {
      CLIENTID      = azuread_application.dagx-terraform-app.application_id,
      TENANTID      = data.azurerm_client_config.current.tenant_id,
      VAULTNAME     = azurerm_key_vault.dagx-terraform-vault.name,
      ATLAS_URL     = "https://${module.atlas-cluster.public-ip.fqdn}"
      NIFI_URL      = "http://${azurerm_container_group.dagx-nifi.fqdn}:8080/"
      NIFI_FLOW_URL = "http://${azurerm_container_group.dagx-nifi.fqdn}:8888/"
    }

    volume {
      mount_path           = "/cert"
      name                 = "certificates"
      share_name           = "certificates"
      storage_account_key  = var.backend_account_key
      storage_account_name = var.backend_account_name
      read_only            = true
    }
  }
}

resource "azurerm_container_group" "dagx-nifi" {
  location            = azurerm_resource_group.rg.location
  name                = "dagx-nifi-continst"
  os_type             = "Linux"
  resource_group_name = azurerm_resource_group.rg.name
  dns_name_label      = "${var.resourcesuffix}-dagx-nifi"
  container {
    cpu    = 4
    image  = "ghcr.io/microsoft/data-appliance-gx/nifi:latest"
    memory = 4
    name   = "nifi"

    ports {
      port     = 8080
      protocol = "TCP"
    }
    ports {
      port     = 8888
      protocol = "TCP"
    }
  }
}

//module "nifi-cluster" {
//  source       = "./aks-cluster"
//  cluster_name = local.cluster_name_nifi
//  location     = var.location
//  dns          = "dagx-nifi"
//}
//
//module "nifi-deployment" {
//  depends_on = [
//  module.nifi-cluster]
//  source         = "./nifi-deployment"
//  cluster_name   = local.cluster_name_nifi
//  kubeconfig     = data.azurerm_kubernetes_cluster.nifi.kube_config_raw
//  resourcesuffix = var.resourcesuffix
//  tenant_id      = data.azurerm_client_config.current.tenant_id
//  providers = {
//    kubernetes = kubernetes.nifi
//    helm       = helm.nifi
//  }
//  public-ip = module.nifi-cluster.public-ip
//}

module "atlas-cluster" {
  source       = "./aks-cluster"
  cluster_name = local.cluster_name_atlas
  location     = var.location
  dns          = "${var.resourcesuffix}-dagx-atlas"
}
module "atlas-deployment" {
  depends_on = [
  module.atlas-cluster]
  source         = "./atlas-deployment"
  cluster_name   = local.cluster_name_atlas
  kubeconfig     = data.azurerm_kubernetes_cluster.atlas.kube_config_raw
  resourcesuffix = var.resourcesuffix
  tenant_id      = data.azurerm_client_config.current.tenant_id
  providers = {
    kubernetes = kubernetes.atlas
    helm       = helm.atlas
  }
  public-ip = module.atlas-cluster.public-ip
}

