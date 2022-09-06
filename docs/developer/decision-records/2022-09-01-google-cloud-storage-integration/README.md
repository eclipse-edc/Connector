# Integration of Google Cloud Storage

## Decision

Google Cloud Storage (GCS) is the object storage on Google Cloud Platform. Data transfer should be enabled from and to
GCS using the Data Plane Framework.

## Rationale

Integrating Google Cloud Platform will enable many users to join data spaces. By using the Data Plane Framework we
enable users to transfer files or blobs across cloud providers.

## Approach

Implementation should mostly follow [dpf-blob-data-transfer](..//2022-04-21-dpf-blob-data-transfer/README.md)

### GCS Implementations for the Data Plane Framework:

1. A GcsProvisioner that creates the bucket and provides write access to provider via service account
   and [OAuth 2.0 token](https://cloud.google.com/storage/docs/authentication#oauth):
    1. Create Bucket
    2. Create service account
    3. Grant service account write permission on the created bucket
    4. Create OAuth Token
    5. Create and return response object that contains all required information to authenticate as the service account
    6. During deprovisioning delete/deactivate service account to remove access rights
2. GcsSink, GcsSource and the correlating Factories. Sink uses the provided token to authenticate

### Future improvements

* GCP Vault implementation using
  Google [Secret Manager](https://cloud.google.com/secret-manager/docs/creating-and-accessing-secrets)
