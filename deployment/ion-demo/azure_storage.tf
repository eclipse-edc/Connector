#storage account
resource "azurerm_storage_account" "main-blobstore" {
  name                     = "${replace(var.environment, "-", "")}gpstorage"
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