# Advanced dependency resolution: default provider methods

This feature (and this document) is aimed at platform developers who intend to provide additional platform features. It
is **not** intended for people who simply want to contribute a technology extension, such as a database implementation
or a storage backend!

In this document we will use the term "default provider" and "default provider method" synonymously to refer to a method
annotated with `@Provider(isDefault=true)`. Similarly, "provider", "provider method" or "factory method"
refer to methods annotated with just `@Provider`.

## Fallbacks versus extensibility

Default provider methods are intended to provide fallback implementations for services rather than to achieve
extensibility - that is what extensions are for. There is a subtle but important semantic difference between _fallback
implementations_ and _extensibility_:

### Fallback implementations

Fallbacks are meant as safety net, in case developers forget or don't want to add a specific implementation for a
service. It is there so as not to end up _without_ an implementation for a service interface. A good example for this
are in-memory store implementations. It is expected that an actual persistence implementation is contributed by another
extension. In-mem stores get you up and running quickly, but we wouldn't recommend using them in production
environments. Typically, fallbacks should not have any dependencies onto other services.

> Default-provided services, even though they are on the classpath, only get instantiated if there is no other
> implementation.

### Extensibility

In contrast, _extensibility_ refers to the possibility of swapping out one implementation of a service for another by
choosing the respective module at compile time. Each implementation must therefore be contained in its own java module,
and the choice between one or the other is made by referencing one or the other in the build file. The service
implementation is typically instantiated and provided by its own extension. In this case, the `@Provider`-annotation **
must not** have the `isDefault` attribute. This is also the case if there will likely only ever be one implementation
for a service.

One example for extensibility is the `IdentityService`: there could be several implementations for it (OAuth,
DecentralizedIdentity, Keycloak etc.), but providing either one as default would make little sense, because all of them
require external services to work. Each implementation would be in its own module and get instantiated by its own
extension.

> Provided services get instantiated only if they are on the classpath, but always get instantiated.

## Deep-dive into extension lifecycle management

Generally speaking every extension goes through these lifecycle stages during loading:

- `inject`: all fields annotated with `@Inject` are resolved
- `initialize`: the `initialize()` method is invoked. All required collaborators are expected to be resolved after this.
- `provide`: all `@Provider` methods are invoked, the object they return is registered in the context.

Due to the fact that default provider methods act a safety net, they only get invoked if no other provider method offers
the same service type. However, what may be a bit misleading is the fact that they typically get invoked _during the
`inject` phase_. The following section will demonstrate this.

### Example 1 - provider method

Recall that `@Provider` methods get invoked regardless, and after the `initialze` phase. That means, assuming both
extensions are on the classpath, the extension that declares the provider method (= `ExtensionA`) will get fully
instantiated before another extension (= `ExtensionB`) can use the provided object:

```java
public class ExtensionA { // gets loaded first
    @Inject
    private SomeStore store; // provided by some other extension

    @Provider
    public SomeService getSomeService() {
        return new SomeServiceImpl(store);
    }
}

public class ExtensionB { // gets loaded second
    @Inject
    private SomeService service;
}
```

After building the dependency graph, the loader mechanism would first fully construct `ExtensionA`, i.e.
`getSomeService()` is invoked, and the instance of `SomeServiceImpl` is registered in the context. Note that this is
done regardless whether another extension _actually injects a `SomeService`_. After that, `ExtensionB` gets constructed,
and by the time it goes through its `inject` phase, the injected `SomeService` is already in the context, so the
`SomeService` field gets resolved properly.

### Example 2 - default provider method

Methods annotated with `@Provider(isDefault=true)` only get invoked if there is no other provider method for that
service, and at the time when the corresponding `@Inject` is resolved. Modifying example 1 slightly we get:

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
invoked _when the `@Inject` dependency is resolved_, because that is the "latest" point in time that that decision can
be made. That means, they get invoked during `ExtensionB`'s inject phase, and _not_ during `ExtensionA`'s provide phase.
There is no guarantee that `ExtensionA` is already initialized by that time, because the extension loader does not know
whether it needs to invoke `getSomeService` at all, until the very last moment, i.e. when resolving `ExtensionB`'s
`service` field. By that time, the dependency graph is already built.

Consequently, default provider methods could (and likely would) get invoked before the defining extension's `provide`
phase has completed. They even could get invoked before the `initialize` phase has completed: consider the following
situation the previous example:

1. all implementors of `ServiceExtension` get constructed by the Java `ServiceLoader`
2. `ExtensionB` gets loaded, runs through its inject phase
3. no provider for `SomeService`, thus the default provider kicks in
4. `ExtensionA.getSomeService()` is invoked, but `ExtensionA` is not yet loaded -> `store` is null
5. -> potential NPE

Because there is no explicit ordering in how the `@Inject` fields are resolved, the order may depend on several factors,
like the Java version or specific JVM used, the classloader and/or implementation of reflection used, etc.

## Usage guidelines when using default providers

From the previous sections and the examples demonstrated above we can derive a few important guidelines:

- do not use them to achieve extensibility. That is what extensions are for.
- use them only to provide a _fallback implementation_
- they should not depend on other injected fields (as those may still be null)
- they should be in their own dedicated extension (cf. `DefaultServicesExtension`) and Java module
- do not provide and inject the same service in one extension
- rule of thumb: unless you know exactly what you're doing and why you need them - don't use them!