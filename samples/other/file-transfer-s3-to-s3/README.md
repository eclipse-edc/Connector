# Transferring files from S3 to S3

This module represents a complete demo that can be launched locally. The configuration is capable of copying files from
one S3 bucket to another. It is possible to use only one instance of the connector, in this case it will send requests
to itself.

Provider configuration is done by providing _provider-artifacts.json_ file where all available artifacts are described.
This file provides information for the artifact metadata storage.

Consumer configuration is done by providing the following Java runtime properties at startup:

* _destinationBucket_ - bucket where to download the requested artifact to (required)
* _destinationRegion_ - AWS region of the destination bucket (required)
* _creds_ - JSON token representing AWS credentials that the provider will use to write to specified bucket (required)

**creds**

```
  {
    "edctype":"dataspaceconnector:secrettoken",
    "sessionToken":"<TOKEN>",
    "accessKeyId":"<ACCESS_KEY>",
    "secretAccessKey":"<SECRET_KEY>"
  }
```

**provider-artifacts.json**

```json
[
  {
    "id": "<objectKey>",
    "policyId": "use-eu",
    "catalogEntry": {
      "edctype": "dataspaceconnector:genericdataentryextensions",
      "keyName": "<objectKey>",
      "type": "dataspaceconnector:s3",
      "bucketName": "<bucket>"
    }
  }
]

```

**NOTE:** For simplicity, the same AWS credentials are used to request all available artifacts. Also, the only policy
available is simple _ids:USE_ policy with id `use-eu`.

**NOTE:** Filename does not change during transfer. When requesting an artifact a consumer provides the identifier of
the requested resource which will look something like the following:

`data/folder/test.txt`

The last part `test.txt` will be used as a target filename.

## Prerequisites

* Cross account access is configured on AWS side
* Files are uploaded to one of the buckets
* File _provider-artifacts.json_ is available in the current directory

## Building and Running

* Build as follows:

  `gradlew :samples:other:file-transfer-s3-to-s3:shadowJar`

* Run as follows:

  `java -jar -Dcreds=<SERIALIZED_JSON_TOKEN> -DdestinationBucket=<DESTINATION_BUCKET> -DdestinationRegion=<DESTINATION_REGION> launchers/basic/build/libs/dataspaceconnector-transfer-demo.jar`

* Use postman collection in _/postman_ folder


