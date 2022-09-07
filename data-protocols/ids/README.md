# IDS

This IDS extension represents a collection of all IDS extensions, that are required to run an IDS connector.

These extensions bridge between EDC core and IDS.

## Included Extensions

- [data-protocols:ids:ids-api-configuration](ids-api-configuration/)
- [data-protocols:ids:ids-api-multipart-dispatcher](ids-api-multipart-dispatcher-v1/)
- [data-protocols:ids:ids-api-multipart-endpoint](ids-api-multipart-endpoint-v1/)
- [data-protocols:ids:ids-core](ids-core/)
- [data-protocols:ids:ids-jsonld-serdes](ids-jsonld-serdes/)
- [data-protocols:ids:ids-spi](ids-spi/)
- [data-protocols:ids:ids-token-validation](ids-token-validation/)
- [data-protocols:ids:ids-transform](ids-transform-v1/)

## IDS Contracts

### IDS Contract Offer

- Contract Offers, that are provided by the EDC Contract Offer Frameworks, are created in the IDS ecosystem
- Assets, that are not part of a Contract Offer, are not created in the IDS ecosystem
- In the IDS data model
    - each Contract Offer is mapped to its own IDS Resource
    - each Asset is mapped to its own IDS Representation
    - each Asset is mapped to its own IDS Artifact

### IDS Messaging

#### Description Request Message

- As not-negotiated ContractOffers are not persisted, it is not possible to send IDS DescriptionRequestMessages for the
  corresponding IDS Resources, where they are mapped to.
