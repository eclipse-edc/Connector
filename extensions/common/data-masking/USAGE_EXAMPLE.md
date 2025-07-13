# Data Masking Extension Usage Example

This example shows how to integrate the Data Masking Extension into your EDC connector.

## Launcher Integration

### build.gradle.kts

```kotlin
plugins {
    `java-library`
    id("application")
    alias(libs.plugins.shadow)
}

dependencies {
    // Core EDC dependencies
    implementation(project(":core:common:boot"))
    implementation(project(":core:common:connector-core"))
    implementation(project(":core:control-plane:control-plane-core"))
    implementation(project(":core:data-plane:data-plane-core"))

    // Data Masking Extension
    implementation(project(":extensions:common:data-masking"))

    // Other extensions...
    implementation(project(":extensions:common:http:jetty-core"))
    implementation(project(":extensions:common:json-ld"))
}

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    exclude("**/pom.properties", "**/pom.xml")
    mergeServiceFiles()
    archiveFileName.set("my-connector-with-masking.jar")
}
```

### config.properties

```properties
# Participant configuration
edc.participant.id=my-organization
edc.runtime.id=my-connector-runtime

# Data Masking Configuration
edc.data.masking.enabled=true
edc.data.masking.fields=name,phone,email,ssn,personalId

# Web server configuration
web.http.port=8181
web.http.path=/api

# Other EDC configuration...
```

## Runtime Behavior

When the connector starts, you'll see in the logs:

```
INFO Data Masking Extension initialized. Masking enabled: true, Fields: name, phone, email, ssn, personalId
INFO Data Masking Transformer registered
```

## Data Flow Example

When data flows through your connector, sensitive fields will be automatically masked:

**Before Masking (Internal):**

```json
{
  "customer": {
    "name": "John Doe",
    "phone": "+1-555-123-4567",
    "email": "john.doe@company.com",
    "ssn": "123-45-6789",
    "address": "123 Main St"
  }
}
```

**After Masking (External):**

```json
{
  "customer": {
    "name": "J*** D**",
    "phone": "+*-***-***-*567",
    "email": "j*******@company.com",
    "ssn": "***-**-*789",
    "address": "123 Main St"
  }
}
```

## Testing Your Integration

1. Build your connector:

   ```bash
   ./gradlew clean build
   ```

2. Run the connector:

   ```bash
   java -jar build/libs/my-connector-with-masking.jar
   ```

3. Test data masking by sending JSON data through your connector's API endpoints.

## Verification

You can verify the extension is working by:

1. Checking the startup logs for masking extension initialization
2. Sending test data through your connector and verifying sensitive fields are masked
3. Testing configuration changes (enabling/disabling, changing field lists)

The extension automatically integrates with EDC's transformation pipeline, so no additional code changes are required in your data processing logic.
