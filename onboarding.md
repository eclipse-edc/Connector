# Onboarding and first steps

This document serves as step-by-step guide for new community members and assumes a working knowledge of the Eclipse
Dataspace Connector (EDC) nomenclature. If you do not know about the EDC nomenclature we strongly advise reading the
documentation and/or watch the introductory video.

Also, a working knowledge of Git, Gradle, Java and HTTP is presumed.

We'll assume that you've just checked out the EDC code base and have Java 11 installed on your development machine. The
Eclipse Dataspace Connector is built using Java 11, so we'll assume you have that installed as well. If not, please
download and install JDK 11 for your OS.

Command examples in this document will use the `bash` syntax, but any other shell should be fine as well.

## Run a basic connector

A runnable connector consists of a `Runtime` and a build file, in our case this is a `build.gradle.kts`.

The first thing we need is the `Runtime` which is the main entry point to the connector application, same as with any
other Java program. It basically uses a few bootstrap classes to get the core up and running. For an example please take
a look
at [this runtime](samples/01-basic-connector/src/main/java/org/eclipse/dataspaceconnector/runtime/ConnectorRuntime.java)
.

The second thing we need is a gradle build file (an example can be
found [here](samples/01-basic-connector/build.gradle.kts))
that contains the essential dependencies. We'll need at least the following things:

```kotlin
dependencies {
    implementation(project(":core:bootstrap"))
    implementation(project(":core:transfer"))
    implementation(project(":core:protocol:web"))
    implementation(project(":extensions:in-memory:transfer-store-memory"))
    implementation(project(":extensions:filesystem:configuration-fs"))

    // junit dependencies omitted for clarity
}
```

with that we can build and run the connector from the root directory:

```bash
./gradlew clean samples:01-basic-connector:shadowJar
java -jar samples/01-basic-connector/build/libs/basic-connector.jar
```

_Note: the above snippet assumes that you did not alter the build file, i.e. the `shadow` plugin is used and the build
artifact resides at the path mentioned above. Also, we assume usage of the Gradle Wrapper, as opposed to a local Gradle
installation._

The keen-eyed reader may have noticed that the `:core:protocol:web` dependency is not strictly speaking _necessary_, but
removing it will cause the application to terminate right after it has started. The dependency causes the application to
be kept running due to an embedded Jetty being started.

If everything works out fine you should see command-line output similar to this:

```bash
INFO 2021-09-06T16:21:50.643126 Configuration file does not exist: dataspaceconnector-configuration.properties. Ignoring.
INFO 2021-09-06T16:21:50.66852 Secrets vault not configured. Defaulting to null vault.
INFO 2021-09-06T16:21:50.802936 Initialized Core Services extension.
INFO 2021-09-06T16:21:50.80613 Initialized Core Transfer extension
INFO 2021-09-06T16:21:50.828181 Initialized Web extension
INFO 2021-09-06T16:21:50.828686 Initialized In-Memory Transfer Process Store extension
INFO 2021-09-06T16:21:50.831001 Started Core Transfer extension
INFO 2021-09-06T16:21:50.865946 Registered Web API context at: /api/*
INFO 2021-09-06T16:21:50.885099 HTTP listening on 8181
INFO 2021-09-06T16:21:51.117412 Started Web extension
INFO 2021-09-06T16:21:51.117517 Started Initialized In-Memory Transfer Process Store extension
INFO 2021-09-06T16:21:51.117577 Dataspace Connector ready
```

This basic connector - while perfectly fine - does not offer any outward-facing API, nor does it provide any
connector-to-connector communication protocols. However, it will serve us as platform to build out more complex
scenarios.

_The contents of this chapter are in `samples/01-basic-connector/`._

## Write your first extension

## Create a filesystem-based configuration

## Implement a simple file transfer

## Improve the file transfer

