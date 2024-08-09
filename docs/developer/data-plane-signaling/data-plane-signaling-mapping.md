# Detailed technical aspects of mapping transfer formats onto data planes

## The transfer format

The transfer format is a descriptor of how certain `Assets` are available for transfer. This means, the same physical
Asset could be available via multiple formats, e.g. HTTP, S3, Azure Blob, etc. In DCAT terms there is one `Distribution`
per format.

Practically, the format conforms to the following schema:

```ebnf
format    ::= DESTTYPE DASH FLOWTYPE ;
DESTTYPE  ::= [a-zA-Z0-9] ;
DASH      ::= "-";
FLOWTYPE  ::= "PUSH" | "PULL" ;
```

The first section, the `DESTTYPE`, is arbitrary, and could be any character sequence. For example one could
define `SuperFastQuantumTransfer-PUSH`, so long as there is a data plane that can fulfil super-fast quantum transfers.
It should be noted that the _format_ is a concept that is immanent only to the control plane.

When registering data planes with the `DataPlaneSelector`, there must be a link between the (arbitrary) format and the
transmission technology utilized in the data plane. The `DataPlaneSelectorService` must be able to dispatch an
incoming `DataRequest` to an appropriate data plane. Consequently, the format must be _mapped_ onto a data plane
descriptor.

_Alternate names: catalog format, distribution format, catalog type, transfer type_

## Mapping the format

The data plane does not know anything about DCAT or `Assets`, thus the _format_ does not exist there as a concept. A
data plane only knows about data sources, data destinations and flow directions. (The source type is not relevant for
this document).

The _format_ is mapped onto a tuple consisting of the `destType`, which is a String, and the  `flowType`, which is
an `Enum`. This tuple is called a `DataPlaneDescriptor` as it identifies a specific data plane:

```java
public record DataPlaneDescriptor(String destinationType, FlowType direction) {
}
```

This mapping is done in
the control plane, and it must be extensible, for example some data planes might map

```
Http-PUSH -> destType=Http, flowType=PUSH
```

but others might have a different format representation, that maps to the same technical transmission stack:

```
SuperFastQuantumTransfer-PUSH -> destType=Http, flowType=PUSH
```

from that it follows that there must be a configurable/extensible way of performing the mapping:

```java
var tokens = format.split("-");
var destType = mappingRegistry.getOrDefault(token[0], tokens[0]);
var flowType = FlowType.parse(tokens[1]);
```

This implies, that the mapping from format to `DataPlaneDescriptor` must be unambiguous. Ambiguity may be countered by
random selection.

## Impact on Authorization

Authorization is only possible when the flow type is `PULL`, because only then does the provider data plane retain
access control. (In PUSH scenarios, the consumer data plane may use authorization, however).

Further, token creation is dispatched based on the _destination type_, as the format does not exist as a concept in the
data plane. If multiple formats are mapped to the same destination type (and thus: transmission stack), but should
have different auth token schemes and authorization mechanisms, _separate data planes are needed_.

The `FlowType` will replace the `transferType` field on the `DataFlowStartMessage`. If the `flowType==PUSH` the
authorization is bypassed.

## Examples of mapped formats

* `Http-PULL` -> `destType = Http`, just connects the streams
* `Http-PUSH`  -> `destType = Http`, connects the streams _and_ starts the transfer
* `HttpAas-PUSH` -> `destType = Http`
* `HttpAas-PULL` -> `destType = Http`
* `AmazonS3-PULL` -> `destType = AmazonS3`, EDR contains bucket data plus temporary access token. Dataplane may transfer
  data into a staging bucket.
* `AmazonS3-PUSH` -> `destType = AmazonS3`

## Data plane registration

Several different _format_ strings could map to the same combination of `destType` and `flowType`. For example there
could be scenarios where the following mapping is implemented:

```
Http-PULL         -> destType=Http, flowType=PUSH  //only performs basic authentication
HttpSecure-PULL   -> destType=Http, flowType=PUSH  //adds some highly secure authorization scheme
```

When registering those data planes, the format is needed as additional parameter, to be able to distinguish between
them. In other words, dispatching solely based on the `DataPlaneDescriptor` may produce ambiguous results.