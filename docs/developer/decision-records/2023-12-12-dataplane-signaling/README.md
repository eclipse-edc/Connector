
# Decision

EDC will implement the Data Plane Signaling Protocol as described [here](). The Signaling Protocol defines how a control plane starts, suspends, and terminates a data flow in a data plane.

# Rationale

The EDC control plane currently implements an unspecified communication protocol when initiating flows in its proprietary data plane implementation, the DPF. In addition to being proprietary and undocumented, the communication has significant limitations. these include the inability to suspend flows and lack of adequate access token management.  

The Data Plane Signaling Protocol will be the foundation for the EDC control plane to interoperate with third-party data planes. It defines standardized messages and access control behavior that allow third-party data planes to work out-of-the-box with EDC-based control planes.

# Impact

The architecture and detailed refactoring requirements are defined in the Data Plane Signaling and Access Control Architecture referenced above.

The Data Plane Signaling Protocol will require refactoring the `DataFlowController` interface in the control plane. It will move `DataAddress` creation and access token generation to the data plane.

The Data Plane Framework will be refactored to persistently manage both push and pull `DataFlow` instances.
