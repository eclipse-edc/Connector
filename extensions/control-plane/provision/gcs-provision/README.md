# Configure and run the EDC

### Authentication

GcsProvisionerExtension authenticates against the Google Cloud API using
the [Application Default Credentials](https://cloud.google.com/docs/authentication#adc).

These will automatically be provided if the connector is deployed with the correct service account attached (see below
in for the project setup and service account creation).

### Configuration

| Key                 | Description                      | Mandatory |
|:--------------------|:---------------------------------|---|
| edc.gcp.project.id | ID of the GCP projcet to be used | X |

# GCP Project Setup

For the provisioning to work we will need to run it with a service account with the correct permissions. There
permissions are:

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

