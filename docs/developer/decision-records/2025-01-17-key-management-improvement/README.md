# Improvements to the handling of cryptographic keys and signatures

## Decision

We will extend the capabilities of our private key management in the following aspects:

- key rotation: key pairs should get rotated automatically by the `SigningService`
- remote signing: it should be possible to cryptographically sign payloads "remotely", i.e. outside of EDC

## Rationale

Production-grade enterprise deployments of the Connector and other components will require
centralized key management capabilities, where key pairs are automatically rotated to defend against key attrition. This
is typically implemented by HSMs such as Hashicorp Vault.

In a similar fashion EDC will provide a feature to "remotely sign" payloads. Instead of transmitting the private key
from the vault to EDC over the network, and signing the payload locally, which carries some risk of exposure, leakage,
mishandling etc.,
EDC will transmit the payload to the vault, have the payload signed/encrypted there and transmit the signature or
encrypted payload back.

With this, the private key never leaves the vault, thus avoiding any risk of key exposure or leakage.

> _Note that there may be a penalty on latency, especially when transmitting very large payloads, such as
VerifiablePresentations that contain many large VerifiableCredentials!_

## Approach

### Implementation Remote signing

- add new interface `SigningService` that declares two methods:
  ```java
  public interface SigningService {
    byte[] sign(String keyId, byte[] data);  
    boolean verify(String keyId, byte[] signature);
  }
  ```
- the default implementation performs signing and verifying exactly as implemented currently, pulling keys out of the
  vault and performing the signing locally.
- For each vault technology, there would be a `SigningService` implementation
- Custom implementations of `JWSSigner` and `JWSVerifier` (from Nimbus) named `RemoteJwsSigner` and `RemoteJwsVerifier`
  respectively, will delegate signing and verifying to the `SigningService`.
  These objects are provided by an extension.
- A configuration value `edc.signing.remote.enable` will be added to enable/disable remote signing

### Implementation Key Rotation

- Add `rotate(String keyId, Map<String, Object> keyProperties)` method to the `SigningService` to specify the algorithm
  and cryptographic properties as well as the TTL (time-to-live)
- IdentityHub: `KeyPairServiceImpl` calls `SigningService#rotate()` when rotating keys
- Configuration values:
    - `edc.signing.keys.rotation.enable`: enable/disable automatic rotation
    - `edc.signing.keys.rotation.algorithm`: the cryptographic algorithm
    - `edc.signing.keys.rotation.ttl`: time-to-live (in days) for keys, until they get rotated automatically

_Note: necessary implementations on Technology repos are not listed here_

### Market overview

|                  | Hashicorp Vault                                                         | Azure KeyVault                                                     | AWS KMS                                 |
|------------------|-------------------------------------------------------------------------|--------------------------------------------------------------------|-----------------------------------------|
| Remote Signing*) | natively supported (REST)                                               | natively supported (REST, SDK)                                     | natively supported (REST, SDK)          |
| Key Rotation     | automatic and manual rotation <br/>supported via Transit Secrets Engine | automatic and manual rotation supported <br/>via rotation policies | automatic and manual rotation supported |

*) automatic notifications upon rotation (auto or manual) are supported, but have to be implemented using CSP-specific
eventing mechanisms, such as EventBridge (AWS) or EventGrid (Azure).
