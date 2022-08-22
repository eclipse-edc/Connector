# S3 Core

This extension registers an AWS credentials provider that can be used by all the S3 related extensions.

The credentials lookup works in this order:
- vault (through the keys specified in the [Configuration](#configuration))
- if there are no vault keys, the AWS `DefaultCredentialProvider` will look at:
>  1. Java System Properties - aws.accessKeyId and aws.secretAccessKey
>  2. Environment Variables - AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY
>  3. Credential profiles file at the default location (~/.aws/credentials) shared by all AWS SDKs and the AWS CLI
>  4. Credentials delivered through the Amazon EC2 container service if AWS_CONTAINER_CREDENTIALS_RELATIVE_URI" environment variable is set and security manager has permission to access the variable,
>  5. Instance profile credentials delivered through the Amazon EC2 metadata service 

## Configuration

| Parameter name                          | Description                                                      | Mandatory | Default value |
|-----------------------------------------|------------------------------------------------------------------|-----------|---------------|
| `edc.aws.access.key`                    | The key of the secret where the AWS Access Key Id is stored.     | false     | null          |
| `edc.aws.secret.access.key`             | The key of the secret where the AWS Secret Access Key is stored. | false     | 5             |
| `edc.aws.endpoint.override`             | If valued, the AWS clients will point to the specified endpoint. | false     | null          |
| `edc.aws.client.async.thread-pool-size` | The size of the thread pool used for the async clients.          | false     | 50            |
