# Automated Documentation Tooling

## Decision

Creating documentation and managing extensions and their configuration properties in EDC is
currently a time-consuming, manual process. A newly introduced module processor (described in the
following) ensures that the documentation is automatically collected and provided in a
human-readable form (as markdown files).

## Rationale

EDC runtimes are composed of __SPI__ and __extension__ modules that contribute capabilities to a
core subsystem. This modularity architecture is built on Java Service Provider loading. SPI modules
define extensibility points via service interfaces, while extension modules register service
implementations.

Extension modules provide metadata that assists the EDC core subsystem to bootstrap a runtime
correctly. The `@Inject` annotation is used to declare required services and obtain a reference to
those services. The `@Provides` annotation denotes which services an extension registers. A
dependency DAG is created from this metadata, and a reverse topological sort is performed to
determine the order extensions must be loaded.

This modularity architecture is simple but allows for a great deal of flexibility. For example,
extensibility layers are created by adding additional SPI modules, and multiple runtimes can be
assembled by including different module sets. However, as the number of extensions increases, the
EDC will need additional metadata and automation to maintain ease of use. The following approach
outlines tooling infrastructure that serves as a foundation for generating module manifests used to
create documentation, facilitate extension discovery, and assist in runtime assembly.

## Approach

### The Module Manifest

The EDC classifies modules as an SPI or an Extension. An SPI module defines one or more __extension
points__. By convention, an extension point is a Java interface, which is referred to as a service.
An extension module __provides__ 0..N extension point implementations. An extension module may
depend on (**reference**) 0..N services provided by other extensions. These relationships between
modules form a DAG.

The module manifest encapsulates this and other key metadata, including configuration settings and a
Markdown-based description. It has a canonical JSON serialization:

```json
[
  {
    "id": "org.eclipse.dataspaceconnector:base",
    "version": "0.0.1-SNAPSHOT",
    "name": "Core Services",
    "type": "extension",
    "categories": [],
    "extensionPoints": [],
    "provides": [
      {
        "service": "org.eclipse.dataspaceconnector.spi.system.health.HealthCheckService"
      }
    ],
    "references": [
      {
        "service": "org.eclipse.dataspaceconnector.spi.system.ExecutorInstrumentation",
        "required": false
      }
    ],
    "configuration": [
      {
        "key": "edc.core.retry.retries.max",
        "required": false,
        "type": "string",
        "description": "Specifies the maximum number of retries"
      }
    ],
    "overview": "…."
  }
]

```   

Serializing manifests in an array allows them to be concatenated for convenience.

As explained below, manifests can be resolved across artifact repositories and used to define the
module DAG for a runtime. This serves as the foundation for future EDC tooling.

### Module Manifest Generation

Module manifests are generated during build time by using the Java Compiler API via an annotation
processor. The annotation processor introspects the compiler-generated AST to produce a manifest
during build-time. Since Gradle has built-in support for annotation processing, manifest generation
is mostly transparent. The following example shows how the EDC module annotation processor can be
enabled in the build configuration:

```
dependencies {
    annotationProcessor(project(":tooling:module-processor"))
}

tasks.withType<JavaCompile> {
    val compilerArgs = options.compilerArgs
    compilerArgs.add("-Aedc.version=${project.version}")
    compilerArgs.add("-Aedc.id=${project.group}:${project.name}")
}

```

Once the processor is enabled, all module documentation, including configuration settings, are
resolved by traversing parsed Java class files and rendered as a manifest.

#### New Annotations and Conventions

Several new annotations are introduced to facilitate manifest generation.

#### SPI Modules

The `@Spi` annotation is intended to be placed on the top-level `package-info.java` in an SPI
module:

```java

@Target({ElementType.PACKAGE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Spi {

    /**
     * The readable module name.
     */
    String value();

    /**
     * Optional categories used to classify this module.
     */
    String[] categories() default "";
}
```

Service interfaces that define extension points are annotated with `@ExtensionPoint`:

```java

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface ExtensionPoint {
}
```

#### Extension Modules

Extension modules use the `@Extension` annotation on the `SystemExtension` class:

```java

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Extension {

    /**
     * The readable module name.
     */
    String value();

    /**
     * Optional categories used to classify this module.
     */
    String[] categories() default "";

}
```

The annotation processor uses the existing `@Setting`, `@Provides`, and `@Inject` annotations.
An `@SettingContext` provides for `ConfigMap` support:

```java

@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SettingContext {
    String value();
}
```

#### Markdown Documentation

SPI and Extension module documentation are taken from Javadoc on the `package-info.java`
and `SystemExtension` class respectively. The Javadoc will then be transformed into Markdown. This
approach ensures that detailed user technical documentation remains in-sync with code-level
documentation.

Settings are also be introspected and collated from code. For example, the following code are
introspected to produce two configuration entries, including proper resolution of constant values (
including the `PREFIX` symbol):

```java
private static final String PREFIX="edc.core.";

@Setting("Specifies the maximum number of retries")
public static final String MAX_RETRIES=PREFIX+"retry.retries.max";

@Setting
public static final String BACKOFF_MIN_MILLIS="edc.core.retry.backoff.min";
```

### Manifest Publication and Resolution

Manifests are published as a separate Maven classifier for their corresponding module. This enables
tolling to resolve EDC manifests using existing Maven-based repositories.

### Documentation Generation

As a second step, a Gradle plugin resolves module manifests from Maven archives and creates
markdown-based documentation from the metadata they contain. This documentation can be integrated
with GitHub pages. _Note that generated documentation could include modules from remote
repositories (by following the Gradle dependency graph) and link to Javadoc for further detail._

## Future Directions

It will be possible to create additional tooling based on EDC module manifests. For example, runtime
configuration templates can be produced by resolving module manifests referenced in a Gradle
Bill-of-Materials project. It will also be possible to build tools that categorize and discover
extensions. 
