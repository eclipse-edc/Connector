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

Currently they are located into the `management-api`. Moving them to a separate layer will permit other kinds of
APIs to use them in an equal manner benefiting from their offered features.

## Approach

### SPI

The service layer will be described by a generic `aggregate-service-spi` module that will contain only the
`AggregateServiceResult` class, that represent a `Result` returned by an `*AggregateService` method call.

Every component will define its own aggregate service layer spi, for example `control-plane-spi` will contain all the
`AggregateService` interfaces of the `control-plane` as `AssetAggregateService`, `ContractNegotiationAggregateService`, 
`TransferProcessAggregateService`, etc...

That `control-plane-spi` module could then be used as a dependency in an extension that exposes an API (for example a
GraphQL one).

### Implementation classes

The implementation classes will be located in a component-related module in the `core` folder, for example 
`control-plane-aggregate-services`, this module will be included in the `control-plane-core` BOM to register the 
`*AggregateService`s.

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
