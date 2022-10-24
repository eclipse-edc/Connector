# Usage of Gradle Version Catalogs

## Decision

The EDC Build Plugin (currently under development) will provide
a [Gradle Version Catalog](https://docs.gradle.org/7.4/userguide/platforms.html)
that will contain versions of all the third-party libraries that are presently used in the EDC codebase.

## Rationale

The usage of version catalogs is expected to remove the possibility for version clashes between EDC and client projects
because it centralizes common definitions.
It will also make it transparent, which versions of which libraries EDC is using internally without having to look at
the source code or performing a dependency inspection on the build level.

There are in fact multiple scenarios where a benefit can be gained from version catalogs.

1. EDC itself will be able to get rid of declaring all dependencies and accessing them through build
   properties (`val something : String by project`), because the version catalog is typed and accessible at
   configuration time. It is also hierarchical, so it is easy to navigate and access.

2. Other EDC projects/components: here we'll mostly use the version catalog to enforce version consistency across
   multiple repos/projects. The version catalog is created centrally, and distributed through Gradle plugins. This also
   helps in keeping third-party libraries updated across multiple projects.

3. Third-party client projects: various industry initiative such as Catena-X would benefit from the version catalog, in
   that they do not have to look at EDC's source code in order to learn which version of which lib it uses, but can
   consult the version catalog distributed by the plugin. That will avoid version clashes with transitive dependencies,
   and the amount of introspection and intricate knowledge necessary to use EDC.

## Approach

The EDC Build Plugin (under development) will declare and distribute the version catalog as part of its public API. It
will contain all third-party libraries currently in use by EDC at the time of publication. For example a structure
similar to the following could emerge:

```kotlin
versionCatalogs {
    create("edcext") { //extensions is a reserved keyword
        library("azure-storage", "com.azure:azure-storage-blob:X.Y.Z")
        library("azure-cosmos", "com.azure:azure-cosmos:X.Y.Z")
        library("azure-resourcemanager", "com.azure:azure-resource-manager:X.Y.Z")
        library("azure-resourcemanager-auth", "com.azure:azure-resource-manager-authorization:X.Y.Z")
        // ...
    }
}
```

Version catalogs are lightweight, much more so than platforms (which actually influence the dependency graph), and they
should be understood as the EDC project team's recommendation. Clients can then use the platform feature to apply them
and restrict the dependency graph or simply choose to override them at their own risk.

### A word on naming

Version catalogs automatically convert separated aliases into hierarchical structures, so `azure-resourcemanager-auth`
would be converted into `azure.resourcemanager.auth`. As a general rule of thumb those aliases should include the
project name and the module that is being imported, for example
Jackson: `com.fasterxml.jackson.core:jackson-annotations:X.Y.Z`:

- **bad**: `jackson.core.annotations`: the `core` is not needed, as it's part of the group id and does not offer
  additional
  insights
- **bad**: `fasterxml-jackson.annotations`: should avoid long project names, people would likely expect it to
  be `jackson` rather than `fasterxml-jackson`
- **better** `jackson.annotations`, `jackson.core`, `jackson.databind`, etc.

### Guidelines when to create new entries in the catalog

As a general rule of thumb a library should be included in the dependency version catalog when:

- it is used in multiple modules in EDC
- it is a technology dependency, such as Azure Blob Storage or Google Cloud Storage
- it is an essential dependency, such as AssertJ, Mockito, etc.
- there are known conflicts, vulnerabilities or inconsistencies, even between minor versions. Crypto-libraries sometimes
  are affected by this.

## Nota Bene

- Version catalogs will be implemented in the EDC Build Plugin first, and will be adopted in EDC at a later point in
  time
- Version catalogs are still an incubating feature