## Google cloud storage provisioner module

### About this module
This module contains a Data Plane extension to copy data to and from Google cloud storage.

# Configure and run the EDC

### Authentication
Google storage data plane supports three different approaches for authentication:
* Default authentication:
    * Authenticates against the Google Cloud API using the [Application Default Credentials](https://cloud.google.com/docs/authentication#adc).
    * These will automatically be provided if the connector is deployed with the correct service account attached.
* Service Account key file 
    * Authentication using a Service Account key file exported from Google Cloud Platform
    * Service Account key file can be stored in a vault or encoded as base64 and provided in the dataAddress.


### Configuration

| Key                      | Description                                                                                                       | Mandatory |
|:-------------------------|:------------------------------------------------------------------------------------------------------------------|-----------|
| edc.gcp.project.id       | ID of the GCP projcet to be used for bucket and token creation. This can be difined in the DataAddress properties |  |

#### DataAddress properties
| Key                      | Description                                                                                                                                                                      | Mandatory |
|:-------------------------|:---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------|
| type                     | GoogleCloudStorage                                                                                                                                                               | X    | 
| project_id               | ID of the GCP projcet to be used for bucket and toekn creation. If the project_id has not been defined here, the projectId in the connector configurations will be used instead! |   | 
| storage_class            | STANDARD/ NEARLINE/ COLDLINE/ ARCHIVE / [More info](https://cloud.google.com/storage/docs/storage-classes)                                                                       | X |
| location                 | [Available regions](https://cloud.google.com/storage/docs/locations#location-r)                                                                                                  | X |
| service_account_key_name | It should reference a vault entry containing a [Service Account Key File](https://cloud.google.com/iam/docs/creating-managing-service-account-keys#creating) in json.            |  |
| service_account_value    | It should contain a [Service Account Key File](https://cloud.google.com/iam/docs/creating-managing-service-account-keys#creating) in json encoded with base64                    |  |

# GCP Default authentication Setup

For the provisioning to work we will need to run it with a service account with the correct permissions. The permissions are:

- creating buckets
- creating service accounts
- setting permissions
- creating access tokens

### Set project variable

```
PROJECT_ID={PROJECT_ID}
```

### Service account setup

Create service account that will be used when interacting with the Google Cloud API.

```
gcloud iam service-accounts create dataspace-connector \
    --description="Service account used for running eclipse dataspace connector" \
    --display-name="EDC service account"
```

Assign required roles to service account

```
gcloud projects add-iam-policy-binding $PROJECT_ID \
--member="serviceAccount:dataspace-connector@$PROJECT_ID.iam.gserviceaccount.com" \
--role="roles/iam.serviceAccountAdmin"

gcloud projects add-iam-policy-binding $PROJECT_ID \
--member="serviceAccount:dataspace-connector@$PROJECT_ID.iam.gserviceaccount.com" \
--role="roles/storage.admin"

gcloud projects add-iam-policy-binding $PROJECT_ID \
--member="serviceAccount:dataspace-connector@$PROJECT_ID.iam.gserviceaccount.com" \
--role="roles/iam.serviceAccountTokenCreator"
```

