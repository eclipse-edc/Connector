# Configure the Azure provider
terraform {
  backend "azurerm" {
    resource_group_name = "edc-infrastructure"
    storage_account_name = "edcstate"
    container_name = "terraform-state"
    key = "terraform.state"
  }
  required_providers {
    azurerm = {
      source = "hashicorp/azurerm"
      version = ">= 2.66.0"
    }
    azuread = {
      source = "hashicorp/azuread"
      version = ">=1.6.0"
    }
    kubernetes = {
      source = "hashicorp/kubernetes"
      version = ">=2.0.3"
    }
    helm = {
      source = "hashicorp/helm"
      version = ">= 2.1.0"
    }
    aws = {
      source = "hashicorp/aws"
      version = "3.45.0"
    }
    http = {
      source = "hashicorp/http"
      version = ">=2.1.0"
    }
  }
}


provider "azurerm" {
  features {
    key_vault {
      purge_soft_delete_on_destroy = true
      recover_soft_deleted_key_vaults = true
    }
  }
}

provider "azuread" {
  # Configuration options
}

provider "kubernetes" {
  alias = "connector"
  host = data.azurerm_kubernetes_cluster.connector.kube_config.0.host
  client_certificate = base64decode(data.azurerm_kubernetes_cluster.connector.kube_config.0.client_certificate)
  client_key = base64decode(data.azurerm_kubernetes_cluster.connector.kube_config.0.client_key)
  cluster_ca_certificate = base64decode(data.azurerm_kubernetes_cluster.connector.kube_config.0.cluster_ca_certificate)
}

provider "helm" {
  alias = "connector"
  kubernetes {
    host = data.azurerm_kubernetes_cluster.connector.kube_config.0.host
    client_certificate = base64decode(data.azurerm_kubernetes_cluster.connector.kube_config.0.client_certificate)
    client_key = base64decode(data.azurerm_kubernetes_cluster.connector.kube_config.0.client_key)
    cluster_ca_certificate = base64decode(data.azurerm_kubernetes_cluster.connector.kube_config.0.cluster_ca_certificate)
  }
}

data "azurerm_kubernetes_cluster" "connector" {
  depends_on = [
    module.connector-cluster]
  name = local.cluster_name_connector
  resource_group_name = local.cluster_name_connector
}

data "azurerm_client_config" "current" {}

resource "azurerm_resource_group" "core-resourcegroup" {
  name = "${var.environment}-resources"
  location = var.location
}

# App registration for the primary identity
resource "azuread_application" "edc-primary-app" {
  display_name = "PrimaryIdentity-${var.environment}"
  available_to_other_tenants = false
}

resource "azuread_application_certificate" "edc-primary-app-cert" {
  type = "AsymmetricX509Cert"
  application_object_id = azuread_application.edc-primary-app.id
  value = var.CERTIFICATE
  end_date_relative = "2400h"
}

resource "azuread_service_principal" "edc-primary-app-sp" {
  application_id = azuread_application.edc-primary-app.application_id
  app_role_assignment_required = false
  tags = [
    "terraform"]
}


# Keyvault
resource "azurerm_key_vault" "edc-primary-vault" {
  name = "edc-${var.environment}-vault"
  location = azurerm_resource_group.core-resourcegroup.location
  resource_group_name = azurerm_resource_group.core-resourcegroup.name
  enabled_for_disk_encryption = false
  tenant_id = data.azurerm_client_config.current.tenant_id
  soft_delete_retention_days = 7
  purge_protection_enabled = false

  sku_name = "standard"
  enable_rbac_authorization = true

}

# Role assignment so that the primary identity may access the vault
resource "azurerm_role_assignment" "primary-id" {
  scope = azurerm_key_vault.edc-primary-vault.id
  role_definition_name = "Key Vault Secrets Officer"
  principal_id = azuread_service_principal.edc-primary-app-sp.object_id
}

#Role assignment so that the currently logged in user may access the vault, needed to add secrets
resource "azurerm_role_assignment" "current-user" {
  scope = azurerm_key_vault.edc-primary-vault.id
  role_definition_name = "Key Vault Secrets Officer"
  principal_id = data.azurerm_client_config.current.object_id
}

#storage account
resource "azurerm_storage_account" "edcblobstore" {
  name = "edctfblob"
  resource_group_name = azurerm_resource_group.core-resourcegroup.name
  location = azurerm_resource_group.core-resourcegroup.location
  account_tier = "Standard"
  account_replication_type = "GRS"
  account_kind = "BlobStorage"
}

resource "azurerm_storage_container" "src-container" {
  name = "src-container"
  storage_account_name = azurerm_storage_account.edcblobstore.name
  container_access_type = "private"

}

## KEYVAULT SECRETS
resource "azurerm_key_vault_secret" "aws-keyid" {
  name = "edc-aws-access-key"
  value = aws_iam_access_key.edc_access_key.id
  key_vault_id = azurerm_key_vault.edc-primary-vault.id
  depends_on = [
    azurerm_role_assignment.current-user]
}

resource "azurerm_key_vault_secret" "aws-secret" {
  name = "edc-aws-secret-access-key"
  value = aws_iam_access_key.edc_access_key.secret
  key_vault_id = azurerm_key_vault.edc-primary-vault.id
  depends_on = [
    azurerm_role_assignment.current-user]
}

resource "azurerm_key_vault_secret" "aws-credentials" {
  key_vault_id = azurerm_key_vault.edc-primary-vault.id
  name = "aws-credentials"
  value = jsonencode({
    "accessKeyId" = aws_iam_access_key.edc_access_key.id,
    "secretAccessKey" = aws_iam_access_key.edc_access_key.secret
  })
  depends_on = [
    azurerm_role_assignment.current-user]
}

resource "azurerm_key_vault_secret" "blobstorekey" {
  name = "${azurerm_storage_account.edcblobstore.name}-key1"
  value = azurerm_storage_account.edcblobstore.primary_access_key
  key_vault_id = azurerm_key_vault.edc-primary-vault.id
  depends_on = [
    azurerm_role_assignment.current-user]
}

module "connector-cluster" {
  source = "./aks-cluster"
  cluster_name = local.cluster_name_connector
  dnsPrefix = "${var.environment}-connector"
  location = var.location
}

module "connector-deployment" {
  depends_on = [
    module.connector-cluster]
  source = "./connector-deployment"
  cluster_name = local.cluster_name_connector
  kubeconfig = data.azurerm_kubernetes_cluster.connector.kube_config_raw
  environment = var.environment
  tenant_id = data.azurerm_client_config.current.tenant_id
  providers = {
    kubernetes = kubernetes.connector
    helm = helm.connector
  }
  public-ip = module.connector-cluster.public-ip
  container_environment = {
    clientId = azuread_application.edc-primary-app.application_id,
    tenantId = data.azurerm_client_config.current.tenant_id,
    vaultName = azurerm_key_vault.edc-primary-vault.name,
    cosmosAccount = azurerm_cosmosdb_account.edc-cosmos.name
    cosmosDb = azurerm_cosmosdb_sql_database.edc-database.name
  }
  certificate_mount_config = {
    accountName = var.backend_account_name
    accountKey = var.backend_account_key
  }
  events = {
    topic_name = azurerm_eventgrid_topic.edc-topic.name
    topic_endpoint = azurerm_eventgrid_topic.edc-topic.endpoint
  }
}

