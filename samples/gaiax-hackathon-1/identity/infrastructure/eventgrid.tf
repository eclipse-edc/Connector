// topic for connector application events
resource "azurerm_eventgrid_topic" "control-topic" {
  location            = azurerm_resource_group.core-resourcegroup.location
  name                = "${var.environment}-control-events"
  resource_group_name = azurerm_resource_group.core-resourcegroup.name
}

// keyvault secret for the access key
resource "azurerm_key_vault_secret" "event-grid-key" {
  key_vault_id = azurerm_key_vault.main-vault.id
  name         = azurerm_eventgrid_topic.control-topic.name
  value        = azurerm_eventgrid_topic.control-topic.primary_access_key
  depends_on = [
  azurerm_role_assignment.current-user]
}

// storage queue that will be used for control events
resource "azurerm_storage_queue" "system-event-queue" {
  name                 = "${var.environment}-control"
  storage_account_name = azurerm_storage_account.main-blobstore.name
}

// subscription for the control events
resource "azurerm_eventgrid_event_subscription" "control-events" {
  name  = "${azurerm_eventgrid_topic.control-topic.name}-sub"
  scope = azurerm_eventgrid_topic.control-topic.id
  storage_queue_endpoint {
    queue_name         = azurerm_storage_queue.system-event-queue.name
    storage_account_id = azurerm_storage_account.main-blobstore.id
  }
}

output "topic-endpoint" {
  value = azurerm_eventgrid_topic.control-topic.endpoint
}
