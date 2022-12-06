# Add pagination to IDS requests

## Decision

Until such time that a holistic refactoring of the IDS protocol module can be done, we will implement a short-term fix
to accommodate the need to have pageable requests.

Currently, this is only needed for the `CatalogRequest`, as it is the only request that returns a set of data (
i.e. `ContractOffers` wrapped in a `Catalog`). However, due to the way how the multipart request handlers are
architected now, it is unfortunately also necessary to update the other `*RequestHandler` classes.

## Rationale

To alleviate problems that surfaced when transmitting large response bodies over HTTP (e.g. a `Catalog` with
5000 `ContractOffers`), it is necessary to splice the response into several digestible chunks. The exact composition of
those chunks is determined by so-called "pagination parameters". Clients must then re-send the request with updated
pagination parameters until no more items are received.

## Approach

We introduce the concept of a `Range`, which contains `from` and a `to` fields. To make this work over IDS two things
are necessary:

1. transmit `from` and `to` over IDS
2. have a piece of code on the requesting side, that re-sends the request until no more items are received

### 1. Encoding `Range` into IDS

The easiest way of doing this to add two integers to the IDS message:

```
// in MultiPartCatalogDescriptionRequestSender.java
message.setProperty(Range.FROM_NAME,request.getRange().getFrom());
message.setProperty(Range.TO_NAME,request.getRange().getTo());
```

Then, on the receiving end, they simply are extracted again:

```
// in ConnectorDescriptionRequestHandler.java 
var from=getInt(descriptionRequestMessage,Range.FROM_NAME,0);
var to=getInt(descriptionRequestMessage,Range.TO_NAME,Integer.MAX_VALUE);
var range=new Range(from,to);
```

Note that `getInt(...)` simply is a null-safe way to either get the value, or use a default value.

### 2. Collecting results of all requests

Instead of calling the `DispatcherRegistry` directly, there will be a collaborator object that handles this. This
pseudocode demonstrates this:

```
int size = to - from;
do {
    var offers = sendCatalogRequest(..., from, to);
    from += size;
    to += size;
} while(offers.size() > 0);
```

Since we do not know the total size of items, we must employ a `do-while`-style to re-send the requests.

## Future improvements

As this is only a temporary fix, which will likely get replaced once a larger refactoring/redesign of IDS is done, there
are several things to highlight here.

- non-deterministic looping: currently, the total number of items is not known, therefore only non-deterministic loops
  are available.
- Blocking `CompletableFutures`: as a consequence of non-deterministic looping, it is not possible to parallelize
  multiple requests, which means we must wait on them with `join()`. If in the future we know the total number of items,
  we can further optimize this.
- Object hierarchy of the `DescriptionRequestHandler`: there is the `AbstractDescriptionRequestHandler`, which is a base
  class for all other request handlers, except the `ConnectorDescriptionRequestHandler`. Obviously, this needs some
  cleaning up, as is it questionable whether all of them are needed at all.
- differentiate between pageable and non-pageable requests