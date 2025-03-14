# Prioritized transfer services

## Decision

We will add a priority to `TransferServices` within the `TransferServiceRegistry`. `TransferServices` can then either
be registered with a specific priority or, when registered without specifying one, be assigned the default priority.
When a `TransferService` is selected, it will be chosen by priority.

## Rationale

For a given transfer, there may be multiple `TransferServices` which can handle the transfer and, for a given use case,
a specific `TransferService` may be preferred over others. Right now, there is no way to ensure that a specific
`TransferService` is chosen. Instead, the first applicable one is selected. By assigning a priority to the
`TransferServices`, it can be ensured that more specialized services are chosen over less specialized ones, if both are
applicable.

## Approach

The implementation will be similar to the registration and selection of `DataFlowControllers` in the
`DataFlowManagerImpl`, as these are also based on a prioritization.

### Registration

A new method will be added to the `TransferServiceRegistry` for registering a `TransferService` with a priority: 

```java
void registerTransferService(int priority, TransferService transferService);
```

The existing method for registering a `TransferService` without assigning a priority will default to using priority 0.
This also ensures that existing registrations continue working in the same way.

Within the `TransferServiceRegistryImpl`, a record will be created which links a `TransferService` to its priority.
Internally, the `TransferServiceRegistryImpl` will then work with this new record.

```java
record PrioritizedTransferService(int priority, TransferService service) { }
```

### Selection

At the moment, a `TransferServiceSelectionStrategy` is used to select a `TransferService`. This will be deprecated
in favor of the prioritization. Selection will then be handled by filtering for all applicable services and choosing
the one with the highest priority:

```java
@Override
@Nullable
public TransferService resolveTransferService(DataFlowStartMessage request) {
    return transferServices.stream()
            .filter(pts -> pts.service.canHandle(request))
            .sorted(Comparator.comparingInt(pts -> -pts.priority))
            .map(PrioritizedTransferService::service)
            .findFirst().orElse(null);
}
```

As by our deprecation policy the `TransferServiceSelectionStrategy` needs to remain functional until removed, both
solutions will be applied in parallel until its removal. Therefore, the `TransferServiceSelectionStrategy` will
for now be applied as a fallback in case no `TransferService` with a priority greater than 0 is present.
