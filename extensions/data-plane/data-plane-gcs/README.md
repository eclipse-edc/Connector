## Google cloud storage data plane module

### About this module

This module contains a Data Plane extension to copy data to and from Google cloud storage.

### Authentication

Currently, Google storage data plane only authenticates against the Google Cloud API using
the [Application Default Credentials](https://cloud.google.com/docs/authentication#adc).

These will automatically be provided if the connector is deployed with the correct service account attached.

### Configuration


### Data source properties

| Key               | Description                                                               | Mandatory |
|:------------------|:--------------------------------------------------------------------------|---|
| type | GoogleCloudStorage                                                        | X |
| bucket_name | A valid name of your bucket                                               | X |
| blob_name | Name of your blob/object in the bucket. Currently only a single blob name | X |

### Data destination properties

| Key               | Description                                                                                                | Mandatory with provisioner | Mandatory without provisioner |
|:------------------|:-----------------------------------------------------------------------------------------------------------|----------------------------|-------------------------------|
| type | "GoogleCloudStorage"                                                                                       | X                          | X                             |
| bucket_name | A valid name of your bucket                                                                                |                           | X                             |
| blob_name | Name of your blob/object in the bucket. Currently only a single blob name                                  |                           | X                             |
| storage_class | STANDARD/ NEARLINE/ COLDLINE/ ARCHIVE / [More info](https://cloud.google.com/storage/docs/storage-classes) | X                          |                              |
| location | [Available regions](https://cloud.google.com/storage/docs/locations#location-r)                            | X                          |                              |
