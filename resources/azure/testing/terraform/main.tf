## This configuration creates and configures the following resources:
## - Azure Key Vault instance for managing data factory secrets
## - Azure Data Factory instance connected to Key Vault
## - Storage accounts for integration tests, representing provider and consumer side

# Get information about the identity Terraform runs under
data "azurerm_client_config" "current" {}

# Get information about the GitHub workflow identity
data "azuread_service_principal" "ci_client" {
  application_id = var.ci_client_id
}

# Create a resource group to store resources
resource "azurerm_resource_group" "main" {
  name     = "rg-${var.prefix}-main"
  location = var.location
  tags     = var.resource_tags
}

## Create storage accounts for provider-side data (source) and consumer-side data (sink).
## Grant test identity access to storage keys.
resource "azurerm_storage_account" "provider" {
  name                     = "sa${var.prefix}prov"
  resource_group_name      = azurerm_resource_group.main.name
  location                 = azurerm_resource_group.main.location
  account_tier             = "Standard"
  account_replication_type = "LRS"
  tags                     = var.resource_tags
}

resource "azurerm_role_assignment" "provider_ci_client" {
  scope                = azurerm_storage_account.provider.id
  role_definition_name = "Storage Account Contributor"
  principal_id         = data.azuread_service_principal.ci_client.object_id
}

resource "azurerm_storage_account" "consumer" {
  name                     = "sa${var.prefix}cons"
  resource_group_name      = azurerm_resource_group.main.name
  location                 = azurerm_resource_group.main.location
  account_tier             = "Standard"
  account_replication_type = "LRS"
  tags                     = var.resource_tags
}

resource "azurerm_role_assignment" "consumer_ci_client" {
  scope                = azurerm_storage_account.consumer.id
  role_definition_name = "Storage Account Contributor"
  principal_id         = data.azuread_service_principal.ci_client.object_id
}

## Create Key Vault
resource "azurerm_key_vault" "main" {
  name                      = "kv${var.prefix}adf"
  location                  = azurerm_resource_group.main.location
  resource_group_name       = azurerm_resource_group.main.name
  tenant_id                 = data.azurerm_client_config.current.tenant_id
  sku_name                  = "standard"
  enable_rbac_authorization = true
  tags                      = var.resource_tags
}

## Create Data Factory
resource "azurerm_data_factory" "main" {
  name                = "adf-${var.prefix}-main"
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name
  identity {
    type = "SystemAssigned"
  }
  tags = var.resource_tags
}

## Create Data Factory linked service to retrieve keys from Key Vault
resource "azurerm_data_factory_linked_service_key_vault" "main" {
  name                = "AzureKeyVault"
  resource_group_name = azurerm_resource_group.main.name
  data_factory_id     = azurerm_data_factory.main.id
  key_vault_id        = azurerm_key_vault.main.id
}

## Grant Data Factory read access to Key Vault secrets
resource "azurerm_role_assignment" "data_factory_key_vault" {
  scope                = azurerm_key_vault.main.id
  role_definition_name = "Key Vault Secrets User"
  principal_id         = azurerm_data_factory.main.identity[0].principal_id
}

## Grant DPF write access to Key Vault secrets
resource "azurerm_role_assignment" "ci_key_vault" {
  scope                = azurerm_key_vault.main.id
  role_definition_name = "Key Vault Secrets Officer"
  principal_id         = data.azuread_service_principal.ci_client.object_id
}

## Grant DPF access to Data Factory
resource "azurerm_role_assignment" "data_factory" {
  scope                = azurerm_data_factory.main.id
  role_definition_name = "Data Factory Contributor"
  principal_id         = data.azuread_service_principal.ci_client.object_id
}

## Store provider storage account accesss key seceret
resource "azurerm_key_vault_secret" "provider_storage_key" {
  name         = "${azurerm_storage_account.provider.name}-key1"
  value        = azurerm_storage_account.provider.primary_access_key
  key_vault_id = azurerm_key_vault.main.id
  depends_on   = [
    azurerm_role_assignment.ci_key_vault
  ]
}

## Store consumer storage account accesss key seceret
resource "azurerm_key_vault_secret" "consumer_storage_key" {
  name         = "${azurerm_storage_account.consumer.name}-key1"
  value        = azurerm_storage_account.consumer.primary_access_key
  key_vault_id = azurerm_key_vault.main.id
  depends_on   = [
    azurerm_role_assignment.ci_key_vault
  ]
}

## CosmosDB account for integration testing. No need to create databases or containers,
## they are created by the tests
resource "azurerm_cosmosdb_account" "cosmosdb_integrationtest" {
  name                = "${var.prefix}-cosmosdb"
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name
  offer_type          = "Standard"

  enable_automatic_failover = false
  enable_free_tier          = true

  capabilities {
    name = "EnableAggregationPipeline"
  }

  consistency_policy {
    consistency_level = "Strong"
  }

  geo_location {
    location          = azurerm_resource_group.main.location
    failover_priority = 0
  }
}