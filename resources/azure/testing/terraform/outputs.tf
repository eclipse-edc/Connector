### Application runtime settings ###

output "EDC_AZURE_TENANT_ID" {
  value       = data.azurerm_client_config.current.tenant_id
  description = "Azure Active Directory Tenant ID for the GitHub workflow identity."
}

output "EDC_AZURE_SUBSCRIPTION_ID" {
  value       = data.azurerm_client_config.current.subscription_id
  description = "Azure Subscription ID in which cloud resources are deployed."
}

output "EDC_DATA_FACTORY_RESOURCE_ID" {
  value       = azurerm_data_factory.main.id
  description = "Resource ID of the Azure Data Factory deployed for tests."
}

output "EDC_DATA_FACTORY_KEY_VAULT_RESOURCE_ID" {
  value       = azurerm_key_vault.main.id
  description = "Resource ID of the Azure Key Vault connected to the Data Factory."
}

output "EDC_DATA_FACTORY_KEY_VAULT_LINKEDSERVICENAME" {
  value       = azurerm_data_factory_linked_service_key_vault.main.name
  description = "Name of the Data Factory linked service representing the connected Key Vault."
}

### Values for CI scripts ###

output "ci_client_id" {
  value       = var.ci_client_id
  description = "Application ID (Client ID) of the GitHub workflow that runs the CI job and needs access to cloud resources."
}

### Integration test configuration values ###

output "test_provider_storage_resourceid" {
  value       = azurerm_storage_account.provider.id
  description = "Resource ID of the Azure Storage account deployed for holding provider data in tests."
}

output "test_provider_storage_name" {
  value       = azurerm_storage_account.provider.name
  description = "Name of the Azure Storage account deployed for holding provider data in tests."
}

output "test_consumer_storage_resourceid" {
  value       = azurerm_storage_account.consumer.id
  description = "Resource ID of the Azure Storage account deployed for holding consumer data in tests."
}

output "test_consumer_storage_name" {
  value       = azurerm_storage_account.consumer.name
  description = "Name of the Azure Storage account deployed for holding consumer data in tests."
}

output "test_key_vault_name" {
  value       = azurerm_key_vault.main.name
  description = "Name of the Azure Key Vault connected to the Data Factory."
}

output "EDC_COSMOS_ITEST_KEY" {
  value       = azurerm_cosmosdb_account.cosmosdb_integrationtest.primary_key
  description = "Primary access key for the CosmosDB Account used in testing"
  sensitive   = true
}

output "EDC_COSMOS_ITEST_URL" {
  value       = azurerm_cosmosdb_account.cosmosdb_integrationtest.endpoint
  description = "Public endpoint for the CosmosDB Account used in testing"
}