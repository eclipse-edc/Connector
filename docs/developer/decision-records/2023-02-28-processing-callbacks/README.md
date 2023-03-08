# Processing Callbacks

## Decision

Implement the ability for clients to specify callback endpoints to be invoked at specific points during request processing:

- Callback endpoints can be specified for _Contract Negotiation_ and _Transfer Process_ state transitions when a request is made via the _Management API_
- Callback endpoints are scoped to the particular state machine instance associated with the request.
- Clients can request the callbacks to be invoked *transactionally* or on a *best-effort* basis.
- Callback endpoints can be remote or local (in-process). The initial implementation will support HTTPS endpoints.

## Rationale

Currently, the EDC provides support for listeners (e.g. `TransferProcessListener`) and event subscribers (`EventSubscriber`). However, there is no way to provide callback endpoint
information that can be used by listener and subscriber implementations when a request is made via the Management API. Processing callbacks will address this gap and provide
support for invoking HTTPS endpoints.

> Note, this functionality can also be used to align EDR handling with the migration to the IDS Dataspace Protocol specifications.

## Configuration Scheme

Support for two types of callbacks will be implemented: static endpoints and dynamic endpoints. Static endpoints are configured in the EDC and referenced by identifier; dynamic
endpoints are specified at the time a request is made.

### Static Endpoints

The static endpoint registration will be done using a configuration extension. This allows for the development of more elaborate endpoint registration mechanisms in the future.

A static endpoint will be registered using EDC configuration:

```
edc.callback.endpoint1.uri=
edc.callback.endpoint1.context=
edc.callback.endpoint1.events=
edc.callback.endpoint1.transactional=
edc.callback.endpoint1.auth-key=
edc.callback.endpoint1.auth-code-id=
```

The previous configuration will create a static endpoint registration for the id `endpoint1` with the following properties:

- The `uri` is scheme-specific address used to dispatch a callback notification.
- The `context` is: `contract-negotiation` or `transfer-process`
- The optional `events` property is a collection of processing events corresponding to a state machine state. If absent, the default value is all events. The '*' character may be
  used as a wildcard to explicitly denote all events.
- If the optional `transactional` property is true, callback dispatches will be conducted transactionally (on supported configurations) during the state machine transition. The
  default is false.
- The optional `auth-key` property defines the transport specific header to use for sending an authentication value. If the `auth-code-id` value is set and this property is not
  set, the value `Authorization` will be used.
- The optional id to use when resolving a required callback authorization key from the Vault.

Static callback endpoints are referenced by their id when a client makes a request. They may be reused by multiple requests.

### Dynamic Endpoints

Dynamic endpoints are registered as part of a client request and are therefore scoped to the latter. Dynamic endpoints have three properties:

- `uri`
- `context`
- `events`
- `transactional`

Dynamic endpoints do not have explicit API keys. Security can be provided at the network layer or through a URI with an randomly-generated single-use path part.

## New Services

### The CallbackRegistry

A `CallbackRegistry` to manage static endpoints will be added to the `control-plane-spi` module:

```
package org.eclipse.edc.connector.spi.callback

public interface CallbackRegistry {

  void register(CallbackAddress address);
  
  List<CallbackAddress> resolve(String context, String state);

}
```

This registry will be implemented in `control-plane-aggregate-services` as a simple, in-memory service. Dynamic endpoint will be persisted with the transfer process.

## Impact on Existing Services

### Management API

`ContractNegotiationDto` and `TransferRequestDto` will be enhanced to include a collection of `CallbackAddress` types that model dynamic endpoint configurations:

```
public class CallbackAddress {

    private String uri;
    
    private String context;
    
    private Set<String> events;

    private boolean transactional 

    ....
}
```

Provided dynamic callback addresses will be persisted alongside the request.

### State Machines

During a state transition, a `ContractNegotiationEventListener` or `TransferProcessEventListener` will resolve `CallbackAddress` entries matching the state to be transitioned to by querying
the `CallbackRegistry` and current `TransferProcess`. If a resolved callback is marked as transactional, an invocation error will mark the current transaction as rollback-only;
otherwise, invocation errors will be logged and the transaction will proceed. Invocations will use the standard EDC retry mechanism.

> Note that the event framework and EventRouter will be refactored to include contract negotiation and transfer process data as part of a pre-requisite Decision Record.   

> Note that transactional callback endpoints must be idempotent. De-duplication can be performed by comparing the associated process id and state with previous invocations.

#### Dispatching

Dispatching to callback endpoints will be performed by registered `RemoteMessageDispatcher`s corresponding to the protocol part of the callback URI. These dispatchers will send
events representing the `ContractNegotiation` or `TransferProcess` data received by the event listener.

### DataFlowController

The `DataFlowController` interface will need to be changed to return an explicit result type. See below for details.

## EndpointDataReferenceReceiver and Processing Callbacks

The existing mechanism for propagating and handling `EndpointDataReference`s will be adapted to processing callbacks. EDR configuration keys, e.g. `edc.receiver.http.dynamic.`
and `edc.receiver.http.` will be aliases for the registration of `CallbackAddress` configurations.

> Note we need to discuss whether to keep support of fallback `edc.receiver.http.dynamic.` endpoints. Also, we need to discuss continuing to support `EndpointDataReferenceReceiver`
> implementations. The implementations in EDC can be converted to `RemoteMessageDispatcher`s but we need to discuss how to handle end-user supplied `EndpointDataReferenceReceiver`
> implementations.

The most significant change will involve propagating the EDR from the provider to the client. The new IDS protocol `TransferStartMessage` contains a `dataAddress` field that can be
used to propagate the EDR instead of requiring the `DataFlowController` to send the EDR out-of-band to the client. The `DataFlowController` will need to return the EDR from
the `initiateFlow` method using a `DataFlowResponse`:

```
 StatusResult<DataFlowResponse> initiateFlow(DataRequest dataRequest, DataAddress contentAddress, Policy policy);
```

If a reference is returned, it will be stored in the Vault and then sent with the `TransferStartMessage` by a dispatching listener. The client will receive the EDR and propagate it
as part of its transfer process state transition.


