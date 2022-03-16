#set the terraform required version
terraform {
  required_version = ">= 1.0"

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      # It is recommended to pin to a given version of the Provider
      version = "=2.98.0"
    }
    azuread = {
      source  = "hashicorp/azuread"
      version = "=2.15.0"
    }
  }
}


# Configure the Microsoft Azure Provider
provider "azurerm" {
  features {
    key_vault {
      # Do not retain Azure Key Vaults after destruction
      purge_soft_delete_on_destroy = true
    }
  }
}
