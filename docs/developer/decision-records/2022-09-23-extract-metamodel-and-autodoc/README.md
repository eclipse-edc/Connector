# Automated Documentation Tooling

## Decision

The tooling code for automatic documentation generation ("autodoc",
cf. [this decision record](../2022-08-04-documentation-automation/README.md))
as well as the model code (i.e. the runtime metamodel) will be moved out into
a [new repository](https://github.com/paullatzelsperger/GradlePlugins).

## Rationale

1) `autodoc`
   This feature will be made available through a Gradle plugin, which needs to be available at build time of the EDC
   code. Gradle plugins are published using the [Gradle Plugin Portal](https://plugins.gradle.org/). Therefor the plugin
   needs to be built and published _before_ building/publishing EDC.

2) `runtime-metamodel`
   The `autodoc` feature has compile-time dependencies onto some metamodel annotations, such as `@Inject`, `@EdcSetting`
   , `@Extension` etc., which are also referenced by EDC. Thus, to break a cyclic dependency, the `runtime-metamodel` is
   moved out into the plugin repo as well.

## Approach

- Both these aforementioned modules will be moved out into
  a [new repository](https://github.com/paullatzelsperger/GradlePlugins).
- the `runtime-metamodel` is built and published to MavenCentral at the following:
  coordinates `org.eclipse.dataspaceconnector:runtime-metamodel:<VERSION>`
- the `autodoc` plugin has its own release cycle and versioning scheme, it will be published with the ID `autodoc`
- EDC will henceforth reference the `runtime-metamodel` artifact as compile-time dependency

## Caveats

- there is currently a pending discussion regarding rebranding of EDC, group-ids and the release process etc. Until that
  discussion is finalized, the `runtime-metamodel` will temporarily be available as version `0.0.1-SNAPSHOT`.
- The Gradle plugin does not support SNAPSHOT versions, it will be made available as version `0.0.1`. Every version
  update will follow the SemVer paradigm.