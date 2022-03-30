# Basic authentication

This extension provides a `AuthenticationService` implementation for basic authentication. This module will be active
if you provide at least one credential pair.

## Usage/Example

You have to provide in your configurations the specific credentials. Therefor you can also provide multiple:

```properties
edc.api.auth.basic.usera: my-password
edc.api.auth.basic.userb: other-password
```

To use this module e.g. together with the DataManagement API you just have to include it into your `build.gradle.kts`:

```kotlin
dependencies {
    implementation(project(":extensions:api:auth-basic"))
    implementation(project(":extensions:api:data-management"))
}
```

## Configurations

The module allows specifying multiple credentials if you want to. To declare credentials you have to use the following
syntax: `edc.api.auth.basic.<USERNAME>: <PASSWORD>`
