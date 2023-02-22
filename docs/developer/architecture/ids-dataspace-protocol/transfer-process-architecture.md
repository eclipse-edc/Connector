# The Dataspace Protocol Transfer Process Architecture

The EDC will be upgraded to implement and be fully compliant to the __Dataspace Protocol Transfer Process Protocol__. This document details the architectural approach that will be taken
to support that protocol.

## Backward Compatibility and Migration Support

It is __NOT__ a goal of the EDC project to provide backward compatibility to the previous IDS implementation. At a declared milestone, once the Dataspace Protocol implementation has reached
a sufficient maturity level, the EDC project will switch to the new protocols and remove the old protocol implementation. Users will be responsible for migrating existing EDC
installations as no automated migration facilities will be provided.

## Goals

The goals of the Dataspace Protocol support are:

1. To be compliant with the Dataspace Protocol Transfer Process Protocol. This will involve passing all mandatory tests of the to-be-released IDS Test Compatibility Kit (IDS-TCK).

## The State Machine

The IDS Specification Contract Negotiation states are:

- REQUESTED
- STARTED
- COMPLETED
- SUSPENDED
- TERMINATED

The following table defines the mapping between existing EDC states, the corresponding new EDC states, and the IDS specification states.

| EDC Existing             | EDC New                  | IDS        | Notes                    |
|--------------------------|:-------------------------|------------|--------------------------|
| UNSAVED                  | (remove)                 | N/A        | This state is not needed |
| INITIAL                  | INITIAL                  | N/A        |                          |
|                          |                          |            |                          |
| PROVISIONING             | PROVISIONING             | N/A        |                          |
| PROVISIONING_REQUESTED   | PROVISIONING_REQUESTED   | N/A        |                          |
| PROVISIONED              | PROVISIONED              | N/A        |                          |
|                          |                          |            |                          |
| REQUESTING               | REQUESTING               | N/A        |                          |
| REQUESTED                | REQUESTED                | REQUESTED  |                          |
|                          |                          |            |                          |
| IN_PROGRESS              | STARTED                  | STARTED    |                          |
| COMPLETED                | COMPLETED                | COMPLETED  |                          |
|                          |                          |            |                          |
| (multiple)               | TERMINATED               | TERMINATED |                          |
|                          |                          |            |                          |
| DEPROVISIONING           | DEPROVISIONING           | N/A        |                          |
| DEPROVISIONING_REQUESTED | DEPROVISIONING_REQUESTED | N/A        |                          |
| DEPROVISIONED            | DEPROVISIONED            | N/A        |                          |
|                          |                          |            |                          |
| STREAMING                | (STARTED)                |            |                          |
| ENDED                    | (TERMINATED)             |            |                          |
| CANCELLED                | (TERMINATED)             |            |                          |
| ERROR                    | (TERMINATED)             |            |                          |
|                          |                          |            |                          |

