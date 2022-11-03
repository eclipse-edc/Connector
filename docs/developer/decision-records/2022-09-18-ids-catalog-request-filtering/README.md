# Add filtering to IDS catalog requests

## Decision

Extend existing `CatalogRequest` object with list of criteria used to filter `Assets` within `Catalog` response.

## Rationale

In a large scale environment, with multiple EDCs holding the information about hundreds of thousands of `Assets`, instead of 
looping thought all of the `ContractOffers`, there is a need to narrow the results down to even single offer. For example, an 
`Asset` might hold the 'special' type of data (like registry or database) and should be searchable by its type.

## Approach

As the logic for querying `AssetIndex` by `QuerySpec` is already in place, all we have to do is to pass filtering criteria via IDS Api:


`filter` property has to be added to the IDS message:

```
// in MultiPartCatalogDescriptionRequestSender.java
message.setProperty(CatalogRequest.FILTER, request.getFilter());
```

Then, on the receiving end, it's simply extracted:

```
// in DescriptionRequestHandler.java 
var querySpec = ofNullable(message.getProperties().get(CatalogRequest.FILTER))
    .map(map -> objectMapper.convertValue(map, typeRef))
    .orElse(/*should never occur!!*/)
```

Existing `ContractOfferQuery` will be used to transport both `Range` and filtering criteria to lower layers.

And now, they can be applied to `AssetIndex` search query when constructing the `Catalog` response:

1) pass Criterions to the `catalog.org.eclipse.CatalogServiceImpl.getDataCatalog()`
2) attach to the existing `ContractOfferQuery` object
3) in `ContractOfferServiceImpl.queryContractOffers()`, merge with `AssetSelectorExpressions` from contract definitions.

## Considerations and limitations

- there is no standardized query language nor will there be one for the foreseeable future;
- querying is based on the "Canonical format": (https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/blob/main/docs/developer/sql_queries.md),
  i.e. the schema of the queried objects, i.e. their Java class. That implies that the client must have knowledge of the schema; 
- that schema is subject to change without special notice.
