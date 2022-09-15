# AWS Test

## Local testing using MinIO

To run AWS integration tests you will need a MinIO instance running:
```
docker run -d -p 9000:9000 -e MINIO_ROOT_USER=root -e MINIO_ROOT_PASSWORD=password bitnami/minio:latest
```

Then set the two environment variables:
```
S3_ACCESS_KEY_ID=root
S3_SECRET_ACCESS_KEY=password
```

## Test using your AWS credential

`IT_AWS_ENDPOINT` can be used to override endpoint URI
for running  integration tests against AWS S3 by environment variable:

```
$ IT_AWS_ENDPOINT=https://us-east-1.amazonaws.com/ \
  IT_AWS_REGION=us-east-1 \
  IT_AWS_PROFILE=myprofie \
  ./gradlew clean test -DincludeTags="AwsS3IntegrationTest" --tests '*S3StatusCheckerIntegrationTest'
```

`IT_AWS_REGION` must be set to your region code in order to avoid
["A conflicting conditional operation is currently in progress against this resource." error](http://stackoverflow.com/questions/13898057/aws-error-message-a-conflicting-conditional-operation-is-currently-in-progress).

`IT_AWS_PROFILE` can be used to specify
[named profile](https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-profiles.html)
referring your own credential.
You can also use access key and secret access key by `S3_ACCESS_KEY_ID` and `S3_SECRET_ACCESS_KEY`.
