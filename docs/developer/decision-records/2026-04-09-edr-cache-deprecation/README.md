# EDR Cache Deprecation

## Decision

We will deprecate the EDR cache management and remove the current v4 beta API for EDRs.

## Rationale

The EDR cache management was introduced as a helper for users in order to manage DataAddress (EDR) received from the
provider during the transfer process. However, this mechanism with the advent
of [DPS](https://github.com/eclipse-dataplane-signaling/dataplane-signaling/) (Data Plane Signaling protocol)
will be shifted to the data plane, which will be responsible for managing the EDRs and their lifecycle.

## Approach

We will deprecate all EDR cache management related coded and in API v4 currently in beta we will remove the
EDR cache management endpoints.

The API v3 will continue to support EDR cache management until the end of its lifecycle, but it will be marked as
deprecated once v4 is stable and users will be encouraged to migrate to API v4 and the new data plane signaling
mechanism for EDR management.
