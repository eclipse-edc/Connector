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

## Gradle Plugins

We use the official Swagger Gradle plugins:

- `"io.swagger.core.v3.swagger-gradle-plugin"`: used to generate a `*.yaml` file per module

So in order for a module to be picked up by the Swagger Gradle plugin, simply add it to the `build.gradle.kts`:

```kotlin
// in yourModule/build.gradle.kts

val rsApi: String by project

plugins {
    `java-library`
    id(libs.plugins.swagger.get().pluginId) //<-- add this
}

dependencies {
    implementation("jakarta.ws.rs:jakarta.ws.rs-api:${rsApi}") //<-- you'll probably already have this
    // other dependencies
}

```

If you developed a REST endpoint, you very likely already have the `jakarta.ws.rs:....` part in your build file. If you
don't, it'll get added automatically for you.

### Categorizing your API

All APIs in EDC should be "categorized", i.e. they should belong to a certain group of APIs.
Please see [this decision record](./decision-records/2022-11-09-api-refactoring/renaming.md) for reference. In order to
add your module to one of the categories, simply add this block to your module's `build.gradle.kts`:

```kotlin
plugins {
    `java-library`
    id(libs.plugins.swagger.get().pluginId)
}

dependencies {
    // ...
}

// add this block:
edcBuild {
    swagger {
        apiGroup.set("management-api")
    }
}
```

This tells the build plugin how to categorize your API and SwaggerHub will list it accordingly.
_Note: currently we have categories for `control-api` and `management-api`_

## How to generate code

This feature does **neither** expose the generated files through a REST endpoint providing any sort of live try-out
feature, **nor** does it generate any sort of client code. A visual documentation page for our APIs is served
through [SwaggerHub](https://app.swaggerhub.com/home?type=API).

However, there is Gradle plugin capable of generating client code.
Please refer to the [official documentation](https://github.com/int128/gradle-swagger-generator-plugin).  
