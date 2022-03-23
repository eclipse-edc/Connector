# Dependency analysis

## Decision

The CI workflow is extended with two Gradle tasks for analyzing EDC module dependencies:

- The [Dependency Analysis Gradle Plugin](https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin) is used for detecting unused dependencies and dependencies declared on the wrong configuration (`api` vs `implementation` vs `compileOnly`, etc.).
- A custom build task defines rules on which module dependencies are allowed within EDC (e.g. no module may depend directly on any of the core modules).

The tasks are initially set to only emit warnings, but will be configured to fail builds once all warnings have been resolved.

The tasks are run in the CI workflow.

## Rationale

The [Gradle Java library plugin](https://docs.gradle.org/current/userguide/java_library_plugin.html#sec:java_library_recognizing_dependencies) defines a clear separation between configurations. This allows controlling and limiting the use of transitive dependencies.

The Java library plugin introduces the concept of ABI (Application Binary Interface) which includes (among others) types used in public methods. Dependencies that define those types should be defined in the `api` configuration. Other dependencies should be defined in the `implementation` configuration so that they are not transitively exposed, and therefore do not leak into the consumers' compile classpath.

The Dependency Analysis Gradle Plugin analyzes the abstract syntax tree to determine the ABI. On that basis, it can recommend which import configurations should be changed.
