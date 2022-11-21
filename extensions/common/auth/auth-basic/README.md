# Basic authentication

This extension provides a `AuthenticationService` implementation for basic authentication. This module will be active
if you provide at least one credential pair.

## Usage/Example

In your `Vault` instance you have to provide the password for each user. You can put them under any key you want, but we
would recommend using the following key format:

```plain
api-basic-auth-<USERNAME>
```

Regarding the keys you have to provide in your EDC configuration the vault key for each user:

```properties
edc.api.auth.basic.usera: api-basic-auth-usera
edc.api.auth.basic.userb: api-basic-auth-userb
```

To use this module e.g. together with the Management API and Azure Vault just have to include the following into
your `build.gradle.kts`:

```kotlin
dependencies {
    implementation(project(":extensions:common:vault:vault-azure"))
    implementation(project(":extensions:common:auth:auth-basic"))
    implementation(project(":extensions:control-plane:api:management-api"))
}
```
