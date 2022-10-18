# [HashiCorp Vault](https://www.vaultproject.io/) Extension

## Configuration

| Key | Description | Mandatory | 
|:---|:---|---|
| edc.vault.hashicorp.url | URL to connect to the HashiCorp Vault | X |
| edc.vault.hashicorp.token | Value for [Token Authentication](https://www.vaultproject.io/docs/auth/token) with the vault | X |
| edc.vault.hashicorp.timeout.seconds | Request timeout in seconds when contacting the vault (default: 30) | |

## Setup vault for integration tests

The integration tests rely on a vault running locally.
This can be achieved by starting a docker container with the following configuration.

`docker run  -e 'VAULT_DEV_ROOT_TOKEN_ID=test-token' -p "8200:8200" vault:1.9.7`
