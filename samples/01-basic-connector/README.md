# Running a simple connector

A runnable connector consists of a `Runtime` and a build file, in our case this is a `build.gradle.kts`.

The first thing we need is the `Runtime` which is the main entry point to the connector application, same as with any
other Java program. In this sample we use the [`BaseRuntime`](../../core/bootstrap/src/main/java/org/eclipse/dataspaceconnector/system/runtime/BaseRuntime.java),
but this can be extended (take a look at the [`custom-runtime`](../other/custom-runtime) sample for more information)

The second thing we need is a [gradle build file](build.gradle.kts)
that contains the essential dependencies. We'll need at least the following things:

```kotlin
dependencies {
    implementation(project(":core"))
    implementation(project(":extensions:in-memory:transfer-store-memory"))
    implementation(project(":extensions:in-memory:assetindex-memory"))
    implementation(project(":extensions:in-memory:negotiation-store-memory"))
}
```

> _Additional dependencies will be added to this list in the future, so be sure to check back regularly!_

with that we can build and run the connector from the root directory:

```bash
./gradlew clean samples:01-basic-connector:build
java -jar samples/01-basic-connector/build/libs/basic-connector.jar
```

_Note: the above snippet assumes that you did not alter the build file, i.e. the `shadow` plugin is used and the build
artifact resides at the path mentioned above. Also, we assume usage of the Gradle Wrapper, as opposed to a local Gradle
installation._

If everything works as intended you should see command-line output similar to this:

```bash
INFO 2021-12-01T11:43:03.912706069 Secrets vault not configured. Defaulting to null vault.
INFO 2021-12-01T11:43:04.056438198 Initialized Core Services extension.
INFO 2021-12-01T11:43:04.05703093 Initialized Schema Registry
INFO 2021-12-01T11:43:04.078382201 Initialized Web extension
INFO 2021-12-01T11:43:04.081509795 Initialized Core Transfer extension
INFO 2021-12-01T11:43:04.082133289 Initialized In-Memory Transfer Process Store extension
INFO 2021-12-01T11:43:04.082889602 Initialized In-Memory Asset Index extension
INFO 2021-12-01T11:43:04.083409126 Initialized In-Memory Contract Negotiation Store extension
INFO 2021-12-01T11:43:04.092667818 Initialized Core Contract Service Extension
INFO 2021-12-01T11:43:04.122422102 Registered Web API context at: /api/*
INFO 2021-12-01T11:43:04.143321284 HTTP listening on 8181
INFO 2021-12-01T11:43:04.38748779 Started Web extension
INFO 2021-12-01T11:43:04.389002651 Started Core Transfer extension
INFO 2021-12-01T11:43:04.389110926 Started Initialized In-Memory Transfer Process Store extension
INFO 2021-12-01T11:43:04.389227079 Started In-Memory Asset Index extension
INFO 2021-12-01T11:43:04.389345155 Started Initialized In-Memory Contract Negotiation Store extension
INFO 2021-12-01T11:43:04.390347726 Started Core Contract Service Extension
INFO 2021-12-01T11:43:04.390636036 edc-527dc74a-761b-4207-98f6-e31845454e28 ready
```

This basic connector - while perfectly fine - does not offer any outward-facing API, nor does it provide any
connector-to-connector communication protocols. However, it will serve us as platform to build out more complex
scenarios.
