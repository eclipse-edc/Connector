# Azure Data Factory module

This module for the Data Plane Framework provides a transfer service for
performing serverless data movement using Azure Data Factory.

The Azure Resource Manager module should be loaded as well. See its documentation for runtime settings required for authentication.

At this time, this module has the following limitations:

- A runtime thread is held on to for polling for transfer completion.
- Only Azure blob is supported as both storage and destination.
- Both Azure storage accounts must be accessible from the Internet.
- Only a single named blob can be copied in a data transfer request.
- The transfer must complete in at most 1 hour, or will time out.
- Resources created in Data Factory and Key Vault secrets are not deleted after the run.

Required runtime settings:

- `edc.data.factory.resource.id` The Resource ID of the Azure Data Factory instance to be used for data transfers.
- `edc.data.factory.key.vault.resource.id` The Resource ID of the Azure Key Vault to be used for storing secrets.

Optional runtime settings:

- `edc.data.factory.key.vault.linkedservicename` name of the Data Factory linked service for the Azure Key Vault in the Data Factory instance. Defaults to `AzureKeyVault`. The Linked Service must already exist.
