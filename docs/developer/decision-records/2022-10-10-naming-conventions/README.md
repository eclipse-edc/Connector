# EDC Naming Conventions

## Decision

The naming of existing and future Java packages, Gradle modules, Maven artifacts, and configuration
properties should follow defined naming conventions that align the project's structure. 

Related decision records:
- [2022-08-09 Project structure review](../2022-08-09-project-structure-review/)
- [2022-08-11 Versioning and Artifacts](../2022-08-11-versioning_and_artifacts/)

## Rationale

A software project's structure should be designed as developer-friendly as possible, by following precisely
defined rules based on established standards.

Our goals by introducing naming conventions are:
- Easy navigation of either Gradle modules, Maven artifacts or the Java package structure.
- Unique naming of config properties (especially with regard to custom properties introduced by extensions).
- Accordance to the Eclipse Foundation's guidelines and release processes.
- Elimination of split packages.

## Approach

### Gradle module name

Gradle modules must have unique names across the entire build classpath, even if they are located in
different module paths. This is because of a bug in Gradle itself where Gradle will erroneously report
a "cyclic dependency" if this rule is violated. The following hypothetical example would constitute
such a violation:

```kotlin
// settings.gradle.kts
include(":core:common:transfer-process")
include(":extensions:sql:transfer-process")
```

The EDC project has checks in place to make sure module IDs are unique.

> Rule 1: Modules must have unique names.

In addition, the _module name_ should give a hint what is in the module, without being too verbose. The
earlier example would be a bad one, because "transfer-process" does not indicate what the contents could
be. This is especially important because Maven's _artifactId_ must be equal to module names.

Here are some bad examples:
- `:core:common:transfer-process:validation`: bad because "validation" is likely to be not unique and isolated it only indicates that it has to do with validation, but not in _what context_.
- `:core:dataplane:framework`: again, "framework" is liable to cause conflicts, and in addition, it's a very generic, unspecific term

Refactoring these bad examples, we could arrive at these:
- `:core:common:transfer-process:transfer-process-validation`: could contain validation functions
- `:core:dataplane:dataplane-framework`: would contain default impls and platform code for the dataplane

> Rule 2: Module names should indicate what the contents are.

> Rule 3: Module names must be identical to the Maven artifactId (published one).

### Maven artifactId

The EDC project uses the same `groupId = "org.eclipse.edc"` across all sub-projects, which means all
_artifactIds_ must be unique across multiple software components to avoid conflicts.

> Rule 4: A Maven artifactId must be unique within the groupId.

> Rule 5: A Maven artifactId must be identical to the module name (cf. [Rule 3](#gradle-module-name)).

### Java package name

Following Oracle's [Java package naming conventions](https://docs.oracle.com/javase/tutorial/java/package/namingpkgs.html),
EDC's base package name is `org.eclipse.edc`, followed by domain and then function. For example, the
Policy SPI should be located at `org.eclipse.edc.policy.spi` rather than `org.eclipse.edc.spi.policy`.
Here, `policy` is the "domain", i.e. the thematic context, and `spi` would be the "function", i.e. what
kind of package it is or: what purpose it serves.

The module name could be a helpful reference for the package name, replacing dashes with dots.

Other positive examples would be:
- `org.eclipse.edc.transferprocess.validation`: domain is `transferprocess`, and it's a package that deals with validation
- `org.eclipse.edc.dataplane.framework.manager`: here the domain is `dataplane.framework`, so all code should be beneath that directory.

This helps to avoid split packages.

> Rule 6: Package names should first contain the domain, and then the function.

### Configuration properties

Configuration properties should have a unique prefix `edc` to avoid clashes, when EDC gets embedded
in other Java frameworks such as Spring. Further, the config property should contain the component for
which it is valid, and a section in hierarchical structure (dot notation) indicating what the value is about.

Bad:
- `edc.retry.backoff.min`: does not contain the component, i.e. _which_ retry backoff min value is configured
- `edc.retry.backoff`: does not contain the component nor does it indicate, _which_ value is configured, i.e. what data type is expected
- `edc.core.retryBackoffMin`: is not hierarchically structured
- `edc.core.system.threadpool-size`: missing part of the component and is therefore misleading, because it does not indicate _what_ threadpool we're configuring
- `edc.dataplane.wait`: does not indicate which value is configured
- `web.http.port`: not prefixed with `edc`, can lead to conflicts

Better:
- `edc.core.retry.backoff.min`
- `edc.core.system.health.check.threadpool-size`
- `edc.dataplane.queue.poll-timeout`
- `edc.web.http.port`

> Rule 7: Configuration properties are prefixed with `edc.`.

> Rule 8: Configuration properties must contain the component in dotted notation to which they belong.

> Rule 9: Configuration properties must indicate the value and datatype that they configure.


## Implementation

Renaming according to the defined [rules](#approach).
- Check if _module name_ and _artifactId_ are unique and represent a concatenation in the correct order.
- Modify release process to use `org.eclipse.edc` as _groupId_.
- Check every Java package and move classes if necessary.
- Detect and resolve split packages.
- Align existing configuration properties in the EDC project. Add a clear warning to extensions that fail to load a (new) value.

Changes in the connector repository will affect downstream repositories, in addition, conventions should
also be implemented there.