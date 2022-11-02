# Aggregate Service layer

## Decision

An "aggregate service" layer will be explicitly defined to provide a clear and simple interface to interact with the 
entities in a safe way.

Please note that in this document we're not referring to "services" as the components that are registered on the
runtime to achieve extensibility.

## Rationale

For `AggregateService` we intend a component that provides a clear and simple interface to interact with entities (such as
`Asset`s, `ContractNegotiation`s and so on) encapsulating some logic and features, like business validation, 
transactional context, exception handling, ...

Currently, they are located into the `data-management-api`. Moving them to an underlying layer will permit to other kind
of API to being exposed over them, benefiting from their offered features.

## Approach

### SPI

The service layer will be described by a generic `aggregate-service-spi` module that, in fact will contain only the
`AggregateServiceResult` class, that represent a `Result` returned by an `AggregateService` method call.

Every component will define its own aggregate service layer spi, for example `control-plane-spi` will contain all the
`AggregateService` interfaces of the `control-plane` as `AssetAggregateService`, `ContractNegotiationAggregateService`, 
`TransferProcessService`, etc...

That `control-plane-spi` module could then be used as a dependency to implement an extension that expose an API (for 
example, a GraphQL one).

### Implementation classes

The implementation classes will be located in a component-related module in the `core` folder, for example 
`control-plane-aggregate-services`, this module will be referenced by the `control-plane-core` BOM to make the 
`AggregateService`s registered at runtime.

For the control plane, the directory tree will look like:
```
├── core
│   ├── control-plane
│   │   ├── control-plane-aggregate-services
├── spi
│   ├── common
│   │   ├── aggregate-service-spi
│   ├── control-plane
│   │   ├── control-plane-spi
```
