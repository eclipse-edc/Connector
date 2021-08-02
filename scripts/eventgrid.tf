resource "azurerm_eventgrid_topic" "dagx-topic" {
  location            = azurerm_resource_group.core-resourcegroup.location
  name                = "connector-events"
  resource_group_name = azurerm_resource_group.core-resourcegroup.name
}

resource "azurerm_key_vault_secret" "event-grid-key" {
  key_vault_id = azurerm_key_vault.dagx-terraform-vault.id
  name         = azurerm_eventgrid_topic.dagx-topic.name
  value        = azurerm_eventgrid_topic.dagx-topic.primary_access_key
}

data "http" "template" {
  url = "https://raw.githubusercontent.com/Azure-Samples/azure-event-grid-viewer/master/azuredeploy.json"
}

resource "azurerm_template_deployment" "eventviewer" {
  deployment_mode     = "Incremental"
  name                = "event-viewer-app"
  resource_group_name = azurerm_resource_group.core-resourcegroup.name
  template_body       = data.http.template.body
  parameters = {
    siteName        = var.eventViewerUrl
    hostingPlanName = "viewerhost"
  }
}
variable "eventViewerUrl" {
  default = "dagx-eventviewer"
}

resource "azurerm_eventgrid_event_subscription" "eventviewer" {
  name  = "event-viewer-subscription"
  scope = azurerm_eventgrid_topic.dagx-topic.id
  webhook_endpoint {
    url = "https://${var.eventViewerUrl}.azurewebsites.net/api/updates"
  }
}

output "topic-endpoint" {
  value = azurerm_eventgrid_topic.dagx-topic.endpoint
}

output "viewer-url" {
  value = "https://${var.eventViewerUrl}.azurewebsites.net/"
}