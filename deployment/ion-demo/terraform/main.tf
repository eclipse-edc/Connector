# Configure the Azure provider
terraform {
  backend "azurerm" {
    resource_group_name  = "edc-infrastructure"
    storage_account_name = "edcstate"
    container_name       = "terraform-state-hackathon"
    key                  = "terraform-paul.state"
  }
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
      recover_soft_deleted_key_vaults = false
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

# Role assignment that the primary identity may provision/deprovision azure resources
resource "azurerm_role_assignment" "primary-id-arm" {
  principal_id         = azuread_service_principal.main-app-sp.object_id
  scope                = data.azurerm_subscription.primary.id
  role_definition_name = "Contributor"
}

# Role assignment so that the currently logged in user may access the vault, needed to add secrets
resource "azurerm_role_assignment" "current-user" {
  scope                = azurerm_key_vault.main-vault.id
  role_definition_name = "Key Vault Secrets Officer"
  principal_id         = data.azurerm_client_config.current.object_id
}


# registration service = ion crawler
resource "azurerm_container_group" "registration-service" {
  name                = "gaiax-registration-service"
  location            = azurerm_resource_group.core-resourcegroup.location
  resource_group_name = azurerm_resource_group.core-resourcegroup.name
  os_type             = "Linux"
  ip_address_type     = "public"
  dns_name_label      = "${var.environment}-reg-svc"
  image_registry_credential {
    password = var.docker_repo_password
    server   = var.docker_repo_url
    username = var.docker_repo_username
  }
  container {
    cpu   = 2
    image = "${var.docker_repo_url}/beardyinc/dataspaceconnector/gx-reg-svc:latest"
    //    image  = "beardyinc/gx-reg-svc:latest"
    memory = "2"
    name   = "gx-reg-svc"

    ports {
      port     = 8181
      protocol = "TCP"
    }

    environment_variables = {
      CLIENTID       = azuread_application.demo-app-id.application_id,
      TENANTID       = data.azurerm_client_config.current.tenant_id,
      VAULTNAME      = azurerm_key_vault.main-vault.name,
      CONNECTOR_NAME = "gx-reg-svc"
      TOPIC_NAME     = azurerm_eventgrid_topic.control-topic.name
      TOPIC_ENDPOINT = azurerm_eventgrid_topic.control-topic.endpoint
      ION_URL        = "http://gx-ion-node.westeurope.cloudapp.azure.com:3000/"
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

# connector that acts as data provider
resource "azurerm_container_group" "provider-connector" {
  name                = "gaiax-provider"
  location            = azurerm_resource_group.core-resourcegroup.location
  resource_group_name = azurerm_resource_group.core-resourcegroup.name
  os_type             = "Linux"
  ip_address_type     = "public"
  dns_name_label      = "${var.environment}-provider"
  image_registry_credential {
    password = var.docker_repo_password
    server   = var.docker_repo_url
    username = var.docker_repo_username
  }
  container {
    cpu   = 2
    image = "${var.docker_repo_url}/beardyinc/dataspaceconnector/gx-provider:latest"
    //    image  = "beardyinc/gx-provider:latest"
    memory = "2"
    name   = "gx-provider"

    ports {
      port     = 8181
      protocol = "TCP"
    }

    environment_variables = {
      CLIENTID       = azuread_application.demo-app-id.application_id,
      TENANTID       = data.azurerm_client_config.current.tenant_id,
      VAULTNAME      = azurerm_key_vault.main-vault.name,
      CONNECTOR_NAME = "gx-provider"
      TOPIC_NAME     = azurerm_eventgrid_topic.control-topic.name
      TOPIC_ENDPOINT = azurerm_eventgrid_topic.control-topic.endpoint
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

# connector that acts as data consumer
resource "azurerm_container_group" "consumer-connector" {
  name                = "gaiax-consumer"
  location            = azurerm_resource_group.core-resourcegroup.location
  resource_group_name = azurerm_resource_group.core-resourcegroup.name
  os_type             = "Linux"
  ip_address_type     = "public"
  dns_name_label      = "${var.environment}-consumer"
  image_registry_credential {
    password = var.docker_repo_password
    server   = var.docker_repo_url
    username = var.docker_repo_username
  }
  container {
    cpu   = 2
    image = "${var.docker_repo_url}/beardyinc/dataspaceconnector/gx-consumer:latest"
    //    image  = "beardyinc/gx-consumer:latest"
    memory = "2"
    name   = "gx-consumer"

    ports {
      port     = 8181
      protocol = "TCP"
    }

    environment_variables = {
      CLIENTID       = azuread_application.demo-app-id.application_id,
      TENANTID       = data.azurerm_client_config.current.tenant_id,
      VAULTNAME      = azurerm_key_vault.main-vault.name,
      CONNECTOR_NAME = "gx-consumer"
      TOPIC_NAME     = azurerm_eventgrid_topic.control-topic.name
      TOPIC_ENDPOINT = azurerm_eventgrid_topic.control-topic.endpoint
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


# vault secrets
resource "azurerm_key_vault_secret" "aws-keyid" {
  name         = "dataspaceconnector-aws-access-key"
  value        = aws_iam_access_key.gx_access_key.id
  key_vault_id = azurerm_key_vault.main-vault.id
  depends_on = [
  azurerm_role_assignment.current-user]
}

resource "azurerm_key_vault_secret" "aws-secret" {
  name         = "dataspaceconnector-aws-secret-access-key"
  value        = aws_iam_access_key.gx_access_key.secret
  key_vault_id = azurerm_key_vault.main-vault.id
  depends_on = [
  azurerm_role_assignment.current-user]
}

resource "azurerm_key_vault_secret" "aws-credentials" {
  key_vault_id = azurerm_key_vault.main-vault.id
  name         = "aws-credentials"
  value = jsonencode({
    "accessKeyId"     = aws_iam_access_key.gx_access_key.id,
    "secretAccessKey" = aws_iam_access_key.gx_access_key.secret
  })
  depends_on = [
  azurerm_role_assignment.current-user]
}

resource "azurerm_key_vault_secret" "blobstorekey" {
  name         = "${azurerm_storage_account.main-blobstore.name}-key1"
  value        = azurerm_storage_account.main-blobstore.primary_access_key
  key_vault_id = azurerm_key_vault.main-vault.id
  depends_on = [
    azurerm_role_assignment.current-user]
}
