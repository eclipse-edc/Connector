output "client_id" {
  value = azuread_application.demo-app-id.application_id
}

output "tenant-id" {
  value = data.azurerm_client_config.current.tenant_id
}

output "certificate" {
  value     = data.azurerm_key_vault_secret.certificate.value
  sensitive = true
}

output "vault-name" {
  value = azurerm_key_vault.main-vault.name
}

output "storage-account-name" {
  value = azurerm_storage_account.main-blobstore.name
}

output "storage-container-name" {
  value = azurerm_storage_container.main-blob-container.name
}
