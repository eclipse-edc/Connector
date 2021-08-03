resource "azurerm_eventgrid_topic" "edc-topic" {
  location = azurerm_resource_group.core-resourcegroup.location
  name = "connector-events"
  resource_group_name = azurerm_resource_group.core-resourcegroup.name
}

resource "azurerm_key_vault_secret" "event-grid-key" {
  key_vault_id = azurerm_key_vault.edc-primary-vault.id
  name = azurerm_eventgrid_topic.edc-topic.name
  value = azurerm_eventgrid_topic.edc-topic.primary_access_key
}

data "http" "template" {
  url = "https://raw.githubusercontent.com/Azure-Samples/azure-event-grid-viewer/master/azuredeploy.json"
}

resource "azurerm_template_deployment" "eventviewer" {
  deployment_mode = "Incremental"
  name = "event-viewer-app"
  resource_group_name = azurerm_resource_group.core-resourcegroup.name
  template_body = data.http.template.body
  parameters = {
    siteName = var.eventViewerUrl
    hostingPlanName = "viewerhost"
  }
}
variable "eventViewerUrl" {
  default = "edc-eventviewer"
}

resource "azurerm_eventgrid_event_subscription" "eventviewer" {
  name = "event-viewer-subscription"
  scope = azurerm_eventgrid_topic.edc-topic.id
  webhook_endpoint {
    url = "https://${var.eventViewerUrl}.azurewebsites.net/api/updates"
  }
}

output "topic-endpoint" {
  value = azurerm_eventgrid_topic.edc-topic.endpoint
}

output "viewer-url" {
  value = "https://${var.eventViewerUrl}.azurewebsites.net/"
}