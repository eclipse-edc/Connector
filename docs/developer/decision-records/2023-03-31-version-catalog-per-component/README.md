# Version Catalog per Component

## Decision

Every component will have its own version catalog

## Rationale

At the moment we have a single catalog located in the `GradlePlugins` repository and used by every component. This is not
sustainable anymore because a version update in that repository could cause breaking changes in the other ones that will
pop up only in the nightly build. Plus, the `Release` repository has to define these catalogs manually because they aren't
published.

## Approach

We should keep a "base" `edc-versions` catalog in the `GradlePlugins` containing only the general purpose dependencies like
`runtime-metamodel`, `jackson`, `junit` ... in fewer words, the one that are automatically injected into every module by
the `DefaultDependencyConvention` in the `edc-build` plugin.

This `edc-versions` catalog could also be injected automatically by the plugin.

Then every component will have its own version catalog containing the version of the main `edc-versions` catalog plus only
the versions that it's actually using, defined in the `gradle/lib.versions.toml`.
This catalog will be published to maven with the artifactId in the format of `<component name>-versions`, like:
- `connector-versions`
- `identity-hub-versions`
- `registration-service-versions`
- `federated-catalog-versions`
  these three will be defined once we migrate the cloud service dependant dependencies to the respective repositories
- `technology-aws-versions`
- `technology-azure-versions`
- `technology-gcp-versions`

