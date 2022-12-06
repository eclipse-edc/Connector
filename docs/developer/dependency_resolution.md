# Dependency resolution in the EDC

The code base of the Eclipse Dataspace Connector is architected in away that allows for easily extending and swapping
the core functionality using certain plug-points called _extensions_. One example would be to swap out an in-memory
implementation for a datastore for one backed by an actual database. In order to achieve that there are several key
components:

- a service interface, typically located in an SPI module
- a module providing the implementation, typically located in the `extensions` directory
- the service registry, i.e. the `ServiceExtensionContext`. Since it is not quite an IoC container, we'll henceforth
  refer to it as the "context".
- a hook point into the loading sequence: an extension that instantiates and registers the implementation class with the
  context

## Registering a service implementation

As a general rule the module that provides the implementation also should register it with the `ServiceExtensionContext`
. This is done in an accompanying service extension. For example, providing a CosmosDB based implementation for
a `FooStore` (stores `Foo` objects) would require the following classes:

1. A `FooStore.java` interface, located in SPI:
    ```java
    public interface FooService {
        void store(Foo foo);
    }   
    ```
2. A `CosmosFooStore.java` class implementing the interface, located in `:extensions:azure:cosmos:foo-store-cosmos`:
    ```java
    public class CosmosFooStore implements FooStore {
        @Override
        void store(Foo foo){
            // ...
        }    
    }
    ```
3. A `CosmosFooStoreExtension.java` located also in `:extensions:azure:cosmos:foo-store-cosmos`. Must be accompanied by
   a _"provider-configuration file"_ as required by
   the [`ServiceLoader` documentation](https://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html). Code
   examples will follow below.

### Option 1: use `@Provider` methods (recommended)

Every `ServiceExtension` may declare methods that are annotated with `@Provider`, which tells the dependency resolution
mechanism, that this method contributes a dependency into the context. This is very similar to other DI containers, e.g.
Spring's `@Bean`
annotation. It looks like this:

```java
public class CosmosFooStoreExtension implements ServiceExtension {

    @Override
    public void initialize(ServiceExtensionContext context) {
        // ...
    }

    //Example 1: no args
    @Provider
    public SomeService provideSomeService() {
        return new SomeServiceImpl();
    }

    //Example 2: using context
    @Provider
    public FooStore provideFooStore(ServiceExtensionContext context) {
        var setting = context.getSetting("...", null);
        return new CosmosFooStore(setting);
    }
}
```

As the previous code snipped shows, provider methods may have no args, or a single argument, which is
the `ServiceExtensionContext`. There are a few other restrictions too. Violating these will raise an exception. Provider
methods must:

- be public
- return a value (`void` is not allowed)
- either have no arguments, or a single `ServiceExtensionContext`.

Having a provider method is equivalent to invoking `context.registerService(SomeService, new SomeServiceImpl())`. Thus,
the return type of the method defines the service `type`, whatever is returned by the provider method determines the
implementation of the service.

**Caution**: there is a slight difference between declaring `@Provider` methods and
calling `service.registerService(...)` with respect to sequence: the DI loader mechanism _first_
invokes `ServiceExtension#initialize()`, and
_then_ invokes all provider methods. In most situations this difference is negligible, but there could be situations,
where this matters.

#### Provide "defaults"

Where `@Provider` methods really come into their own is when providing default implementations. This means we can have a
fallback implementation. For example, going back to our `FooStore` example, there could be an extension that provides a
default (=in-mem)
implementation:

```java
public class DefaultsExtension implements ServiceExtension {

    @Override
    public void initialize(ServiceExtensionContext context) {
        // ...
    }

    @Provider(isDefault = true)
    public FooStore provideDefaultFooStore() {
        return new InMemoryFooStore();
    }
}
```

Provider methods configured with `isDefault=true` are only invoked, if the respective service (here: `FooStore`) is not
provided by any other extension.

> Default provider methods are a tricky topic, please be sure to thoroughly read the additional documentation about
> them [here](default_provider_methods.md)!

### Option 2: register manually

Of course, it is also possible to manually register services by invoking the respective method on
the `ServiceExtensionContext`

```java

@Provides(FooStore.class/*, possibly others*/)
public class CosmosFooStoreExtension implements ServiceExtension {

    @Override
    public void initialize(ServiceExtensionContext context) {
        var setting = context.getSetting("...", null);
        var store = new CosmosFooStore(setting);
        context.registerService(FooStore.class, store);
    }
}
```

There are three important things to mention:

1. the call to `context#registerService` makes the object available in the context. From this point on other extensions
   can inject a `FooStore` (and in doing so will receive a `CosmosFooStore`).
2. declaring the exposed interface in the `@Provides()` annotation is required, as it helps the extension loader define
   the order in which it needs to initialize extensions
3. service registrations **must** be done in the `initialize()` method.

## Injecting a service

Services should only be referenced by the interface they implement. This will keep dependencies clean and maintain
extensibility and modularity. Say we have a `FooMaintenanceService` that receives `Foo` objects over an arbitrary
network channel and stores them.

### Option 1: use `@Inject` to declare dependencies (recommended)

```java
public class FooMaintenanceService {
    private final FooStore fooStore;

    public FooMaintenanceService(FooStore fooStore) {
        this.fooStore = fooStore;
    }
}
```

Note that the example uses what we call _constructor injection_ (even though nothing is actually _injected_), because
that is needed for object construction, and it increases testability. Also, those types of class fields should be
declared `final` to avoid programming errors.

In contrast to conventional DI frameworks the `fooStore` dependency won't get auto-injected - rather, there has to be
another `ServiceExtension` that has a reference to the `FooStore` and that constructs the `FooMaintenanceService`:

```java
public class FooMaintenanceExtension implements ServiceExtension {
    @Inject
    private FooStore fooStore;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var service = new FooMaintenanceService(fooStore); //use the injected field
    }
}
```

The `@Inject` annotation on the `fooStore` field tells the extension loading mechanism that `FooMaintenanceExtension`
depends on a `FooService` and because of that, any provider of a `FooStore` must be initialized _before_
the `FooMaintenanceExtension`. The fact that `CosmosFooStoreExtension` provides a `FooStore` is declared using
the `@Provides` annotation.

### Option 2: use `@Requires` to declare dependencies

In cases where defining a field seems unwieldy or is simply not desirable, we provide another way to dynamically resolve
service from the context:

```java

@Requires({ FooService.class, /*maybe others*/ })
public class FooMaintenanceExtension implements ServiceExtension {

    @Override
    public void initialize(ServiceExtensionContext context) {
        var fooStore = context.getService(FooStore.class);
        var service = new FooMaintenanceService(fooStore); //use the resolved object
    }
}
```

The important issue to mention is that `@Requires` is absolutely necessary to inform the service loader about the
dependency. Failing to add it this may potentially result in exceptions, and in further consequence, in
an `EdcInjectionException`.

Option 1 and 2 are almost semantically equivalent, with the small exception of optional dependencies:
while `@Inject(required=false)` allows for nullable dependencies, `@Requires` has no such option and the service
dependency must be resolved explicitly using a boolean parameter `context.getService(FooStore.class, true)`.

## Extension initialization sequence

The extension loading mechanism uses a two-pass procedure to resolve dependencies. First, all implementors
of `ServiceExtension` are instantiated using their public default constructor, put in a list and sorted using a
topological sort algorithm based on their dependency graph. Cyclic dependencies would be reported in this stage.

Second, the extension is initialized by setting all fields annotated with `@Inject` and by calling its `initialize()`
method. This implies that every extension can assume that by the time its `initialize()` method executes, all its
dependencies are already instantiated and registered, because the extension(s) providing them were ordered at previous
positions in the list, and thus have already been initialized.

## Tests for classes using injection

To test classes using the `@Inject` annotation, use the appropriate JUnit extension:

- If only basic dependency injection is needed (unit testing), use the `DependencyInjectionExtension`.
- If the full EDC runtime should be run (integration testing), use the `EdcExtension`.

## Limitations and differences to fully-fledged IoC containers

#### Only available in `ServiceExtensions`

Services can only be injected into `ServiceExtension` objects at this time as they are the main hook points for plugins,
and they have a clearly defined interface. All subsequent object creation must be done manually using conventional
mechanisms like constructors or builders.

#### No multiple registrations

Registering two implementations for an interface will result in the first registration being overwritten by the second
registration. If both providers have the same topological ordering it is undefined whichever comes first. A warning is
posted to the `Monitor`.

_It was a conscientious architectural decision to forego multiple service registrations for the sake of simplicity and
clean design. Patterns like composites or delegators exist for those rare cases where having multiple implementors of
the same interface is indeed needed. Those should be used sparingly and not without a strong reason._

#### No collection-based injection

Because there can be only ever one implementation for a service, it is not possible to inject a collection of
implementors as it would be in other DI frameworks.

#### Only field injection

At the moment the `@Inject` annotation can only target fields, meaning, that we cannot perform constructor or setter
injection with it, for example `public SomeExtension(@Inject SomeService someService){ ... }` would not be possible.

#### No named dependencies

Dependencies cannot be decorated with an identifier, which would technically allow for multiple service registrations (
using different names). Technically this is linked to the limitation of single service registrations.

#### Direct inheritors/implementors only

This is not due to a limitation of the dependency injection mechanism, but rather due to the way how the context
maintains service registrations: it simply maintains a `Map` containing interface class and implementation type.

#### Cyclic dependencies

Cyclic dependencies are detected by the `TopologicalSort` algorithm, but the error reporting is a bit limited.

#### No generic dependencies

It's not possible to have dependencies with a type parameter.
