#!/bin/bash

set -euxo pipefail

cd $(dirname "$0")

. .env

# Create resource group
az group create --name "$TERRAFORM_STATE_STORAGE_RESOURCE_GROUP" --location "$TERRAFORM_STATE_STORAGE_LOCATION" -o none

# Create storage account
az storage account create --resource-group "$TERRAFORM_STATE_STORAGE_RESOURCE_GROUP" --name "$TERRAFORM_STATE_STORAGE_ACCOUNT" -o none

# Create blob container
az storage container create --name "$TERRAFORM_STATE_STORAGE_CONTAINER" --account-name "$TERRAFORM_STATE_STORAGE_ACCOUNT" --auth-mode login -o none

echo "Script completed successfully."
