# Add filtering to IDS requests

## Decision

Extend existing `CatalogRequest` object with list of criteria used to filter `Assets` within `Catalog` response.

## Rationale

In a large scale environment, with multiple EDCs holding the information about hundreds of thousands of `Assets`, instead of 
looping thought all of the `ContractOffers`, there is a need to narrow the results down to even single offer. For example, and 
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
 Optional<Object> maybeFilter = Optional.ofNullable(message.getProperties().get(CatalogRequest.FILTER));

 if (maybeFilter.isPresent()) {
   criterions = objectMapper.convertValue(maybeFilter.get(), new TypeReference<>() {});
 }
```

And now, they can be applied to `AssetIndex` search query when constructing the `Catalog` response
