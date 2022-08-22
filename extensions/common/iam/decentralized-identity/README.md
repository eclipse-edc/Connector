## Folder structure

These extensions contain modules that implement the "Decentralized Identifier" use case. In particular:

- `identity-did-spi`: contains extension points for the distributed identity subsystem
- `identity-did-crypto`: contains the cryptographic utilities
- `identity-did-core`: contains core services, including the `DidResolverRegistry`
- `identity-did-service`: contains the `DecentralizedIdentifierService`, which is an implementation of the `IdentityService`
  interface.
- `identity-did-web`: contains support for resolving Web DIDs. 
- `registration-service`: contains a periodic job that crawls the ION network for DIDs of a particular type
- `registration-service-api`: contains a REST API for the aforementioned registration service
- `identity-common-test`: contains a utility class to load an RSA key from a *.jks file. See
  also [here](identity-common-test/src/testFixtures/resources/readme-keystore.txt)

Those modules are still under development and should not be used in production scenarios! Code (APIs, interfaces,
implementations) are likely to change in the future and without notice.

## Terminology

- Verifiable Credential (VC) and JSON Web Tokens (JWT) are the same thing. Or: a VC is represented by a JWT
- the Verifier is a trusted third party, e.g. some company like Accenture used to verify additional object data
- "(additional) object data" refers to an arbitrary set of properties or a JSON structure that are stored in a
  connector's Hub
- DID and DID Document are the same thing
- VerifiableAssertion = hub data object

## Verification process

The following sequence has to be performed whenever a request is received:

C ... Consumer, P ... Provider

1. C presents JWT to P (in message header, i.e. in the `_securityToken_` field of C IDS messages)
1. P resolves the DID URL from the VC received from C
1. P resolves C's DID Document from ION and from it retrieves C's public key and C's Hub URL
1. P verifies C's VC using C's public key (from the DID Document)
1. P obtains object data from C's Hub:
    - P sends query to C's hub together with its DID url
    - C decrypts hub data object and re-encrypts with P's public key (obtained from P's DID)
    - P receives and decrypts C's hub data object
1. P obtains the Verifier's DID document from ION (Verifier's DID URL must be contained in the hub data object)
1. P uses Verifier's public key to verify C's object data

## General notes and restrictions

- The Verifier (or Attestor) in this demo is just another Key Pair
- DIDs are generated and anchored once during initial setup, it does **not** happen during deployment
- This will be one set of object data per hub and one hub per connector (so no filtering at this time)
- The hub runs in its separate runtime and exposes a simple GET API

This module is currently under development and will someday contain a Java-port of
the [ION-Tools](https://github.com/decentralized-identity/ion-tools) and parts of
the [ION-SDK](https://github.com/decentralized-identity/ion-sdk).

_Please do not use this module in a production environment yet, interfaces and implementations might change without
prior notification._
