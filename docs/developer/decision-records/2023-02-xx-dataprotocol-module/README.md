# Dataspace Protocol software architecture

## Decision

Create a new module for the Dataspace Protocol and align his structure on the existing ids module. This decision
was made, because in the Dataspace are some breaking changes in comparison to the existing ids module. Also it will
be possible for a defined time to support legacy software in the new versions of the EDC-Connector.

## Rationale

On implementing th Dataspace Protocol two options are the way to go. The first option is two use the exisiting 
ids module and refactor this module. The second way is to create a completly new module next to the existing ids
module. It is important to take the decision, because we like to support the old ids protocol for a specified time.

## Approach

### Assignment of modules to new structure

#### Module structure

Dataspace-Protocol

- spi
  - service
    - CatalogService
    - ContractNegotiationService
    - TransferProcessService
- core
  - controller
    - CatalogController
    - ContractNegotiationController
    - TransferProcessController
  - service
    - CatalogServiceImpl
    - ContractNegotiationServiceImpl
    - TransferProcessServiceImpl
  - ...
- api-configuration


### Additional information on existing modules and new modules

#### ids-multipart-* (deprecated)

Because the Dataspace Protocol does not use HTTP-Multipart Messages anymore this modules are not existing anymore.
Their work is handled by the both new modules **Controller Layer** and **Service Layer**.

#### spi

#### core

##### controller

In the new Dataspace Protocol every request context has his own individual REST-endpoint {catalog, negotiation, transfer-process}. 
Each of this contexts should own his own Extension, so parts can be easier exchanged by developers or you can maximize
the use of an Service Oriented Architecture. The Controller will register for every HTTP-Binding context a individual Handler.

Also it will be exist an build file to bunch all single Extension to one big Extension together.

##### service

Every single Service of the three contexts need to support every HTTP-Endpoint defined in the [Dataspace protocol](https://docs.internationaldataspaces.org/communication-guide-v2-draft/overview/readme)  

###### catalog

[Link to HTTP-Bindings of the catalog](https://docs.internationaldataspaces.org/communication-guide-v2-draft/catalog/catalog.binding.https)

```http request
POST /catalog/request endpoint
```


###### contract-negotiation
[Link to HTTP-Bindings of the contract negotiation](https://docs.internationaldataspaces.org/communication-guide-v2-draft/contract-negotiation/contract.negotiation.binding.https)


```http request
GET /negotiations/:id
```

```http request
POST /negotiations/request
```

```http request
POST /negotiations/:id/request
```

```http request
POST /negotiations/:id/events
```

```http request
POST /negotiations/:id/agreement/verification
```

```http request
POST /negotiations/:id/termination
```

Consumer Callback Path Bindings

```http request
POST /negotiations/:id/offers
```

```http request
POST /negotiations/:id/agreement
```

```http request
POST /negotiations/:id/events
```



###### transfer-process

[Link to HTTP-Bindings of the transfer process](https://docs.internationaldataspaces.org/communication-guide-v2-draft/transfer-process/transfer.process.binding.https)

```http request
GET /transfer-processes/:id
```

```http request
POST /transfer-processes/request
```

```http request
POST /transfer-processes/:id/start
```

```http request
POST /transfer-processes/:id/completion
```

```http request
POST /transfer-processes/:id/termination
```

```http request
POST /transfer-processes/:id/suspension
```

Consumer Callback Path Bindings

```http request
POST /transfer-processes/:id/start
```

```http request
POST /transfer-processes/:id/completion
```

```http request
POST /transfer-processes/:id/termination
```

```http request
POST /transfer-processes/:id/suspension
```


#### jsonld-serdes

General module for serializing and deserializing JSON-LD. Open another Decision Record about
JSON-LD. Move out to a more general part of the code

#### ids-token-validation

Ongoing discussion about to move this part of the module to the DAPS itself or it should stay 
<!-- 
// TODO 
-->


#### ids-transform

- Asset - Dataset
- Data Address - Distribution
- Catalog - Catalog
- ...
