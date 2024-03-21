# Transfer Type

## Decision

We will decouple `DataAddress#type` from the actual type of transfer by introducing a `transferType` concept, that will drive 
the selection of the right `DataPlane` for a specific `TransferRequest`. 

## Rationale

Currently `EDC` chooses the `DataPlane` using the `DataFlowController`s mechanism:

For `Pull` and `Push` scenario `EDC` ships:

- `ConsumerPullTransferDataFlowController`
- `ProviderPushTransferDataFlowController`

That are engaged with the following rules:

`ConsumerPullTransferDataFlowController`:

```java
 public boolean canHandle(TransferProcess transferProcess) {
    return HTTP_PROXY.equals(transferProcess.getDestinationType());
 }
```

and `ProviderPushTransferDataFlowController`:

```java
public boolean canHandle(TransferProcess transferProcess) {
   return !HTTP_PROXY.equals(transferProcess.getDestinationType());
}
```

which limits the `ConsumerPullTransferDataFlowController` to `HttpProxy`, while the `ProviderPushTransferDataFlowController`
takes the rest.

Additionally by using the `DataAddress#type` of the destination as the driver for the `DataFlowController`'s selection,
users are forced to pass a `DataAddress` even in the case where it's not really a `DataAddress` but just a marker e.g. `HttpProxy`.

How `DataPlane`s are selected through the `DataFlowManager` will change with the `Data Signaling DR`, 
but in the meanwhile we will introduce the `transferType` to provide additional information about the transfer request
to `DataFlowController`s.

## Approach

The introduction of a `transferType` will impact the following area/components:

- Catalog
- Transfer
- DataFlowControllers

### Catalog

The new `transferType` will be taken into account when creating the `Distribution` list for an `Asset` using the `DistributionResolver`.

The `DataFlowManager` will be the component with the responsibility for creating such list.

### Transfer

An additional field `transferType` will be required when starting a transfer process in:

- `TransferRequest`
- `TransferProcess`

The `transferType` will be reflected also in the DSP `TransferRequestMessage` with the field [`format`](https://docs.internationaldataspaces.org/ids-knowledgebase/v/dataspace-protocol/transfer-process/transfer.process.binding.https#2.5-the-provider-transfers-request-resource) .

### DataFlowControllers

Implementors of `DataFlowController` should take into account the new `transferType` field when checking if they    
can handle a transfer process. The default EDC implementations will be tackle with `Data Signaling DR`.
