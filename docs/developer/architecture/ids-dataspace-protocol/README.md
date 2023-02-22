# Dataspace Protocol Architecture

The EDC will be upgraded to implement and be fully compliant to the __Dataspace Protocol__ protocol specifications. These include:

- Catalog and Catalog Binding HTTP
- Contract Negotiation and Contract Negotiation Binding HTTP
- Transfer Process and Transfer Process Binding HTTP

This document outlines the approach that will be taken to support those protocols. Other documents will detail how the specifications will impact specific subsystems.

## Backward Compatibility and Migration Support

It is __NOT__ a goal of the EDC project to provide backward compatibility to the previous IDS implementations. At a declared milestone, once the Dataspace Protocol implementation has reached
a sufficient maturity level, the EDC project will switch to the new protocol and remove the old protocol implementation. Users will be responsible for migrating existing EDC
installations as no automated migration facilities will be provided.

> EDC will not offer runtime migration, i.e. conversion of state machines that are in live EDC instances. This will be the responsibility of other parties to either
> provide this level of support or deal with migration in another manner.

# Goals

The goals of the Dataspace Protocol support are:

1. To be compliant with the Dataspace Protocol Specifications listed above. This will involve passing all mandatory tests of the to-be-released IDS Test Compatibility Kit (IDS-TCK).
2. Based on specification compliance, the EDC aims to be interoperable with other IDS implementations that are also the Dataspace Protocol specification compliant.
3. The EDC __will not__ support any IDS versions prior to the Dataspace Protocol Specifications.
4. Following the approach taken with Dataspace Protocol, the core EDC project __will not__ implement specific usage policies. It is expected that usage policy implementations will be
   provided by other projects and dataspaces.

# Impact and Approach

The Dataspace Protocol is built on [JSON-LD](https://www.w3.org/TR/json-ld11/), [DCAT](https://www.w3.org/TR/vocab-dcat-3/) and [ODRL](https://w3c.github.io/poe/model/). The core EDC
architecture will remain largely unchanged. The most significant change will be the modification and addition of contract negotiation and transfer process states. As development on
the Dataspace Protocol proceeds, we will attempt to retrofit state machine changes to the existing IDS extensions. The goal here is not to offer parallel support for the old protocol but to
ensure that existing tests continue to function during development until the changeover to the Dataspace Protocol is made.

Support for the Dataspace Protocol will be done in the following steps:

1. Core JSON-LD support will be added as detailed in the [JSON-LD Processing Architecture](./json-ld-processing-architecture.md).
2. The Dataspace Protocol endpoint and service extensions will be added in parallel to the existing IDS extensions. This work is described in
   the [IDS Endpoints and Services Architecture](./ids-endpoints-services-architecture.md).
3. The contract negotiation manager state machine will be updated to accommodate the Dataspace Protocol protocols as described in
   the [Contract Negotiation Architecture](./contract-negotiation-architecture.md). If possible, the new state transitions should be retrofitted back to the existing IDS
   extensions. Since the existing contract managers do not implement all state transitions, retrofitting the new states back to the existing IDS protocol should be possible.
4. The transfer process state machine will be updated to accommodate the Dataspace Protocol protocols as described in [the Transfer Process Architecture](./transfer-process-architecture.md).
   Since all new states are nearly 1:1 with existing EDC states, retrofitting to the old IDS protocol should be possible.
5. When 1-4 are stabilized, the old IDS modules and supporting services will be removed.
6. An ensuing release will update the Management API

