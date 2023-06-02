# Splitting up `GradlePlugins` repo

## Decision

The [GradlePlugins repository](https://github.com/eclipse-edc/GradlePlugins) currently contains the `runtime-metamodel`
module, which will be moved out into a separate GitHub repository named `Runtime-Metamodel`.

## Rationale

This endeavour is meant to rectify the dependency graph, which currently contains a cyclic dependency between at least
one of our Gradle plugins (`edc-build`) and the `runtime-metamodel` module.

Moving out the `runtime-metamodel` module into a separate repository will not only break that cycle, it will also keep
the `GradlePlugins` repo focused on what it was intended to contain: Gradle plugins.
That in turn could come in handy, once the build process for plugins starts to deviate from our other components, i.e.
publishing to the Gradle Portal instead of MavenCentral.

## Approach

- Open an EF HelpDesk ticket, requesting that additional repo
- Move out the code
- Update the release pipeline
- Have all plugins use the `runtime-metamodel` artefact from Sonatype instead of a direct project dependency

It must be noted that once this is done, the `runtime-metamodel` cannot leverage the `edc-build` plugin anymore, as that
would
obviously re-introduce the cycle. Rather, the relevant parts of the build configuration will be replicated there, most
notably the publishing and signing configuration.

## Further considerations

Although it would probably be possible for the `runtime-metamodel` to use an older already published version of the
build
plugin, assuming the relevant parts of the build plugins publish configuration don't change very often, it seems to be a
rather unelegant solution, because that would add a lot of unused dependencies and other spurious configuration to
the `runtime-metamodel`. Further, it could theoretically introduce subtle bugs that stem from a version discrepancy of
any one of the plugins.