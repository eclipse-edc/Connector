# Dataplane Selection Improvements

## Decision

In the scope of `Dataplane Signaling`, a new way to manage dataplane registration and de-registration will be implemented

## Rationale

Currently, the dataplane registration is done manually by the operator through the management-api, this way is not optimal
because it could lead to errors, plus, there's no way for the control plane to ensure that the dataplane is still active
and ready to accept new transfer requests.

## Approach

The dataplane needs to register itself to the control plane.
To do that, it will need a way to be aware of its supported `source` types and `transferTypes`.

For the first, a new method need to be added to the `DataSourceFactory` interface, that tells which type is handled by the
factory:
```java
public interface DataSourceFactory {
    String supportedType();
}
```

doing so, the `canHandle` will be obsolete, and the factory will be bound to a specific type.

The `transferTypes` instead will need:
- for `PUSH` flows: the same method will be added to the `DataSinkFactory`, so collecting the types from the registered
  factories will give the set of supported `PUSH` types
- for `PULL` flows: the types are registered to the `PublicEndpointGeneratorService`


With this knowledge, the data-plane will be able at startup to register itself to the controlplane over the `control-api`
(and de-register itself at shutdown).

The dataplane will need to have a UUID configured to handle idempotency.

The controlplane will have the possibility to "heartbeat" the dataplane to verify its availability in order to be able
to build the catalog in the correct way and being able to tell if a requested transfer can be started or not.
