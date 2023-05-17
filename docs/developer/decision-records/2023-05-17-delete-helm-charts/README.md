# Delete Helm Chart

## Decision

The Helm Chart located in the `resources` folder will be removed.

## Rationale

Helm Charts are artifacts that are specific to the EDC customization, so we expect every Dataspace to provide their own.
e.g. [Tractus-X Helm Charts](https://eclipse-tractusx.github.io/charts/)

## Approach

Delete the `resources/charts` folder and the `@MiniKubeTest` as well.
