resource "azurerm_cosmosdb_account" "edc-cosmos" {
  location = azurerm_resource_group.core-resourcegroup.location
  name = "edc-cosmos"
  resource_group_name = azurerm_resource_group.core-resourcegroup.name
  offer_type = "Standard"
  consistency_policy {
    consistency_level = "Session"
  }
  geo_location {
    failover_priority = 0
    location = azurerm_resource_group.core-resourcegroup.location
  }

}

resource "azurerm_cosmosdb_sql_database" "edc-database" {
  account_name = azurerm_cosmosdb_account.edc-cosmos.name
  name = "edc-database"
  resource_group_name = azurerm_resource_group.core-resourcegroup.name
  throughput = 400
}

resource "azurerm_cosmosdb_sql_container" "transferprocess" {
  name = "edc-transferprocess"
  resource_group_name = azurerm_cosmosdb_account.edc-cosmos.resource_group_name
  account_name = azurerm_cosmosdb_account.edc-cosmos.name
  database_name = azurerm_cosmosdb_sql_database.edc-database.name
  partition_key_path = "/partitionKey"
}

resource "azurerm_cosmosdb_sql_stored_procedure" "nextForState" {
  name = "nextForState"
  resource_group_name = azurerm_cosmosdb_account.edc-cosmos.resource_group_name
  account_name = azurerm_cosmosdb_account.edc-cosmos.name
  database_name = azurerm_cosmosdb_sql_database.edc-database.name
  container_name = azurerm_cosmosdb_sql_container.transferprocess.name

  body = file("nextForState.js")
}

resource "azurerm_cosmosdb_sql_stored_procedure" "lease" {
  name = "lease"
  resource_group_name = azurerm_cosmosdb_account.edc-cosmos.resource_group_name
  account_name = azurerm_cosmosdb_account.edc-cosmos.name
  database_name = azurerm_cosmosdb_sql_database.edc-database.name
  container_name = azurerm_cosmosdb_sql_container.transferprocess.name

  body = file("lease.js")
}

resource "azurerm_key_vault_secret" "cosmos_db_master_key" {
  key_vault_id = azurerm_key_vault.edc-primary-vault.id
  name = azurerm_cosmosdb_account.edc-cosmos.name
  value = azurerm_cosmosdb_account.edc-cosmos.primary_master_key
}

output "cosmos-config" {
  value = {
    account-name = azurerm_cosmosdb_account.edc-cosmos.name
    db-name = azurerm_cosmosdb_sql_database.edc-database.name
  }
}