# About The Project

> The Eclipse Dataspace Connector provides a framework for sovereign, inter-organizational data exchange. It will implement the International Data Spaces standard (IDS) as well as relevant protocols associated with GAIA-X. The connector is designed in an extensible way in order to support alternative protocols and integrate in various ecosystems.

Please also refer to:

- The [Eclipse Project Homepage](https://projects.eclipse.org/projects/technology.dataspaceconnector)
- [International Data Spaces](https://www.internationaldataspaces.org)
- The [GAIA-X](https://gaia-x.eu) project
- The [Onboarding Guide](onboarding.md)

_Note: items marked with [TBW] indicate that the respective documentation is yet to be written_

## Built with

One of the guiding principles in developing the connector is simplicity and keeping the core small and efficient with as
little external dependencies as possible to avoid version conflicts. We do not want to force any third-party
dependencies onto our users, so we aim to avoid any of the big frameworks. Of course, if you want to use them, you still
can add them to your extensions (see: [TBW]). The connector is a plain Java application built with Gradle, but it can be
embedded into any form of application deployment.

For detailed information about the project, please have a look at our [Documentation](/docs/README.md).

# Getting Started

## Checkout and build code

The project requires JDK 11+. To get started:

``` shell 
git clone git@github.com:eclipse-dataspaceconnector/DataSpaceConnector.git

cd DataSpaceConnector ```

./gradlew clean build
```

That will build the connector and run unit tests.

## [Optional] Setup your IDE

If you wish to configure your IDE/editor to automatically apply the EDC code style, please
follow [this guide](styleguide.md).

_Note: the style guide will be checked/enforced in Github Actions._

## Run your first connector

Connectors can be started using the concept of "launchers", which are essentially compositions of Java modules defined
as gradle build files. There is a `basic` launcher, which launches a simple connector that has no cloud-based extensions
whatsoever.

In a shell run

```shell
./gradlew :launchers:basic:shadowJar
java -jar launchers/basic/build/libs/dataspaceconnector-basic.jar
```

Once it says `"Dataspace Connector ready"` the connector is up and running.

More information about the extension concept can be found here [TBW].

More information about shadowJar can be found [here](https://github.com/johnrengelman/shadow).

## Generate the OpenApi specification

Please refer to [this document](./openapi.md).

# Directory structure

### `spi`

This is the primary extension point for the connector. It contains all necessary interfaces that need to be implemented
as well as essential model classes and enums. Basically, the `spi` modules defines the extent to what users can
customize and extend the code.

### `core`

Contains all absolutely essential building that is necessary to run a connector such as `TransferProcessManager`,
`ProvisionManager`, `DataFlowManager`, various model classes, the protocol engine and the policy piece. While it is
possible to build a connector with just the code from the `core` module, it will have very limited capabilities to
communicate and to interact with a data space.

### `extensions`

This contains code that extends the connector's core functionality with technology- or cloud-provider-specific code. For
example a transfer process store based on Azure CosmosDB, a secure vault based on Azure KeyVault, etc. This is where
technology- and cloud-specific implementations should go.

If someone were to create a configuration service based on Postgres, then the implementation should go into
the `extensions/database/configuration-postgres` module.

### `launchers`

Launchers are essentially connector packages that are runnable. What modules get included in the build (and thus: what
capabilities a connector has) is defined by the `build.gradle.kts` file inside the launcher subdirectory. That's also
where a Java class containing a `main` method should go. We will call that class a "runtime" and in order for the
connector to become operational the `runtime` needs to perform several important tasks (="bootstrapping"). For an
example take a look at
[this runtime](launchers/basic/src/main/java/org/eclipse/dataspaceconnector/runtime/ConnectorRuntime.java)

### `resources/charts`

Contains a Helm chart for the EDC runtime. You can use the `launchers/generic/Dockerfile` to build a runtime image
for your connector runtime, and deploy the resulting image to Kubernetes.

### `data-protocols`

Contains implementations for communication protocols a connector might use, such as IDS.

### `samples`

Contains code that demonstrates how the connector can be used in various scenarios. For example, it shows how to run a
connector from a unit test in order to try out functionality quickly or how to implement an outward-facing REST API for
a connector.

### `common`

Contains utility code such as collection utils, string utils and helper classes.

### `scripts`

Contains several scripts to deploy a connector in an AKS cluster on Microsoft Azure using Terraform.

## Code style & Patterns

Please refer to the dedicated [style guide](styleguide.md) and the patterns we documented in [architecture principles](docs/architecture-principles.md).

## Roadmap

See [here](CONTRIBUTING.md#project-and-milestone-planning) for more information about project and
milestone planning. Scheduled and ongoing milestones are listed 
[here](https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/milestones).

## Contributing

See [how to contribute](CONTRIBUTING.md).

## Tags

Available tags can be found [here](https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/tags).
