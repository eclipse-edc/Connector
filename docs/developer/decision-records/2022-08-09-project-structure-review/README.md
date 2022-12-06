# Title of the Decision Record

## Decision

The project structure needs to be changed, to follow a "component" structure.

## Rationale

Currently it's hard for a new developer or adopter to understand the different components of the application are composed, in particular 
`control-plane`, `data-plane`, `federated-catalog` etc...

> A component is a particular EDC combination of dependencies that respond to a particular need, e.g.: `control-plane`, `data-plane`, `data-plane-selector`.
This would help developers to orient themselves in the codebase more easily and separate responsibilities in a better way.

The goal of this review is to have a clearer structure.

## Approach

### Spi
The `spi` module should be refactored as:
```
spi/common/catalog-spi
spi/common/core-spi
spi/common/transport-spi
spi/common/web-spi
spi/control-plane
spi/control-plane/asset-spi
spi/control-plane/contract-spi
spi/control-plane/policy-spi
spi/control-plane/transfer-spi
spi/data-plane/data-plane-spi
spi/data-plane/data-plane-selector-spi
```
Every component should have its own `spi` module, then other "common" spi could exist as well (like `core`, `catalog`, `transport` and `web`)

Some details:
- the `asset-spi` would be extracted from the `core-spi`
- the `*Service` interfaces should be moved to the `control-plane/*-spi`

### Core
The responsibility of the core modules is to provide a working base version of every component, so they should provide
a default implementation for every interface present in the relative `spi` module, so these three modules will be introduced:
- `control-plane-core`: would contain all the default implementations of the stores and the services and an extension that register them as default
- `data-plane-core`: is the current `data-plane-framework` extension, will register all the default implementations
- `data-plane-selector-core`: is the current `selector-core` module

All of them should extend the `base-core` module. The `defaults` module would probably disappear (replaced by the `<component>-core` ones)

### Extensions

This component separation will be put in place also in the `extensions` folder, as some extensions are needed only by one of the components, for example:
- `control-plane`: data management api, provisioners (s3, blobstorage, http...), "stores" sql/cosmos implementations (for assets, contracts, transfer process, policies...), data-plane-selector client, ...
- `data-plane`: implementations of the source/sink interfaces like `data-plane-http`, `data-plane-s3`, etc.., data plane specific api (`data-plane-api` module)
- `data-plane-selector`: implementations of the `DataPlaneInstanceStore`
- common (needed by more than one component): observability api, vault, configuration, http, loggers, iam, ...