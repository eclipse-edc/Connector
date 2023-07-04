# The EDC version catalog

EDC provides a [Version Catalog](https://docs.gradle.org/7.4/userguide/platforms.html) which contains all the
third-party dependencies
that are currently in use by EDC.

This version catalog should be regarded as the recommended and tested dependency matrix, but it is not mandatory nor
does it enforce the use of a particular dependency. We only use "required dependencies", i.e. no minimum or maximum
versions, no ranges, no rejected versions etc.

## Using the version catalog

The version catalog gets distributed as regular Maven Artifact using the following coordinates:

```
org.eclipse.edc:edc-versions:<VERSION>
```

As per the [documentation](https://docs.gradle.org/7.4/userguide/platforms.html#sec:importing-published-catalog) the
version catalog and the repositories, in which to look for it, must be declared in the `settings.gradle.kts`:

```kotlin
// in settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        maven {
            url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
        }
        mavenCentral()
        mavenLocal()
    }
    versionCatalogs {
        create("libs") {
            from("org.eclipse.edc:edc-versions:<VERSION>>")
        }
    }
}
```

Then, the version catalog named `"libs"` is available in the project, its exact contents can be
inspected [in this `*.toml` file](https://github.com/eclipse-edc/GradlePlugins/blob/main/gradle/libs.versions.toml)
. Be aware that the library aliases are _normalized_, that means all the dashes, underscores and dots are interpreted as
separators.

Utilizing a dependency is easy, simply refer to it in the dependency configuration:

```kotlin
implementation(libs.jackson.annotation) // resolves to "com.fasterxml.jackson.core:jackson-annotations"
```

### Using bundles

In the context of version catalogs a `bundle` is a set of versions grouped together, similar to a Maven BOM. Using them
in build files is just as easy as with "normal" dependencies:

```kotlin
implementation(libs.bundles.jersey.core)

testFixturesImplementation(libs.bundles.jupiter)   
```

## Modifying the version catalog

There are two main scenarios where you might want to change the provided version catalog:

- adding new libraries
- upgrading a specific version

_It must be made clear that changing the provided version catalog should only be done deliberately and with a very
specific purpose in mind. Expect to be challenged during code reviews if you choose to do it!_

### Extending the version catalog

Adopting a new version into the EDC Version Catalog will take some time: a feature request issue must be opened, that
issue needs to be processed, a PR must be opened, a review must be performed, and a new artifact version of the Version
Catalog artifact must be built, etc. While that is the recommended process, we understand that it is not always possible
to wait for that.

For example if you have a time-sensitive PR open in EDC for which you need a third-party library that
will be used in multiple packages (NimbusDS would be a good example). Then it might be a good idea to extend the EDC
Version Catalog temporarily, until that lib can be adopted into the EDC Version Catalog.

Another situation would be a third-party library, that will only be used in a very limited scope, for example an SPI
package and the corresponding implementation package. We would not necessarily need to adopt such a lib into the EDC
Version Catalog, but it is still a good idea to harmonize version management inside the EDC project.

```kotlin
dependencyResolutionManagement {
    // not shown: repositories
    versionCatalogs {
        create("libs") {
            from("org.eclipse.edc:edc-versions:0.1.4-SNAPSHOT-SNAPSHOT")
            // this is not part of the published EDC Version Catalog, so we'll just "amend" it. 
            // the versionRef "okhttp" is already defined in the Version Catalog
            library("dnsOverHttps", "com.squareup.okhttp3", "okhttp-dnsoverhttps").versionRef("okhttp")
        }
    }
}
```

### Overriding the version catalog

Due to the reasons mentioned before, it is sometimes quicker to override a specific version directly in the client
project. This can come in handy when there are breaking changes in the lib's API, or there are known critical
vulnerabilities, or you simply need a new and shiny feature. Then (temporarily) overwriting a library's version could be
an option:

```kotlin
dependencyResolutionManagement {
    // not shown: repositories
    versionCatalogs {
        create("libs") {
            from("org.eclipse.edc:edc-versions:0.1.4-SNAPSHOT-SNAPSHOT")
            // override the version for Jackson. Must use existing alias
            version("jackson", "2.69.0")
        }
    }
}
```

Note that the version, that actually gets used during runtime, may still be different due to conflict resolution etc.
See also
the [official documentation](https://docs.gradle.org/7.4/userguide/platforms.html#sec:overwriting-catalog-versions)
