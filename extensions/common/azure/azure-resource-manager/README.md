# Azure Resource Manager module

This module provides Azure Identity SDK and Azure Resource Manager SDK objects configured based on runtime settings:

Required runtime settings:

- `edc.azure.tenant.id`: the Azure Active Directory tenant identifier to connect to.
- `edc.azure.subscription.id`: the identifier of the Azure subscription containing the resources to access.

In addition, the module requires a credential to be provided using one of the methods in the [DefaultAzureCredential class](https://docs.microsoft.com/java/api/com.azure.identity.defaultazurecredential), for example:

- The `AZURE_CLIENT_ID`, `AZURE_CLIENT_SECRET` and `AZURE_TENANT_ID` environment variables to be set for service principal authentication.
- In a development environment, the Azure CLI to be [logged in](https://docs.microsoft.com/cli/azure/authenticate-azure-cli).

See the [DefaultAzureCredential class](https://docs.microsoft.com/java/api/com.azure.identity.defaultazurecredential) documentation for additional options.
