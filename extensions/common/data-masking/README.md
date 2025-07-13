# EDC Data Masking Extension

## Overview

The Data Masking Extension is a configurable EDC extension that automatically masks sensitive data fields during data exchange flows. It acts as a protective filter, similar to hiding personal details on a printed receipt, ensuring that sensitive information is protected while maintaining data utility.

## Features

- **Automatic masking** of sensitive fields in JSON data
- **Configurable field selection** - choose which fields to mask
- **Multiple masking strategies** for different data types
- **Seamless integration** with EDC's transformation pipeline
- **No performance impact** when disabled
- **Extensive test coverage** with clear examples

## Supported Data Types

### Name Masking

- **Rule**: Keep initials visible, mask remaining characters
- **Example**: `"Jonathan Smith"` → `"J******* S****"`

### Phone Number Masking

- **Rule**: Mask all but the last three digits
- **Example**: `"+44 7911 123456"` → `"+** **** ***456"`

### Email Address Masking

- **Rule**: Keep first character and domain visible, mask the rest
- **Example**: `"jonathansmith@example.com"` → `"j************@example.com"`

## Configuration

### Enable/Disable Masking

```properties
# Enable or disable data masking (default: true)
edc.data.masking.enabled=true
```

### Configure Specific Fields

```properties
# Comma-separated list of field names to mask (optional)
edc.data.masking.fields=name,phone,email,phoneNumber,customField
```

If no specific fields are configured, the extension will mask these default fields:

- `name`
- `phone`, `phoneNumber`, `phone_number`
- `email`, `emailAddress`, `email_address`

## Integration Guide

### 1. Add Dependency

Add the extension to your EDC launcher's `build.gradle.kts`:

```kotlin
dependencies {
    implementation(project(":extensions:common:data-masking"))
    // ... other dependencies
}
```

### 2. Configuration

Create or update your `config.properties` file:

```properties
# Enable data masking
edc.data.masking.enabled=true

# Optional: specify custom fields to mask
edc.data.masking.fields=name,phone,email,customSensitiveField
```

### 3. Build and Run

```bash
# Build the connector
./gradlew clean build

# Run your connector
java -jar build/libs/your-connector.jar
```

## Usage Examples

### Basic JSON Masking

**Input:**

```json
{
  "name": "Jonathan Smith",
  "phone": "+44 7911 123456",
  "email": "jonathansmith@example.com",
  "age": 30
}
```

**Output:**

```json
{
  "name": "J******* S****",
  "phone": "+** **** ***456",
  "email": "j************@example.com",
  "age": 30
}
```

### Complex Nested Structures

**Input:**

```json
{
  "users": [
    {
      "name": "Alice Johnson",
      "contact": {
        "email": "alice@company.com",
        "phone": "555-123-4567"
      }
    }
  ]
}
```

**Output:**

```json
{
  "users": [
    {
      "name": "A**** J******",
      "contact": {
        "email": "a****@company.com",
        "phone": "***-***-*567"
      }
    }
  ]
}
```

## Architecture

The extension consists of several key components:

### DataMaskingService (SPI)

- Interface defining masking operations
- Extensible for custom masking strategies

### DataMaskingServiceImpl

- Default implementation with configurable rules
- Handles JSON parsing and field detection
- Supports nested objects and arrays

### DataMaskingTransformer

- Integrates with EDC's transformation pipeline
- Automatically detects JSON data with sensitive fields
- Applies masking during data exchange

### DataMaskingExtension

- ServiceExtension that registers all components
- Handles configuration injection
- Manages extension lifecycle

## Testing

The extension includes comprehensive tests:

### Unit Tests

```bash
./gradlew :extensions:common:data-masking:test
```

### Integration Tests

```bash
./gradlew :extensions:common:data-masking:integrationTest
```

### Test Coverage

- Individual masking rules (name, phone, email)
- JSON data transformation
- Configuration scenarios
- Error handling
- Edge cases and validation

## Advanced Configuration

### Custom Masking Fields

```properties
# Mask only specific fields
edc.data.masking.fields=customerName,phoneNumber,emailAddress,ssn
```

### Disable for Specific Deployments

```properties
# Disable masking in development environment
edc.data.masking.enabled=false
```

## Performance Considerations

- **Zero overhead** when disabled
- **Minimal impact** when enabled - only processes JSON data containing sensitive fields
- **Efficient field detection** using case-insensitive matching
- **Graceful error handling** - returns original data if masking fails

## Troubleshooting

### Extension Not Loading

1. Verify the extension is in your classpath
2. Check that `META-INF/services/org.eclipse.edc.spi.system.ServiceExtension` exists
3. Ensure no conflicting transformer registrations

### Masking Not Applied

1. Check `edc.data.masking.enabled=true` in configuration
2. Verify field names match configuration (case-insensitive)
3. Ensure data is valid JSON format
4. Check logs for masking warnings

### Performance Issues

1. Verify masking is only enabled where needed
2. Check JSON data size - very large documents may impact performance
3. Monitor transformation pipeline logs

## License

This extension is licensed under the Apache License 2.0, same as the Eclipse Dataspace Connector project.

## Contributing

Contributions are welcome! Please ensure:

- All tests pass
- Code follows EDC conventions
- Documentation is updated
- New features include appropriate tests
