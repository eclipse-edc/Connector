# Advanced dependency resolution: default provider methods

This feature (and this document) is aimed at platform developers who intend to provide additional platform features. It
is **not** intended for people who simply want to contribute a technology extension, such as a database implementation
or a storage backend!

## Fallbacks versus extensibility

Be aware that default provider methods are intended to provide fallback implementations for services. They are not
intended to achieve extensibility - that is what extensions are for. There is a subtle but important semantic difference
between _fallback implementations_ and _extensibility_.

### Fallback implementations

Fallbacks are meant as safety net, so as not to end up _without_ an implementation for an interface. A good example for
this are in-memory store implementations. It is expected that an actual persistence implementation is contributed by
another extension. In-mem stores get you up and running quickly, but we wouldn't recommend using them in production
environments. Typically, fallbacks should not have any dependencies onto other services.

> Default-provided services, even though they are on the classpath, only get instantiated if there is no other
> implementation.

### Extensibility

In contrast, _extensibility_ refers to the possibility of swapping out one implementation of a service for another by
choosing the appropriate module at compile time. Each implementation must therefore be contained in its own java module,
and the choice between one or the other is made by referencing one or the other in the build file. In this case,
the `@Provider`-annotation **must not** have the `isDefault` attribute. This is also the case if there will likely only
ever be one implementation for a service.

Another example for extensibility is the `IdentityService`: there could be several implementations for it (OAuth,
DecentralizedIdentity, etc.), but providing either one as default would make little sense, because all require external
services to work. Each implementation would be in its own module and get instantiated by its own extension.

> Provided services only get instantiated if they are on the classpath.

## Deep-dive into extension lifecycle management

Generally every extension goes through these lifecycle stages during loading:

- `inject`: all fields annotated with `@Inject` are resolved
- `initialize`: the `initialize()` method is invoked. All required collaborators are expected to be resolved after this.
- (`prepare`: planned in future releases.)
- `provide`: all `@Provider` methods are invoked, the object they return is registered in the context.

Due to the fact that default provider methods act a safety net, they only get invoked if no other provider method offers
the same service type.

### Example 1 - provider method

`@Provider` methods get invoked regardless, and after the `initialze` phase. That means, assuming both extensions are on
the classpath, the extension that declares the provider method (= `ExtensionA`) will get fully instantiated before
another extension (= `ExtensionB`) can use the provided object:

```java
public class ExtensionA {
    @Inject
    private SomeStore store;

    @Provider
    public SomeService getSomeService() {
        return new SomeServiceImpl(store);
    }
}

public class ExtensionB {
    @Inject
    private SomeService service;
}
```

After building the dependency graph, the loader mechanism would first fully construct `ExtensionA`, i.e.
`getSomeService()` is invoked, and the instance of `SomeServiceImpl` is registered in the context. After
that, `ExtensionB` gets constructed, and by the time it goes through its `inject` phase, the injected service is already
in the context, so the `SomeService` field gets resolved properly.

### Example 2 - default provider method

Methods annotated with `@Provider(isDefault=true)` only get invoked if there is no other provider method for that
service. Modifying example 1 slightly we get:

```java
public class ExtensionA {

    @Inject
    private SomeStore store;

    @Provider(isDefault = true)
    public SomeService getSomeService() {
        return new SomeServiceImpl(store);
    }
}

public class ExtensionB {
    @Inject
    private SomeService service;
}
```

The biggest difference here is the point in time at which `getSomeService` is invoked. Default provider methods get
invoked _when the `@Inject` dependency is resolved_. That means, they get invoked during `ExtensionB`'s inject phase,
and _not_ during `ExtensionA`'s provide phase. There is no guarantee that `ExtensionA` is already initialized by that
time, because the extension loader does not know whether it needs to invoke `getSomeService` until the very last moment,
i.e. when resolving `ExtensionB`'s `service` field. By that time, the dependency graph is already built. Consequently,
default provider methods could get invoked before the defining extension's `provide` phase - even before it's
`initialize` phase has completed. Here, this means that `store` might still be `null`.

## Usage guidelines when using default providers

From the previous sections and the examples demonstrated above we can derive a few important guidelines:

- do not use them to achieve extensibility. That is what extensions are for.
- they should not depend on injected fields (as those may still be null)
- they should be in their own dedicated extension (cf. `DefaultServicesExtension`)
- do not `@Inject` and `@Provider` the same service in one extension
- rule of thumb: unless you know exactly what you're doing and why you need them - don't use them!