# EDC Naming Conventions

## Decision

The naming of existing and future Java packages, Gradle modules, Maven artifacts, and configuration
properties should follow defined naming conventions that align the project's structure. Changes in the
connector repository will affect downstream repositories, in addition, conventions should also be implemented
there.

Related decision records:
- [2022-08-09 Project structure review](../2022-08-09-project-structure-review/)
- [2022-08-11 Versioning and Artifacts](../2022-08-11-versioning_and_artifacts/)

## Rationale

Since the developed framework components of the EDC project are designed to be adopted in other projects,
their usages must be as simple and developer-friendly as possible. This is supported, for instance,
by a consistent naming following clearly defined conventions and established standards.

Our goals by introducing naming conventions are:
- Easy navigation of Gradle modules, Maven artifacts and imported Java packages.
- Unique naming of config properties (especially with regard to custom properties introduced by extensions).
- Accordance to the Eclipse Foundation's guidelines and release processes.
- Elimination of split packages.

## Approach

Existing standards that are considered in the following are listed below.
- Apache Maven's [guide to naming conventions on groupId, artifactId, and version](https://maven.apache.org/guides/mini/guide-naming-conventions.html)
- Oracle's [Java package naming conventions](https://docs.oracle.com/javase/tutorial/java/package/namingpkgs.html)

### Naming Conventions

#### Gradle Module

All core components of the EDC project are implemented as Gradle multi-module projects. A Gradle module
is referenced in the `settings.gradle`, e.g.:

```kotlin
include(":core:data-plane:data-plane-framework")
```

In this example, the _moduleId_ is `data-plane-framework`. This module implements the "data plane framework"
that is part of the "data plane" without naming the `core` module. With this, the _moduleId_ is a unique
concatenation of its location, without preventing the moving and a restructuring of the package.


#### Maven Artifact

A Maven artifact is composed of the following components:
```
<groupId>.<artifactId>.<version>
```

The _groupId_ uniquely identifies our project: `org.eclipse.edc.*`. All EDC core components (as of 10.10.2022:
[connector](https://github.com/eclipse-dataspaceconnector/DataSpaceConnector),
[identity hub](https://github.com/eclipse-dataspaceconnector/IdentityHub),
[registration service](https://github.com/eclipse-dataspaceconnector/RegistrationService),
and [federated catalog](https://github.com/eclipse-dataspaceconnector/FederatedCatalog))
are published with the same version under this same _groupId_.

The _artifactId_ is the name of the jar without a version. In the EDC project, the _artifactId_ is identical
to the _moduleId_. For example:

```kotlin
publishing {
    publications {
        create<MavenPublication>("data-plane-framework") {
            artifactId = "data-plane-framework"
            from(components["java"])
        }
    }
}
```

For versioning, see [here](../2022-07-06-release-automation/).

#### Java Package

Following Oracle's [Java package naming conventions](https://docs.oracle.com/javase/tutorial/java/package/namingpkgs.html),
package names are written in all lower case. In addition, package names use the reversed domain name,
for example, `com.example.mypackage` for a package named `mypackage` created by a programmer at `example.com`.

Accordingly, in the EDC project, aligned with the _groupId_, Java packages start with `org.eclipse.edc.*`.
Corresponding to the _moduleId_, this is followed by a unique path. To remain with our example, the
class `DataPlaneFrameworkExtension` is located at:

```java
package org.eclipse.dataspaceconnector.dataplane.framework;
```

In this context, everything after `org.eclipse.edc` is sorted first by field/discipline and then by scope.
For example, a policy spi will not be located at `org.eclipse.edc.spi.policy` but at `org.eclipse.edc.policy.spi`.

#### Configuration Property

Following the [policy scopes](../2022-03-15-policy-scopes/), configuration properties are using a hierarchical
dot notation. They do **not** start with an `edc` or similar Eclipse related prefix.

Configuration properties should fit to related _moduleIds_ or package names and be grouped by scope.

### Implementation

Renaming according to the defined [naming conventions](#naming-conventions).
- Check if _moduleId_ and _artifactId_ are unique and represent a concatenation in the correct order.
- Modify release process to use `org.eclipse.edc` as _groupId_.
- Check every Java package and move classes if necessary.
- Detect and resolve split packages.
- Remove `edc` prefix from any configuration property.
- Align existing configuration properties in the EDC project. Add a clear warning to extensions that
  fail to load a (new) value.