## Folder structure

These extensions contain modules that implement the "Distributed Identity" use case. In particular:

- `identity-did-core`: contains code dealing with the Identity Hub
- `identity-did-service`: contains the `DistributedIdentityService`, which is an implementation of the `IdentityService`
  interface.
- `identity-hub-verifier`: contains an implementation of the `CredentialsVerifier` that uses an _IdentityHub_
- `verifiable-credentials`: contains code to generate signed JWTs that act as _VerifiableCredentials_
- `identity-did-spi`: contains domain-specific interfaces like the `IdentityHub` and the `IdentityHubClient`
- `registration-service`: contains a periodic job that crawls the ION network for DIDs of a particular type
- `registration-service-api`: contains a REST API for the aforementioned registration service
- `identity-common-test`: contains a utility class to load an RSA key from a *.jks file. See
  also [here](identity-common-test/src/testFixtures/resources/readme-keystore.txt)

Those modules are still under development and should not be used in production scenarios! Code (APIs, interfaces,
implementations) are likely to change in the future and without notice.

## Verification process

The following sequence has to be performed wenever a request is received:

A ... Consumer, B ... Provider

1. A presents JWT to B (in message header, i.e. in the `_securityToken_` field of a IDS messages)
1. B resolves the DID URL from the VC received from A
1. B resolves A's DID Document from ION and from it retrieves A's public key and A's Hub URL
1. B verifies A's VC using A's public key (from the DID Document)
1. B obtains object data from A's Hub:
    - B sends query to A's hub together with its DID url
    - A decrypts hub data object and re-encrypts with B's public key (obtained from B's DID)
    - B receives and decrypts A's hub data object
1. B obtains the Verifier's DID document from ION (Verifier's DID URL must be contained in the hub data object)
1. B uses Verifier's public key to verify A's object data

## Terminology

- Verifiable Credential and JWT are the same thing. Or: a VC is represented by a JWT
- the Verifier is a trusted third party, e.g. some company like Accenture used to verify additional object data
- "(additional) object data" refers to an arbitrary set of properties or a JSON structure that are stored in a
  connector's Hub
- Hub and Identity Hub are the same thing
- DID and DID Document are the same thing
- VerifiableAssertion = hub data object

## General notes and restrictions

- The Verifier (or Attestator) in this demo is just another Key Pair
- DIDs are generated and anchored once during initial setup, it does **not** happen during deployment
- The will be one set of object data per hub and one hub per connector (so no filtering at this time)
- The hub runs in its separate runtime and exposes a simple GET API
