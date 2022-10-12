# Build your own connector

EDC is a set of modules that, wedged together can build up different components to be used to create your dataspace or
to join to an existent one.

This guide will explain how to set up a **connector**, that would be able to communicate with other connectors to
negotiate contracts and transfer data.

A connector is logically divided in 3 parts:
- control-plane
- data-plane
- data-plane-selector

The connector can run as a single deployable composed by the 3 parts or as separated units, the latter will permit
to scale data-planes independently of control-planes.

## Build and run a monolithic connector

The easiest way to build up a connector is to build all their parts into a single deployment unit.
To do this, create a new gradle project with a `build.gradle.kts` file in it:
```kotlin
plugins {
    `java-library`
    id("application")
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.iais.fraunhofer.de/artifactory/eis-ids-public/")
    }
}

// pick-up the latest version from https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/releases
val edcVersion = "<x.x.x>"

dependencies {
    // the core control-plane module set
    implementation("org.eclipse.dataspaceconnector:control-plane-core:${edcVersion}")

    // the ids protocol module
    implementation("org.eclipse.dataspaceconnector:ids:${edcVersion}")

    // a test implementation of the `IdentityService`
    implementation("org.eclipse.dataspaceconnector:iam-mock:${edcVersion}")

    // data management api, will permit to manage the connector through a REST interface
    implementation("org.eclipse.dataspaceconnector:data-management-api:${edcVersion}")

    // the core data-plane module set
    implementation("org.eclipse.dataspaceconnector:data-plane-core:${edcVersion}")
}

// configure the main class
application {
    mainClass.set("org.eclipse.dataspaceconnector.boot.system.runtime.BaseRuntime")
}

// build the application as a single jar that will be available in `build/libs/connector.jar`
tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    mergeServiceFiles()
    archiveFileName.set("connector.jar")
}

```

Note that no `data-plane-selector` modules are needed, this because the `data-plane` is built as embedded in the connector.

Build:
```
./gradlew build
```

Run:
```
java -jar build/libs/connector.jar
```

### Setting database

The connector you just built stores all the data in memory by default. This is obviously not enough to use the connector
in an environment that's not your local machine.
Currently, there are two different database extensions for the control-plane:

- `control-plane-sql`: Sql database ([postgresql](https://www.postgresql.org/) is the only dialect currently supported)
- `control-plane-cosmos`: [Microsoft CosmosDB](https://azure.microsoft.com/products/cosmos-db) database

### Setting data-plane

To make the *control-plane* interact with the *data-plane*, it will need at least one of these extensions:
- `data-plane-transfer-client`: provides a client to delegate data transfer to the *data-plane*.
- `data-plane-transfer-sync`: provides services to use the *data-plane* as a proxy for querying data from the provider data source.

The *data-plane* will need extensions to being able to read/write data from/to different protocols/services, e.g.:
- `data-plane-api`: will make the *data-plane* expose APIs that are needed to actually transfer data
- `data-plane-http`: support HTTP protocol
- `data-plane-azure-storage`: support [Azure Blob Storage](https://azure.microsoft.com/products/storage/blobs/) service
- `data-plane-aws-s3`: support [AWS S3](https://aws.amazon.com/s3/) service
- other custom provided *data-plane* extensions