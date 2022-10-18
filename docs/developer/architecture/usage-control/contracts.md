# Contracts

## Offering

The EDC connector is able to provide contract offers. This feature requires four different types of extensions:

- [Core Contract Extension](../../../../core/control-plane/contract-core/README.md)
- Extension that implements the **Asset Index**
- Extension that implements the **Contract Offer Framework**
- Extension that implements an API to access the offers, e.g. IDS extension

### Most important classes and interfaces

- **Contract Offer Service**
    - may be used to query offers
    - core implementation
- **Contract Offer Framework**
    - provide offers to the **Contract Offer Service**
    - create non-persistent **Contract Offers Templates** for a specific consumer
    - **Contract Offer Templates** target a range of **Assets**
    - implemented by extensions
- **Asset Index**
    - provides **Assets**
    - implemented by extensions

### Prototypic Sequence for Contract Offer Creation

![Offer Query](../catalog/diagrams/offer-query.png)
*The consumer connector requests a description, that contains the contract offers, from the provider.*

## Negotiation

TBD