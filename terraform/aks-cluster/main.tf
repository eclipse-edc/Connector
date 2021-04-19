
terraform {
  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = ">=2.42"
    }
    azuread = {
      source  = "hashicorp/azuread"
      version = ">=1.4.0"
    }
  }
}

resource "azurerm_resource_group" "clusterrg" {
  name     = var.cluster_name
  location = var.location
}

resource "azurerm_kubernetes_cluster" "default" {
  name                = var.cluster_name
  location            = var.location
  resource_group_name = azurerm_resource_group.clusterrg.name
  dns_prefix          = var.cluster_name

  default_node_pool {
    name               = "agentpool"
    node_count         = 3
    vm_size            = "Standard_D2_v2"
    os_disk_size_gb    = 30
    availability_zones = ["1", "2", "3"]
    max_pods           = 110
    type               = "VirtualMachineScaleSets"
    os_disk_type       = "Managed"
  }

  identity {
    type = "SystemAssigned"
  }

  network_profile {
    load_balancer_sku = "standard"
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
    Environment = "Test"
  }
}

