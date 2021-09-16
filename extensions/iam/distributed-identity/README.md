These extensions contain modules that implement the "Distributed Identity" use case. In particular:

- `identity-did-core`: contains code dealing with the Identity Hub
- `identity-did-service`: contains the `DistributedIdentityService`, which is an implementation of the `IdentityService`
  interface.
- `identity-did-spi`: contains domain-specific interfaces like the `IdentityHub` and the `IdentityHubClient`
- `registration-service`: contains a periodic job that crawls the ION network for DIDs of a particular type
- `registration-service-api`: contains a REST API for the aforementioned registration service
- `identity-common-test`: contains a utility class to load an RSA key from a *.jks file. See
  also [here](identity-common-test/src/testFixtures/resources/readme-keystore.txt)

Those modules are still under development and should not be used in production scenarios! Code (APIs, interfaces,
implementations) are likely to change in the future and without notice.

