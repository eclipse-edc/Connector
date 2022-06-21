# Configuration

It is possible to configure the Eclipse Dataspace Connector. All configuration are retrieved by their key from an
extension, that implements the Configuration Extension interface.

Whether this configuration may happen when the application is running or only on start-up may vary for each
configuration extension.

## Create new Setting

Settings are identified by their key. When creation new setting please follow the best practices listed below.

Settings are retrieved from the ServiceExtensionContext interface.

**Best practices for Settings**:

- setting keys start with _edc_
- setting keys are defined in _private static final_ fields
- setting fields are annotated with the _@EdcSetting_ marker interface
- settings have a valid default value

**Example Setting**

```java
class ExampleSetting {

    @EdcSetting
    private final static String EDC_IDS_TITLE = "edc.ids.title";

    private final static String DEFAULT_TITLE = "Default Title";

    private String getTitle(ServiceExtensionContext context) {
        return context.getSetting(EDC_IDS_TITLE, DEFAULT_TITLE);
    }

}
```

## Configuration Extension

The integration of each configuration extension may vary. Please have a look at their corresponding README.md files.

The following extensions implement the ConfigurationExtension interface.

- [File System Configuration](/extensions/filesystem/configuration-fs/README.md)
