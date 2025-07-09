# Embed `data-plane-selector` into `control-plane`

## Decision

`data-plane-selector` will embedded in a `control-plane` without the possibility to deploy it separately.

## Rationale

At the moment we are maintaining the http communication layer between the `control-plane` and a potentially independently
deployed `data-plane-selector`.
There's no actual demonstration of the usefulness of this separation and all the project we know are actually deploying
them together.
Nothing will stop us in the future to re-think the boundaries again and come up with another separation or the same one.

## Approach

These classes will be removed:
- `RemoteDataPlaneSelectorService`
- `DataplaneSelectorControlApiController`
- other related classes.
