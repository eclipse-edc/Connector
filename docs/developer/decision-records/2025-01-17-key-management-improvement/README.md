# Improvements to the handling of cryptographic keys and signatures

## Decision

We will extend the capabilities of our private key management in the following aspects:

- key rotation: key pairs should get rotated automatically by the `Vault`
- remote signing: it should be possible to cryptographically sign payloads "remotely", i.e. outside of EDC

## Rationale

Production-grade enterprise deployments of the Connector and other components will require 
centralized key management capabilities, where key pairs are automatically rotated to defend against key attrition. This
is typically implemented by HSMs such as Hashicorp Vault.

In a similar fashion EDC will provide a feature to "remotely sign" payloads. Instead of transmitting the private key
from the vault to EDC over the network, and signing the payload locally, which carries some risk of exposure, leakage, mishandling etc.,
EDC will transmit the payload to the vault, have the payload signed/encrypted there and transmit the signature or encrypted payload back.

With this, the private key never leaves the vault, thus avoiding any risk of key exposure or leakage.

> _Note that there may be a penalty on latency, especially when transmitting very large payloads, such as
VerifiablePresentations that contain many large VerifiableCredentials!_

## Approach

### Implementation Remote signing

- add `byte[] sign(String keyId, byte[] data)` method to `Vault`
- add `boolean verify(String keyId, byte[] signature)` method to `Vault`
- implement remote signing on Hashicorp Vault (via REST)
- implement custom `JWSSigner` (from Nimbus) named `RemoteJwsSigner`, that delegates signing to the `Vault`. This custom
  `JWSSigner` is provided as extension.
- implement custom `JWSVerifier` (from Nimbus) named `RemoteVerifier`, that delegates verifying to the `Vault`. This custom
  `JWSVerifier` is provided by an extension.
- configuration values: `edc.vault.signing.enable` to enable/disable remote signing

### Implementation Key Rotation

- add `rotate(String keyId, Map<String, Object> keyProperties)` method to the `Vault` to specify the algorithm and
  cryptographic properties as well as the TTL (time-to-live)
- implement key rotation in Hashicorp Vault (via REST)
- IdentityHub: `KeyPairServiceImpl` calls `Vault#rotate()` when rotating keys
- configuration values:
    - `edc.vault.keys.rotation.enable`: enable/disable automatic rotation
    - `edc.vault.keys.rotation.algorithm`: the cryptographic algorithm
    - `edc.vault.keys.rotation.ttl`: time-to-live (in days) for keys, until they get rotated automatically

_Note: necessary implementations on Technology repos are not listed here_

### Market overview

|                  | Hashicorp Vault                                                         | Azure KeyVault                                                     | AWS KMS                                 |
|------------------|-------------------------------------------------------------------------|--------------------------------------------------------------------|-----------------------------------------|
| Remote Signing*) | natively supported (REST)                                               | natively supported (REST, SDK)                                     | natively supported (REST, SDK)          |
| Key Rotation     | automatic and manual rotation <br/>supported via Transit Secrets Engine | automatic and manual rotation supported <br/>via rotation policies | automatic and manual rotation supported |

*) automatic notifications upon rotation (auto or manual) are supported, but have to be implemented using CSP-specific
eventing mechanisms, such as EventBridge (AWS) or EventGrid (Azure).

### Backwards compatibility
To accomodate `Vault` implementations, that do not support either remote signing/verification or key rotation, the `Vault` interface will be amended with the following method:
```java
public interface Vault {
  default VaultCapabilities getVaultCapabilities() {
    return VaultCapabilities.Builder.newInstance()
            .supportsSigning(false)
            .supportsVerifying(false)
            .supportsAutomaticRotation(false)
            .supportsManualRotation(false)
            .build();
  }
// ...
}
```
and each `Vault` implementation must then overwrite the method with its capabilities. Client code that performs either of those actions should then check and inspect the vault's capabilities and fall back to local signing etc. if not supported.
