# Configuration injection

## Decision

In addition to service injection, the EDC project will support _configuration injection_ ("CI") in future releases.

## Rationale

In an effort to improve the ease-of-use and to lower the barrier of entry for developers we will provide a feature to
configure extensions with an annotation mechanism. Resolving configuration can result in convoluted and difficult to
read code.

## Approach

### Requirements

- default values: it should be possible to provide a default value for configuration fields
- optionality: configuration fields that are not required should be `null` in case there is no config value for them
- type flexibility: at least `String`, `Integer`, `Long`, `Double` and `Boolean` must be supported
- annotation-based: the configuration injection is triggered by annotations

Configuration values are resolved during runtime startup, more specifically during the dependency injection phase.
Technically they are another type of `InjectionPoint`.

### Resolving and injecting plain configuration values

Extension classes can declare fields of type `String`, `Integer`, `Long`, `Double` or `Boolean` and annotate them with
the `@Setting` annotation:

```java
public class SomeExtension implements ServiceExtension {

    @Setting(key = "edc.iam.publickey.alias")
    private String publicKeyAlias;
}
```

This would check if a config value `edc.iam.publickey.alias` is present on the `Config` object, and if so, assign its
value to the field. An injection error would be raised if no config value is found for `edc.iam.publickey.alias`. This
can be avoided by declaring a default value:

```java
public class SomeExtension implements ServiceExtension {

    @Setting(key = "edc.iam.publickey.alias", defaultValue = "foobar")
    private String publicKeyAlias;

    @Setting(key = "edc.some.timeout", defaultValue = "60")
    private Integer someTimeout; // default value gets converted to int

    @Setting(key = "edc.some.fraction", defaultValue = "barbaz")
    private Double someFraction; // runtime exception if the default value is used: "barbaz" cannot be converted to double
}
```

Note that default values are supplied as Strings, and an attempt is made to convert them to the appropriate type. An
injection error is raised raised if the value cannot be converted to the desired type.

Alternatively, config values can be marked as optional:

```java
public class SomeExtension implements ServiceExtension {

    @Setting(key = "edc.iam.publickey.alias", required = false)
    private String publicKeyAlias;
}
```

If no config value is found for `edc.iam.publickey.alias`, then the field is `null`.

Note that if a `defaultValue` is supplied, the `required` attribute becomes meaningless.

### Resolving and injecting configuration objects

In addition to supplying simple config values it should be possible to have the injection mechanism construct a _config
object_:

```java
public class SomeExtension implements ServiceExtension {

    @Configuration
    private KeyConfig keyConfig;
}

public record KeyConfig(@Setting(key = "edc.iam.publickey.alias") String alias,
                        @Setting(key = "edc.iam.key.algorithm") String algorithm) {
}

// alternatively:
public class KeyConfig{

    @Setting(key = "edc.iam.publickey.alias")
    private String alias;

    @Setting(key = "edc.iam.key.algorithm")
    private String algorithm;

    // MUST have a public default constructor!
}
```

These are Java POJOs that contain the actual config values. The same principle applies as before, but the
`@Setting`-annotated fields are compounded in a class or a `record`. However, some limitations apply:

- config record classes can only contain constructors where every parameter is annotated with `@Setting`
- `@Configuration`-annotated fields are optional if and only if **all** `@Setting`-annotated fields within them have the
  `required = false` attribute. This optionality is _implicit_ and cannot be configured.
- `@Configuration`-annotated fields **cannot** have default values because those would have to be compile-time constant.
  However, all config values _within_ can have default values.
- all `@Setting`-annotated fields of a `@Configuration` object must be optional or resolvable, either from config or via
  a default value, otherwise the CI mechanism fails with an error
- if config objects are normal classes, they **must** have a public default constructor. All other constructors are
  ignored by the CI mechanism
- `@Setting`-annotated fields must not be `final`
- nested config objects are not supported

### Changes to the `autodoc` processor

We already have a `@Setting` annotation in the `runtime-metamodel` component, which we should re-use for this. A new
attribute named `key` is added to the `@Setting` annotation. If present, it triggers the config injection mechanism.

Currently, the description is the default `value()` attribute of the `@Setting` annotation. This should eventually be
changed so that the `key` becomes the default value, and `description` is another named attribute.

### Error reporting

By piggy-backing on the dependency injection mechanism, errors are automatically reported in a collated way. So multiple
unresolved config values would be reported as one:

```
Caused by: org.eclipse.edc.boot.system.injection.EdcInjectionException: The following injected fields were not provided or could not be resolved:
Configuration value "fooBarBaz" of type class java.lang.Long (config key 'foo.bar.baz')
Configuration object 'config' of type 'class org.eclipse.edc.some.FooBarExtension$FooBarConfig' in class class org.eclipse.edc.some.FooBarExtension
	at org.eclipse.edc.boot.system.DependencyGraph.of(DependencyGraph.java:136)
	at org.eclipse.edc.boot.system.ExtensionLoader.loadServiceExtensions(ExtensionLoader.java:85)
	at org.eclipse.edc.boot.system.runtime.BaseRuntime.createExtensions(BaseRuntime.java:164)
	at org.eclipse.edc.boot.system.runtime.BaseRuntime.boot(BaseRuntime.java:92)
	... 1 more
```
