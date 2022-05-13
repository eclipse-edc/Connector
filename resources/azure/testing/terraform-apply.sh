#!/bin/bash

set -euo pipefail

ENVIRONMENT=Azure-dev

cd $(dirname "$0")

echo "== Running terraform init =="
. util/terraform-init.sh

echo "== Checking GitHub contributor permissions =="
gh="gh --repo $GITHUB_REPO --env $ENVIRONMENT"
if ! $gh secret list > /dev/null
then
  echo "Cannot access repo $GITHUB_REPO"
  echo "Usage: $0 OWNER/REPO TerraformStateStorageAccountName"
  echo "OWNER/REPO must be a repository on which you have Contributor permissions."
  exit 1
fi

echo "== Running terraform apply =="
terraform apply

echo "== Collecting terraform outputs =="
. ../util/terraform-download-output.sh

terraform output -raw ci_client_id | $gh secret set AZURE_CLIENT_ID
terraform output -raw EDC_AZURE_SUBSCRIPTION_ID | $gh secret set AZURE_SUBSCRIPTION_ID
terraform output -raw EDC_AZURE_TENANT_ID | $gh secret set AZURE_TENANT_ID

cd ..

$gh secret set RUNTIME_SETTINGS < runtime_settings.properties
$gh secret set AZURE_CLIENT_SECRET --body "$CI_CLIENT_SECRET"
