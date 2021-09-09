# Configure the Azure provider
terraform {
  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = ">= 2.72.0"
    }
    azuread = {
      source  = "hashicorp/azuread"
      version = "1.6.0"
    }
    aws = {
      source  = "hashicorp/aws"
      version = "3.45.0"
    }
    http = {
      source  = "hashicorp/http"
      version = ">=2.1.0"
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

data "azurerm_client_config" "current" {}
data "azurerm_subscription" "primary" {}


resource "azurerm_resource_group" "core-resourcegroup" {
  name     = "${var.environment}-resources"
  location = var.location
  tags = {
    "Environment" : "EDC"
  }
}

# App registration for the primary identity
resource "azuread_application" "demo-app-id" {
  display_name               = "PrimaryIdentity-${var.environment}"
  available_to_other_tenants = false
}

resource "azuread_application_certificate" "demo-main-identity-cert" {
  type                  = "AsymmetricX509Cert"
  application_object_id = azuread_application.demo-app-id.id
  value                 = var.CERTIFICATE
  end_date_relative     = "2400h"
}

resource "azuread_service_principal" "main-app-sp" {
  application_id               = azuread_application.demo-app-id.application_id
  app_role_assignment_required = false
  tags = [
  "terraform"]
}

# Keyvault
resource "azurerm_key_vault" "main-vault" {
  name                        = "${var.environment}-vault"
  location                    = azurerm_resource_group.core-resourcegroup.location
  resource_group_name         = azurerm_resource_group.core-resourcegroup.name
  enabled_for_disk_encryption = false
  tenant_id                   = data.azurerm_client_config.current.tenant_id
  soft_delete_retention_days  = 7
  purge_protection_enabled    = false

  sku_name                  = "standard"
  enable_rbac_authorization = true

}

# Role assignment so that the primary identity may access the vault
resource "azurerm_role_assignment" "primary-id" {
  scope                = azurerm_key_vault.main-vault.id
  role_definition_name = "Key Vault Secrets Officer"
  principal_id         = azuread_service_principal.main-app-sp.object_id
}

//# Role assignment that the primary identity may provision/deprovision azure resources
//resource "azurerm_role_assignment" "primary-id-arm" {
//  principal_id         = azuread_service_principal.main-app-sp.object_id
//  scope                = data.azurerm_subscription.primary.id
//  role_definition_name = "Contributor"
//}

#Role assignment so that the currently logged in user may access the vault, needed to add secrets
resource "azurerm_role_assignment" "current-user" {
  scope                = azurerm_key_vault.main-vault.id
  role_definition_name = "Key Vault Secrets Officer"
  principal_id         = data.azurerm_client_config.current.object_id
}

#storage account
resource "azurerm_storage_account" "main-blobstore" {
  name                     = "${replace(var.environment, "-", "")}storage"
  resource_group_name      = azurerm_resource_group.core-resourcegroup.name
  location                 = azurerm_resource_group.core-resourcegroup.location
  account_tier             = "Standard"
  account_replication_type = "GRS"
  account_kind             = "StorageV2"
  //allows for blobs, queues, fileshares, etc.
}

# storage container
resource "azurerm_storage_container" "main-blob-container"{

  name = "src-container"
  storage_account_name = azurerm_storage_account.main-blobstore.name
}

# put a file as blob to the storage container
resource "azurerm_storage_blob" "testfile" {
  name = "test-document.txt"
  storage_account_name = azurerm_storage_account.main-blobstore.name
  storage_container_name = azurerm_storage_container.main-blob-container.name
  type = "Block"
  source = "test-document.txt"
}

// primary key for the blob store
resource "azurerm_key_vault_secret" "blobstorekey" {
  name         = "${azurerm_storage_account.main-blobstore.name}-key1"
  value        = azurerm_storage_account.main-blobstore.primary_access_key
  key_vault_id = azurerm_key_vault.main-vault.id
  depends_on = [
  azurerm_role_assignment.current-user]
}

// the AWS access credentials
resource "azurerm_key_vault_secret" "aws-keyid" {
  name         = "dataspaceconnector-aws-access-key"
  value        = aws_iam_access_key.access_key.id
  key_vault_id = azurerm_key_vault.main-vault.id
  depends_on = [
  azurerm_role_assignment.current-user]
}

resource "azurerm_key_vault_secret" "aws-secret" {
  name         = "dataspaceconnector-aws-secret-access-key"
  value        = aws_iam_access_key.access_key.secret
  key_vault_id = azurerm_key_vault.main-vault.id
  depends_on = [
  azurerm_role_assignment.current-user]
}

resource "azurerm_key_vault_secret" "aws-credentials" {
  key_vault_id = azurerm_key_vault.main-vault.id
  name         = "aws-credentials"
  value = jsonencode({
    "accessKeyId"     = aws_iam_access_key.access_key.id,
    "secretAccessKey" = aws_iam_access_key.access_key.secret
  })
  depends_on = [
  azurerm_role_assignment.current-user]
}
