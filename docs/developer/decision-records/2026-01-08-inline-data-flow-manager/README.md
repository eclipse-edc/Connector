# Inline DataFlowManager

## Decision

`DataFlowManager` will be removed and there will be a single instance of the `DataFlowController` that could be used by
the runtime.

## Rationale

When it was implemented there was this will to have the possibility to run multiple `DataFlowController`s in the same runtime.
This feature has never really been used, and the selection mechanism is definitely not necessary, especially now with the
upcoming DataPlane Signaling spec: we can foresee that there will be a single DataPlane protocol used, eventually with 
multiple versions, but everything managed by the same flow controller.

## Approach

`DataFlowManager` will be removed, and `DataFlowController` will become an injectable mandatory service.
