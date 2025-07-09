# Embed `data-plane-selector` into `control-plane`

## Decision

`data-plane-selector` "select" functionality will only be available embedded in the `control-plane`.

## Rationale

At the moment we are maintaining the http communication layer for `data-plane` selection between the `control-plane` and
a potentially independently deployed `data-plane-selector`.
There's no actual demonstration of the usefulness of this separation and all the project we know are actually deploying
them together.
Nothing will stop us in the future to re-think the boundaries again, but at the moment this is a simplification that will
reduce maintenance burden.

## Approach

The implementation of the `select` method in the `RemoteDataPlaneSelectorService` will always return a failed result.
The `/select` endpoint in the `DataplaneSelectorControlApiController` will be removed.
In the `DataPlaneSelectorService` only the `select(@Nullable String selectionStrategy, Predicate<DataPlaneInstance> filter)`
method will remain, the other `select(DataAddress source, String transferType, @Nullable String selectionStrategy)` will
be removed as less flexible and all it's usages can be easily replaced with the first one.
