# [HashiCorp Vault](https://www.vaultproject.io/) Extension

## Configuration

| Key | Description | Mandatory | 
|:---|:---|---|
| edc.vault.hashicorp.url | URL to connect to the HashiCorp Vault | X |
| edc.vault.hashicorp.token | Value for [Token Authentication](https://www.vaultproject.io/docs/auth/token) with the vault | X |
| edc.vault.hashicorp.timeout.seconds | Request timeout in seconds when contacting the vault (default: 30) | |
