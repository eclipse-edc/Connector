# Generating the OpenApi Spec (*.yaml)

It is possible to generate an OpenApi spec in the form of a `*.yaml` file by invoking two simple Gradle tasks.

## Generate `*.yaml` files

Every module (=subproject) that contains REST endpoints is scanned for Jakarta Annotations which are then used to
generate a `*.yaml` specification for that particular module. This means that there is one `*.yaml`file _per module_,
resulting in several `*.yaml` files.

Those files are named `MODULENAME.yaml`, e.g. `observability.yaml` or `control.yaml`.

To re-generate those files, simply invoke

```shell
./gradlew clean resolve
```

This will generate all `*.yaml` files in the `resources/openapi/yaml` directory.

### Merge the files

Unfortunately those files are not yet usable, because they need to be **merged together**. For that we need another
Gradle task:

```shell
./gradlew mergeOpenApiFiles
```

which takes all `*.yaml` files located in `resources/openapi/yaml`, combines them into a single file and puts that
into `resources/openapi/openapi.yaml`

The resulting `openapi.yaml` can then be used to generate client code, expose static web content, etc.

> **IMPORTANT: these two Gradle tasks must be executed separately! `./gradlew resolve mergeOpenApiFiles` will NOT
work!**

### Generate static SwaggerUI

Finally, after the new OpenAPI spec has been generated, we should re-generate the Swagger UI. It is available as static
web content located [in the docs folder](docs/swaggerui/index.html).

To do that, simply execute the following Gradle task:

```shell
./gradlew generateSwaggerUi
```

## Gradle Plugins

We use two different Gradle plugins:

- `"io.swagger.core.v3.swagger-gradle-plugin"`: used to generate a `*.yaml` file per module
- `"com.rameshkp.openapi-merger-gradle-plugin"`: used to merge all the `*.yaml` files together

So in order for a module to be picked up by the Swagger Gradle plugin, simply add it to the `build.gradle.kts`:

```kotlin
// in yourModule/build.gradle.kts

val rsApi: String by project

plugins {
    `java-library`
    id("io.swagger.core.v3.swagger-gradle-plugin") //<-- add this
}

dependencies {
    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}") //<-- you'll probably already have this
    // other dependencies
}
```

If you developed a REST endpoint, you very likely already have the `jakarta.ws.rs:....` part in your build file.
However, if you leave it out, the Swagger Gradle Plugin will report an error.

## How to generate code

This feature does **neither** expose the generated files through a REST endpoint providing any sort of live try-out
feature, **nor** does it generate any sort of client code. The static web content for Swagger UI is merely served
through our [documentation page](https://eclipse-dataspaceconnector.github.io/DataSpaceConnector/).

However, the same gradle plugin we use for generating the static HTML content is also capable of generating client code.
Please refer to the [official documentation](https://github.com/int128/gradle-swagger-generator-plugin).  