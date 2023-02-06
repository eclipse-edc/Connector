# AWS Secrets Manager Vault

The vault-aws extension is an implementation of the Vault interface, which stores secrets in AWS Secrets Manager.
Arbitrary key names are possible through the key sanitation feature. 

## Limitations
- 50 TpS (Transactions per Second) for storing secrets and deleting secrets.
- 10,000 TpS (Transactions per Second) for retrieving secrets.

## Configuration

### Credentials resolution
The vault-aws extension uses the AWS SDK Secrets Manager client. Credentials for accessing Secrets Manager are resolved using the default credential provider chain.
The default AWS credentials provider chain that looks for credentials in this order:

1. Environment Variables - AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY 
2. Java System Properties - aws.accessKeyId and aws.secretKey
3. Web Identity Token credentials from the environment or container
4. Credential profiles file at the default location (~/.aws/credentials) shared by all AWS SDKs and the AWS CLI
5. Credentials delivered through the Amazon EC2 container service if AWS_CONTAINER_CREDENTIALS_RELATIVE_URI environment variable is set and security manager has permission to access the variable,
6. Instance profile credentials delivered through the Amazon EC2 metadata service

### Client retry behaviour
The AWS SDK has retry behaviour built in. It can be controlled globally through the environment variables AWS_MAX_ATTEMPTS, AWS_RETRY_MODE.
Please see [the SDK documentation](https://docs.aws.amazon.com/sdkref/latest/guide/feature-retry-behavior.html) for details.

### Other configuration options

| Parameter name                                      | Description                        | Mandatory | Default value                          |
|:----------------------------------------------------|:-----------------------------------|:----------|:---------------------------------------|
| `edc.vault.aws.region`  | AWS region for AWS Secrets Manager | true      |                                        |

## Decisions
- Use default credentials provider to be as flexible as possible in credentials resolution. 
- Secrets will not be overwritten if they exist to prevent potential leakage of credentials to third parties.
- Keys strings are sanitized to comply with key requirements of AWS Secrets Manager. Sanitizing replaces all illegal characters with '-' and appends the hash code of the original key to minimize the risk of key collision after the transformation, because the replacement operation is a many-to-one function. A warning will be logged if the key contains illegal characters.

## Change log
