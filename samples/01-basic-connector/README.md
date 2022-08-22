# Running a simple connector

A runnable connector consists of a `Runtime` and a build file, in our case this is a `build.gradle.kts`.

The first thing we need is the `Runtime` which is the main entry point to the connector application, same as with any
other Java program. In this sample we use the [`BaseRuntime`](../../core/boot/src/main/java/org/eclipse/dataspaceconnector/boot/system/runtime/BaseRuntime.java),
but this can be extended (take a look at the [`custom-runtime`](../other/custom-runtime) sample for more information)

The second thing we need is a [gradle build file](build.gradle.kts)
that contains the essential dependencies. We'll need at least the following things:

```kotlin
dependencies {
    implementation(project(":core:control-plane:control-plane-core"))
    
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
INFO 2022-01-13T13:43:57.677973407 Secrets vault not configured. Defaulting to null vault.
INFO 2022-01-13T13:43:57.680158117 Initialized Null Vault
INFO 2022-01-13T13:43:57.851181615 Initialized Core Services
INFO 2022-01-13T13:43:57.852046576 Initialized Schema Registry
INFO 2022-01-13T13:43:57.853010987 Initialized In-Memory Transfer Process Store
INFO 2022-01-13T13:43:57.856956651 Initialized Core Transfer
INFO 2022-01-13T13:43:57.857664924 Initialized In-Memory Asset Index
INFO 2022-01-13T13:43:57.857957714 Initialized In-Memory Contract Definition Store
INFO 2022-01-13T13:43:57.860738605 Initialized Core Contract Service
INFO 2022-01-13T13:43:57.861390422 Initialized In-Memory Contract Negotiation Store
INFO 2022-01-13T13:43:57.862002044 Started Core Services
INFO 2022-01-13T13:43:57.862247712 Started Schema Registry
INFO 2022-01-13T13:43:57.862782289 Started In-Memory Transfer Process Store
INFO 2022-01-13T13:43:57.8635804 Started Core Transfer
INFO 2022-01-13T13:43:57.86371948 Started In-Memory Asset Index
INFO 2022-01-13T13:43:57.863838751 Started In-Memory Contract Definition Store
INFO 2022-01-13T13:43:57.86497334 Started Core Contract Service
INFO 2022-01-13T13:43:57.865146132 Started In-Memory Contract Negotiation Store
INFO 2022-01-13T13:43:57.866073376 edc-e796b518-35f0-4c45-a333-79ca20a6be06 ready
```

This basic connector - while perfectly fine - does not offer any outward-facing API, nor does it provide any
connector-to-connector communication protocols. However, it will serve us as platform to build out more complex
scenarios.
