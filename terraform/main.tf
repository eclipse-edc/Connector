# Configure the Azure provider
terraform {
  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = ">= 2.26"
    }
    azuread = {
      source  = "hashicorp/azuread"
      version = "1.4.0"
    }
  }

  required_version = ">= 0.14.9"
}

provider "azurerm" {
  features {
    key_vault {
      purge_soft_delete_on_destroy = true
    }
  }
}

provider "azuread" {
  # Configuration options
}

resource "azurerm_resource_group" "rg" {
  name     = "dagx-terraform-test"
  location = "westeurope"
}

resource "azuread_application" "dagx-terraform-app" {
  display_name               = "dagx-terraform-app"
  available_to_other_tenants = false
}

resource "azuread_application_certificate" "dagx-terraform-app-cert" {
  type                  = "AsymmetricX509Cert"
  application_object_id = azuread_application.dagx-terraform-app.id
  value                 = file("cert.pem")
  end_date_relative     = "2400h"
}

resource "azuread_service_principal" "dagx-terraform-app-sp" {
  application_id               = azuread_application.dagx-terraform-app.application_id
  app_role_assignment_required = false
  tags                         = ["terraform"]
}

data "azurerm_client_config" "current" {}

resource "azurerm_key_vault" "dagx-terraform-vault" {
  name                        = "dagx-terraform-vault"
  location                    = azurerm_resource_group.rg.location
  resource_group_name         = azurerm_resource_group.rg.name
  enabled_for_disk_encryption = false
  tenant_id                   = data.azurerm_client_config.current.tenant_id
  soft_delete_retention_days  = 7
  purge_protection_enabled    = false

  sku_name                  = "standard"
  enable_rbac_authorization = true
}

resource "azurerm_role_assignment" "assign_vault" {
  scope                = azurerm_key_vault.dagx-terraform-vault.id
  role_definition_name = "Key Vault Secrets Officer"
  principal_id         = azuread_service_principal.dagx-terraform-app-sp.object_id
}

resource "azurerm_storage_account" "dagxblobstore" {
  name                     = "dagxtfblob"
  resource_group_name      = azurerm_resource_group.rg.name
  location                 = azurerm_resource_group.rg.location
  account_tier             = "Standard"
  account_replication_type = "GRS"
  account_kind             = "BlobStorage"
}

data "azurerm_storage_account" "dagxblobstore" {
  name                = azurerm_storage_account.dagxblobstore.name
  resource_group_name = azurerm_storage_account.dagxblobstore.resource_group_name
}

# resource "azurerm_key_vault_secret" "key1" {
#     name = "blobstore-key1"  
#     value= data.azurerm_storage_account.dagxblobstore.primary_access_key
#     key_vault_id = azurerm_key_vault.dagx-terraform-vault.id
# }

resource "azurerm_kubernetes_cluster" "dagx" {
  name                = "dagx-tf-aks1"
  location            = azurerm_resource_group.rg.location
  resource_group_name = azurerm_resource_group.rg.name
  dns_prefix          = "dagx-tf-aks"

  default_node_pool {
    name       = "agentpool"
    node_count = 3
    vm_size    = "Standard_D2_v2"
    os_disk_size_gb = 30
    availability_zones = [ "1","2","3" ]
    max_pods = 110
    type = "VirtualMachineScaleSets"
    os_disk_type = "Managed"
  }



  identity {
    type = "SystemAssigned"
  }

  network_profile {
    load_balancer_sku = "standard"
    load_balancer_profile {
      
    }
    network_plugin    = "kubenet"
  }

  addon_profile {
    http_application_routing {
      enabled = true
    }
    azure_policy {
      enabled = false
    }
  }

  tags = {
    Environment = "Production"
  }
}

output "client_certificate" {
  value = azurerm_kubernetes_cluster.dagx.kube_config.0.client_certificate
}

output "kube_config" {
  value     = azurerm_kubernetes_cluster.dagx.kube_config_raw
  sensitive = true
}
