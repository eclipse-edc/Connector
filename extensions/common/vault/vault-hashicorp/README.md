# [HashiCorp Vault](https://www.vaultproject.io/) Extension

---

**Please note:**
Using the HashiCorp vault it is possible to define multiple data entries per secret. Other vaults might allow only one
entry per secret (e.g. Azure Key Vault).

Therefore, the HashiCorp vault extension **only** checks the '**content**' data entry! Please use this knowledge when
creating secrets the EDC should consume.

---

## Configuration

| Key                                         | Description                                                                                                      | Mandatory | Default          |
|:--------------------------------------------|:-----------------------------------------------------------------------------------------------------------------|-----------|------------------|
| edc.vault.hashicorp.url                     | URL to connect to the HashiCorp Vault                                                                            | X         |                  |     |
| edc.vault.hashicorp.token                   | Value for [Token Authentication](https://www.vaultproject.io/docs/auth/token) with the vault                     | X         |                  |     |
| edc.vault.hashicorp.timeout.seconds         | Request timeout in seconds when contacting the vault                                                             |           | `30`             |
| edc.vault.hashicorp.health.check.enabled    | Enable health checks to ensure vault is initialized, unsealed and active                                         |           | `true`           |
| edc.vault.hashicorp.health.check.standby.ok | Specifies if a vault in standby is healthy. This is useful when Vault is behind a non-configurable load balancer |           | `false`          |
| edc.vault.hashicorp.api.secret.path         | Path to the [secret api](https://www.vaultproject.io/api-docs/secret/kv/kv-v1)                                   |           | `/v1/secret`     |
| edc.vault.hashicorp.api.health.check.path   | Path to the [health api](https://www.vaultproject.io/api-docs/system/health)                                     |           | `/v1/sys/health` |

## Health Check

The HashiCorp Vault Extension is able to run health checks. A health check is successful when the vault is
_initialized_, _active_ and _unsealed_. Successful health checks are logged with level _FINE_. Unsuccessful health
checks will be logged
with level _WARNING_.

---

### Health Checks

If your project uses the Tractus-X HashiCorp Vault please set `edc.vault.hashicorp.health.check.standby.ok` to _true_.
Otherwise, the health check would fail if the Vault is in standby.

```plain
# Logs of successful check with standby vault
[2022-08-01 14:48:37] [FINE   ] HashiCorp Vault HealthCheck successful. HashicorpVaultHealthResponsePayload(isInitialized=true, isSealed=false, isStandby=true, isPerformanceStandby=false, replicationPerformanceMode=disabled,replicationDrMode=disabled, serverTimeUtc=1659365317, version=1.9.2, clusterName=vault-cluster-4b193c26, clusterId=83fabd45-685d-7f8d-9495-18fab6f50d5e)
```

---

## Example: Create & Configure an OAuth2 private key

### Insert private key into HashiCorp Vault

```bash
cat << EOF | /bin/vault kv put secret/my-private-key content=-
        -----BEGIN PRIVATE KEY-----
        <PRIVATE KEY CONTENT>
        EOF
```

### Configure key in the EDC

```bash
EDC_OAUTH_PRIVATE_KEY_ALIAS: my-private-key
```

or

```bash
edc.oauth.private.key.alias=my-privatekey
```

## Example

```properties
#########
# Vault #
#########
edc.vault.hashicorp.url=https://vault.demo.tractus-x.net
# or even better configure token as k8 secret
edc.vault.hashicorp.token=<token>
edc.vault.hashicorp.api.secret.path=/v1/<tenant>/
edc.vault.hashicorp.health.check.standby.ok=true
```
