**Please note**

### Work in progress

All content reflects the current state of discussion, not final decisions.

---

# Eclipse Dataspace Connector

<p style='text-align: justify;'> EDC is an interoperable, cross-organization framework for data sharing between 2 or more Organizations in a trusted and
compliant way. This framework has focused to develop in 4 key technological key points for safer data sharing: Identity
of the user(s), Trust, Policies and Interoperability. The Eclipse Dataspace Connector framework provides different
modules for performing data queries, exchanges, policy enforcement, monitoring and auditing.

## Overview

<p style='text-align: justify;'> The connector is a framework for sovereign, inter-organizational data exchange and contains the technology component that allows each organization involved in the exchange to define how digital processes, infrastructures, and data flows are structured, built, and managed to ensure adherence to corporate policies and data sovereignty regulations<sup>1</sup></p>

A connector is composed of 2 subsystems:

| **Control Plane (First checking step)** | **Data Plane (Second checking step)** |
|:----------------------------------------|:--------------------------------------|
| User verification (access to data)      | Moves bits.                           |
| Contract negotiation.                   | Big Data Tasks.                       |
| Overseeing of Policy enforcement.       | Streaming.                            |
| Provisioning management.                | Events.                               |

<p style='text-align: justify;'> The connector processes requests asynchronously, which are useful for example in case one has to deal with lengthy data preparation</p>

## Configuration

TBD

## Installation

TBD

## Troubleshooting

TBD

## Architecture

[Architecture Principles](architecture-principles.md)

[Sequence diagrams](architecture/README.md)

[Domain Model](domain-model.md)

## Terminology

see [Terminology](Terminology.md)

## References

1. https://projects.eclipse.org/projects/technology.dsconnector/reviews/creation-review
