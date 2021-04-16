#!/bin/bash

function createAppReg() {
  displayName=$1
  certFile=$2
  description=$3
  numArgs=$#

  appRegistrationName=$displayName
  echo "provision app registration"
  az ad app create --display-name "$appRegistrationName" --key-type AsymmetricX509Cert --available-to-other-tenants false >/dev/null
  sleep 10
  appJson=$(az ad app list --display-name $appRegistrationName)
  objectId=$(echo $appJson | jq -r '.[0].objectId')
  appId=$(echo $appJson | jq -r '.[0].appId')

  if [ $numArgs -eq 2 ]; then
    # interpret certFile as certificate path
    # upload the public key to the app reg
    echo "upload certificate of $objectId"
    credJson=$(az ad app credential reset --id $objectId --cert @$certFile)
  else
    # interpret certfile as secret
    clientSecret=$certFile
    echo "upload client secret to $objectId"
    credJson=$(az ad app credential reset --id $objectId --password $clientSecret --credential-description "$description")
  fi

  clientId=$(echo $credJson | jq -r '.appId')
  tenantId=$(echo $credJson | jq -r '.tenant')

  # create a service principial for the App Reg - THIS IS THE IMPORTANT PART!
  echo "create service principal"
  az ad sp create --id $objectId
}

function createStorageAccount() {
  accountName=$1
  rgName=$2

  saJson=$(az storage account create --name $accountName -g $rgName --https-only true --kind BlobStorage -l westeurope --min-tls-version TLS1_2 --sku Standard_LRS --access-tier hot)
}

function storeKeysInVault() {
  storageAccountName=$1
  keyVaultName=$2

  keyJson=$(az storage account keys list -n $storageAccountName)
  key1=$(echo $keyJson | jq -r '.[0].value')
  key2=$(echo $keyJson | jq -r '.[1].value')
  az keyvault secret set --name "$storageAccountName-key1" --vault-name $keyvaultName --value $key1
  az keyvault secret set --name "$storageAccountName-key2" --vault-name $keyvaultName --value $key2
}

function createAks() {

  suffix=$1
  rgName=$2
  templateDir=$3

  region=$(cat aks/parameters.json | jq -r '.parameters.location.value')
  aksName=$(cat aks/parameters.json | jq -r '.parameters.resourceName.value')
  echo "deploy AKS named $aksName in $rgName"
  mcgName="MC_${rgName}_${aksName}_${region}"
  echo "note that there will at least be one additional resource group named $mcgName"

  az deployment group create -g $rgName -n DeployAks-$suffix --template-file aks/template.json --parameters @aks/parameters.json

  # create a dns name label in the public IP address resource
  # 1. get load balancer's public IP addresses:
  publicIpIds=$(az network lb show -g $mcgName -n kubernetes --query "frontendIpConfigurations[].publicIpAddress.id" --out tsv)
  # 1.1 iterate over all public IP addresses,  read their IP/FQDN pairs
  # 2. filter out the one starting with "kubernetes-"
  publicIpName=""
  while read publicIpId; do
    if [[ $publicIpId == *"/kubernetes-"* ]]; then
      publicIpName=$(echo $publicIpIds | rev | cut -d'/' -f 1 | rev)
      az network public-ip show --ids "${publicIpId}" --query "{ ipAddress: ipAddress, fqdn: dnsSettings.fqdn }" --out tsv
    fi
  done <<<"${publicIpIds}"

  # set the public IP's dns name
  dnsName=$rgName
  echo "setting DNS label of IP $publicIpName -> $dnsName"
  az network public-ip update --dns-name $dnsName --allocation-method Static -n $publicIpName -g $mcgName
}

suffix=''
if [ -z "$1" ]; then
  suffix=$(date +%s)
else
  suffix=$1
fi

echo "suffix is $suffix"

if ! command -v openssl &>/dev/null; then
  echo "openssl is not installed - aborting!"
fi

if ! command -v jq &>/dev/null; then
  echo "jq (a Json Processor) is not installed - aborting!"
  exit 1
fi

if ! command -v az &>/dev/null; then
  echo "Azure CLI not installed, please install and login with your AD credentials!"
  exit 2
fi

if test -f "cert.pem"; then
  echo "certificate exists - reusing"
else
  echo "no certificate found - creating a new one"
  # create a certificate - will ask for some information!
  openssl req -newkey rsa:4096 -new -nodes -x509 -days 3650 -keyout key.pem -out cert.pem

  # convert cert.pem to cert.pfx
  openssl pkcs12 -inkey key.pem -in cert.pem -export -out cert.pfx
fi

# provision an app registration for the primary identity
createAppReg PrimaryIdentity-$suffix cert.pem
primaryAppObjectId=$objectId
prim_clientId=$clientId
prim_tenantId=$tenantId
echo "primary app id is : $primaryAppObjectId"

# create a resource group
rgName=dagx-$suffix
echo "create resource group $rgName"
az group create --name $rgName --location westeurope

# provision keyvault
keyvaultName=DagxKeyVault-$suffix
echo "provision key vault $keyvaultName and assign roles"
az keyvault create --enable-rbac-authorization -g $rgName -n $keyvaultName -l westeurope
keyVaultId=$(az keyvault list -g $rgName | jq -r '.[0].id')
az role assignment create --role "Key Vault Secrets Officer" --scope $keyVaultId --assignee $appId

# assign current user as admin to vault
currentUserOid=$(az ad signed-in-user show | jq -r '.objectId')
az role assignment create --role "Key Vault Administrator" --scope $keyVaultId --assignee $currentUserOid

# provision storage account
saName=dagxblobstore$suffix
createStorageAccount $saName $rgName

## let keyvault handle the storage account's key regeneration and store an SAS token definition
## if we wanna use that feature, we can uncomment the next 5 lines

# saId=$(echo $saJson | jq -r '.id')
# az role assignment create --role "Storage Account Key Operator Service Role" --scope $saId --assignee cfa8b339-82a2-471a-a3c9-0fc0be7a4093
# az keyvault storage add --vault-name $keyvaultName -n $saName --active-key-name key1 --auto-regenerate-key --regeneration-period P90D --resource-id $saId
# sas=$(az storage account generate-sas --expiry 2021-01-05 --permission rwdl --resource-type co --services b --https-only --account-name $saName --account-key 00000000)
# az keyvault storage sas-definition create --vault-name $keyVaultName --account-name $saName -n DataTransferDefinition --validity-period P2D --sas-type service --templateUri $sas

# Store storage account key1 and key2 in vault
storeKeysInVault $saName $keyVaultName

echo "storage account keys successfully stored in vault"

echo "remove role assignment for current user"
az role assignment delete --role "Key Vault Administrator" --scope $keyVaultId --assignee $currentUserOid

# deploying AKS
createAks $suffix $rgName aks

# provision another service principal that will later be used for the Apache Nifi cluster
clientSecret=$(openssl rand -base64 32)
createAppReg NifiIdentity-$suffix $clientSecret "Nifi LB secret"
nifiAppObjectId=$objectId

# some output:
echo "certificate: ./cert.pfx and ./cert.pem"
echo "private key: ./key.pem"
echo "primary client-id: $prim_clientId"
echo "primary tenant-id: $prim_tenantId"
echo "resource-group: $rgName"
echo "nifi app client secret: $clientSecret"

# cleanup
echo 'Press q to cleanup resources, any other key to quit...'
read -n 1 k <&1

if [[ $k == q ]]; then

  echo "cleaning up..."
  echo "deleting primary app registration"
  az ad app delete --id $primaryAppObjectId
  echo "deleting nifi app registration"
  az ad app delete --id $nifiAppObjectId
  echo "deleting resource groups"
  az group delete -n $rgName -y
  az group delete -n NetworkWatcher_$region -y
  echo "purging vault $keyvaultName"
  az keyvault purge --name $keyvaultName
  echo "cleanup done"
fi
