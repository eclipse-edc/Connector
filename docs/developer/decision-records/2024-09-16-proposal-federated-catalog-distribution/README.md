# Proposal for FederatedCatalog with Tractus-X distribution

## Decision

The **proposed solution is to have a FederatedCatalog as own standalone service**.
Choosing a solution that decouples from Control Plane (like the one used for the Data Plane) and also able to be scalable, we believe future proofs the Federated Catalog as a feature and embraces wider usage. Like so, having a FederatedCatalog service, scalable on its own based on user needs and not on Control Plane allows to best resource management in the long term and allows to add features to the Federated Catalog without the need to change the Control Plane logic (assuming no API usage change).
The Network Latency expected may be a concern, but not more than any other downstream dependencies of the service.

## Rationale

Currently two main options are being considered regarding the distribution of Tractus-X (TX) with the FederatedCatalog (FC):
- FederatedCatalog **running as a standalone service** with its own API providing the catalogs' data it contains upon request, similar to what happens currently with the DataPlane (DP).
- FederatedCatalog as **a ControlPlane (CP) extension**, i.e. embedded and shipped as any other extension.

Taking under consideration both options mentioned above, the following topics were analyzed.

To reduce verbosity, the next acronyms are used
**FCSS**: Federated Catalog as a Standalone Service
**FCE**:  Federated Catalog as a Control Plane Extension


### Coupling
Aiming at decoupling the FCSS is a better option, since changes on Control Plane do not demand action on FC and vice versa. Additionally, with this solution, **independent upgrades can be assured**, unless they include API changes.

### Scalability
FCSS can be better scalable since has no other service dependencies. If several instances of Control Plane are needed, there would be a need to either disable the extension on some tasks or accept several FederatedCatalog extentions, either being a scalability concern and showing limitations of FCE. So, if each instance of the Control Plane boots up a FederatedCatalog extension that can be an overkill and have undesired overhead.

### Network Latency
Since FCE does not require HTTP requests (being the FederatedCatalog embedded part of the client) the network latency expected is lower, even if FCSS is deployed in a participant own private network. The impact should be minimal, but FCSS would have more latency.

### Resources
FCSS and FCE would both have a store, which would represent the majority of computing resources (directly related with storage cost) so that is the most relevant consideration either way.
It also is important to highlight that FCE may require less computing resources usage since FederatedCatalog does not have own instance.



## Approach

Running the FederatedCatalog own application should be straightforward. If need to test locally, the [Readme.md](https://github.com/eclipse-edc/FederatedCatalog/blob/main/README.md) includes a very helpful guide. In terms of infrastructure applicability, similar approach to Control Plane or Data Plane can be used.