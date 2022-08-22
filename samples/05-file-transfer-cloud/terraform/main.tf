# Configure the Azure provider
terraform {
  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "2.88.1"
    }
    azuread = {
      source  = "hashicorp/azuread"
      version = "2.12.0"
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
  display_name = "PrimaryIdentity-${var.environment}"
}

# Allow the app to authenticate with the generated principal
resource "azuread_application_certificate" "demo-main-identity-cert" {
  type                  = "AsymmetricX509Cert"
  application_object_id = azuread_application.demo-app-id.id
  value                 = azurerm_key_vault_certificate.demo-main-identity-cert.certificate_data_base64
  end_date              = azurerm_key_vault_certificate.demo-main-identity-cert.certificate_attribute[0].expires
  start_date            = azurerm_key_vault_certificate.demo-main-identity-cert.certificate_attribute[0].not_before
}

# Generate a service principal
resource "azuread_service_principal" "main-app-sp" {
  application_id               = azuread_application.demo-app-id.application_id
  app_role_assignment_required = false
  tags = [
  "terraform"]
}

# Create central Key Vault for storing generated identity information and credentials
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

#Role assignment so that the currently logged in user may access the vault, needed to add certificates
resource "azurerm_role_assignment" "current-user-certificates" {
  scope                = azurerm_key_vault.main-vault.id
  role_definition_name = "Key Vault Certificates Officer"
  principal_id         = data.azurerm_client_config.current.object_id
}

#Role assignment so that the currently logged in user may access the vault, needed to add secrets
resource "azurerm_role_assignment" "current-user-secrets" {
  scope                = azurerm_key_vault.main-vault.id
  role_definition_name = "Key Vault Secrets Officer"
  principal_id         = data.azurerm_client_config.current.object_id
}

# Generate a certificate to be used by the generated principal
resource "azurerm_key_vault_certificate" "demo-main-identity-cert" {
  name         = "demo-app-id-certificate"
  key_vault_id = azurerm_key_vault.main-vault.id

  certificate_policy {
    issuer_parameters {
      name = "Self"
    }

    key_properties {
      exportable = true
      key_size   = 2048
      key_type   = "RSA"
      reuse_key  = true
    }

    lifetime_action {
      action {
        action_type = "AutoRenew"
      }

      trigger {
        days_before_expiry = 30
      }
    }

    secret_properties {
      content_type = "application/x-pkcs12"
    }

    x509_certificate_properties {
      # Server Authentication = 1.3.6.1.5.5.7.3.1
      # Client Authentication = 1.3.6.1.5.5.7.3.2
      extended_key_usage = ["1.3.6.1.5.5.7.3.1"]

      key_usage = [
        "cRLSign",
        "dataEncipherment",
        "digitalSignature",
        "keyAgreement",
        "keyCertSign",
        "keyEncipherment",
      ]

      subject            = "CN=${azurerm_resource_group.core-resourcegroup.name}"
      validity_in_months = 12
    }
  }
  depends_on = [
    azurerm_role_assignment.current-user-certificates
  ]
}

# Retrieve the Certificate from the Key Vault.
# Note that the data source is actually a Certificate in Key Vault, and not a Secret.
# However this actually works, and retrieves the Certificate base64 encoded.
# An advantage of this method is that the "Key Vault Secrets User" (read-only)
# role is then sufficient to export the certificate.
# This is documented at https://docs.microsoft.com/azure/key-vault/certificates/how-to-export-certificate.
data "azurerm_key_vault_secret" "certificate" {
  name         = azurerm_key_vault_certificate.demo-main-identity-cert.name
  key_vault_id = azurerm_key_vault.main-vault.id
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
resource "azurerm_storage_container" "main-blob-container" {

  name                 = "src-container"
  storage_account_name = azurerm_storage_account.main-blobstore.name
}

# put a file as blob to the storage container
resource "azurerm_storage_blob" "testfile" {
  name                   = "test-document.txt"
  storage_account_name   = azurerm_storage_account.main-blobstore.name
  storage_container_name = azurerm_storage_container.main-blob-container.name
  type                   = "Block"
  source                 = "test-document.txt"
}

// primary key for the blob store
resource "azurerm_key_vault_secret" "blobstorekey" {
  name         = "${azurerm_storage_account.main-blobstore.name}-key1"
  value        = azurerm_storage_account.main-blobstore.primary_access_key
  key_vault_id = azurerm_key_vault.main-vault.id
  depends_on = [
  azurerm_role_assignment.current-user-secrets]
}

// the AWS access credentials
resource "azurerm_key_vault_secret" "aws-keyid" {
  name         = "edc-aws-access-key"
  value        = aws_iam_access_key.access_key.id
  key_vault_id = azurerm_key_vault.main-vault.id
  depends_on = [
  azurerm_role_assignment.current-user-secrets]
}

resource "azurerm_key_vault_secret" "aws-secret" {
  name         = "edc-aws-secret-access-key"
  value        = aws_iam_access_key.access_key.secret
  key_vault_id = azurerm_key_vault.main-vault.id
  depends_on = [
  azurerm_role_assignment.current-user-secrets]
}
