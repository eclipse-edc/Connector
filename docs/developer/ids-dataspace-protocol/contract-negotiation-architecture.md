# Dataspace Protocol Contract Negotiation Architecture

The EDC will be upgraded to implement and be fully compliant to the __Dataspace Protocol Contract Negotiation Protocol__. This document details the architectural approach that will be taken
to support that protocol.

## Backward Compatibility and Migration Support

It is __NOT__ a goal of the EDC project to provide backward compatibility to the previous IDS implementation. At a declared milestone, once the Dataspace Protocol implementation has reached
a sufficient maturity level, the EDC will switch to the new protocol and remove the old protocol implementation. Users will be responsible for migrating existing EDC installations
as no automated migration facilities will be provided.

# Goals

The goals of the Dataspace Protocol support are:

1. To be compliant with the Dataspace Protocol Contract Negotiation Protocol. This will involve passing all mandatory tests of the to-be-released IDS Test Compatibility Kit (IDS-TCK).
2. To support a range of requirements involving externally assisted contract negotiation decisions. These include manual negotiation approval and integration with external systems
   to determine state transitions.
3. To support integration with facilities for producing digital contract signatures and contract non-repudiation.

# The State Machine

The IDS Specification Contract Negotiation states are:

- INITIALIZED,
- REQUESTED,
- OFFERED,
- ACCEPTED,
- AGREED,
- VERIFIED,
- FINALIZED,
- TERMINATED

The following table defines the mapping between existing EDC states, the corresponding new EDC states, and the IDS specification states.

| EDC Existing       | EDC New       | IDS        | Transition Function      | Notes                    |
|--------------------|---------------|------------|--------------------------|--------------------------|
| UNSAVED            | (remove)      | N/A        |                          | This state is not needed |
| INITIAL            | INITIAL       | N/A        |                          |                          |
|                    |               |            |                          |                          |
| REQUESTING         | REQUESTING    | N/A        |                          |                          |
| REQUESTED          | REQUESTED     | REQUESTED  | Provider (new & counter) |                          |
|                    |               |            |                          |                          |
| PROVIDER_OFFERING  | OFFERING      | N/A        |                          |                          |
| PROVIDER_OFFERED   | OFFERED       | OFFERED    | Consumer                 |                          |
| CONSUMER_OFFERING  | (REQUESTING)  |            |                          |                          |
| CONSUMER_OFFERED   | (REQUESTED)   |            |                          |                          |
|                    |               |            |                          |                          |
| CONSUMER_APPROVING | ACCEPTING     | N/A        |                          |                          |
| CONSUMER_APPROVED  | ACCEPTED      | ACCEPTED   | Provider                 |                          |
|                    |               |            |                          |                          |
| DECLINING          | (TERMINATING) |            |                          |                          |
| DECLINED           | (TERMINATED)  |            |                          |                          |
|                    |               |            |                          |                          |
| CONFIRMING         | AGREEING      | N/A        |                          |                          |
| CONFIRMED          | AGREED        | AGREED     | Consumer                 |                          |
|                    | VERIFYING     | N/A        |                          |                          |
|                    | VERIFIED      | VERIFIED   | Provider                 |                          |
|                    | FINALIZING    | N/A        |                          |                          |
|                    | FINALIZED     | FINALIZED  | Consumer                 |                          |
|                    | TERMINATING   | N/A        |                          |                          |
|                    | TERMINATED    | TERMINATED | P & C                    |                          |
|                    |               |            |                          |                          |
| ERROR              | (TERMINATED)  |            |                          |                          |

# State Transition Functions

State transition functions (`StateTransitionFunction`) can be registered at specific callback points which are responsible for transitioning the Contract Negotiation State Machine
(CNSM) to a new state. These functions can be used to implement custom workflows. In runtime configurations that support it, transition functions will be called transactionally as
part of the `ContractNegotiationManager` process loop. This will ensure state transitions are atomic.

`StateTransitionFunction` will operate on `ContractNegotiation` instances.

```java

@FunctionalInterface
public interface StateTransitionFunction {

    Result<Boolean> process(ContractNegotiation negotiation);

}
```

Note this does not follow immutability principles but has the advantage of simplicity and the ability to enforce proper state transitions in the `ContractNegotiation` instance.
The `Result<Boolean>` response indicates if the `ContractNegotiation` has been updated (true) or not (false), or if processing failed. This can be used to avoid
re-persisting `ContractNegotiation` instances if they have not changed.

End users will be able to register their own `StateTransitionFunction` implementations to provide custom contract negotiation workflows. For example, a `StateTransitionFunction`
implementation can be registered to be invoked when a consumer request is received (REQUESTED) to support manual approval. The function will be called by
the `ConsumerContractNegotiationManager` during its processing loop. If the request initiates a new negotiation, the function implementation could trigger a process in an external
approval system. In a subsequent processing loop, the `StateTransitionFunction` could check for the completion of the external approval process (e.g. by querying a column in a
table) and updating the `ContractNegotition`.

# Default StateTransitionFunctions

The following tabled define the default `StateTransitionFunction`s to use if one is not registered for the specified state transition.

## Consumer Default StateTransitionFunctions

| State     | Default StateTransitionFunction                                 |
|-----------|-----------------------------------------------------------------|
| OFFERED   | Transition to ACCEPTING if offer matches the original requested |
| AGREED    | Transition to VERIFYING                                         |
| FINALIZED | No-op                                                           |
|           |                                                                 |
|           |                                                                 |
|           |                                                                 |

## Provider Default StateTransitionFunctions

| State      | Default StateTransitionFunction                                                  |
|------------|----------------------------------------------------------------------------------|
| REQUESTED  | For the initial request transition to AGREED if the offer corresponds to a       |
|            | provider offer and the consumer evaluates successfully against the offer policy. |
|            |                                                                                  |
| ACCEPTED   | Transition to AGREEING                                                           |
| VERIFIED   | Transition to FINALIZING                                                         |
| TERMINATED | No-op                                                                            |

# HTTP Transition Functions (Phase II)

The EDC provides the ability to extend the runtime via HTTP endpoints, for example, HTTP provisioning. A similar method should be added for state transitions where an HTTP endpoint
can implement processing logic for one of the defined `StateTransitionFunction` extension points. In addition to invoking an external endpoint, the EDC runtime will need to provide
an HTTP REST controller to serve as a callback for receiving updated `ContractNegotiation` instances.

## Callback Endpoint Reliability

The controller may use the command infrastructure to update a received `ContractNegotiation`. However, since the command infrastructure relies on an in-memory queue, updates will
not be reliable. Three options for addressing this are:

1. Make the in-memory queue reliable and persistent. This option is likely to be the most complicated and difficult to achieve as it will require persistence and the ability to
   perform message ordering.
2. Introduce a persistence mechanism for storing intermediate state machine results including `ContractNegotiation` and `TransferProcess,` and updates them using the command
   infrastructure in a reliable way (e.g. removing entries once the command processing has been completed). This is still a complicated solution but has the advantage of
   using the command infrastructure as a single point for external state machine updates.
3. Introduce a lock-based synchronous update mechanism for state machine instances including `ContractNegotiation` and `TransferProcess` instances that bypasses the command queue.
   This approach has the advantage of relative simplicity but introduces an additional code path for external state machine updates.
 
A generic solution should be designed which can be used by other extensions such as the HTTP provisioner to provide reliability guarantees.  

> Note that this feature will also be required for supporting reliable callbacks when a transfer is complete and when a transfer has been suspended.


