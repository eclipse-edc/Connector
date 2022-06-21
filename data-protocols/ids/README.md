# IDS

This IDS extension represents a collection of all IDS extensions, that are required to run an IDS connector.

These extensions bridge between EDC core and IDS.

## Included Extensions

- [data-protocols:ids:ids-spi](../ids-new/ids-spi/README.md)
- [data-protocols:ids:ids-core](../ids-new/ids-core/README.md)
- [data-protocols:ids:ids-api-multipart](../ids-new/ids-api-multipart-endpoint-v1/README.md)

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
