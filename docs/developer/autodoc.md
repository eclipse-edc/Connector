# The `autodoc` Gradle plugin

Please find the comprehensive documentation about the `autodoc` plugin in
the [Github Repo](https://github.com/eclipse-edc/GradlePlugins/blob/main/docs/developer/autodoc.md) of
the plugin.

In EDC, the plugin is intended to be used to generate metamodel manifests for every Gradle module, which then
transformed into Markdown files, subsequently rendered for publication in static web content.

## Publishing the manifest files

For every subproject that generates an `edc.json` file a Maven publication is created in the root build file, so that
the manifest file gets published alongside the binary jar files, sources jar and javadoc jar.

## Downloading the manifest files

For publishing we use `type=json` and `classifier=manifest`, which means a dependency in a client project would look
like
this (kotlin DSL):

```kotlin
implementation("org.eclipse.edc:<ARTIFACT>:<VERSION>:manifest@json")
```

For example, for the `:core:control-plane:control-plane-core` module in version `0.1.4-SNAPSHOT-SNAPSHOT`, this would be:

```kotlin
implementation("org.eclipse.edc:control-plane-core:0.1.4-SNAPSHOT-SNAPSHOT:manifest@json")
```

When the dependency gets resolved, the manifest file will get downloaded to the local gradle cache, typically located
at `.gradle/caches/modules-2/files-2.1`. So in the example the manifest would get downloaded
at `~/.gradle/caches/modules-2/files-2.1/org.eclipse.edc/control-plane-core/0.1.4-SNAPSHOT-SNAPSHOT/<HASH>/control-plane-core-0.1.4-SNAPSHOT-SNAPSHOT-manifest.json`
