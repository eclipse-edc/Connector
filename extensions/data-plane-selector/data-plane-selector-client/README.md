# DataPlane Selector Client

this module contains implementations for running a DPF Selector embedded in the Control Plane, or as remote instance,
accessing it's REST API.

There are two implementations at the moment:
1. `EmbeddedDataPlaneSelectorClient`: when the DPF selector runs embedded in the control plane
2. `RemoteDataPlaneSelectorClient`: when the DPF selector runs as stand-alone process and is accessible via REST API. 
   Requires URL to the DPF selector as constructor param.  