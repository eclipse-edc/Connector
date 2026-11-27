# Conditional dependency resolution

## Decision

EDC will support loading extensions conditionally. An extension can be bound to a _feature_, which is represented by a
configuration key and a value. Such an extension is only loaded if the feature key is configured with that value. An
extension that is not loaded behaves as if it were not on the classpath.

## Rationale

In order to publish usable all-in-one distributions of EDC components, those distributions must contain all major
features available in EDC today. These are most notably SQL persistence, the HashiCorp Vault and the task-based state
machine, which uses NATS for message passing.

The distribution (i.e. a Docker image) would therefore have all those Java modules on the classpath, but only load
those selected by configuration. The capabilities of a runtime would then be dictated by configuration rather than by
the presence of modules on the classpath.

This is not possible today: an extension that is on the classpath is always loaded. Features are selected at build time
by composing BOMs, in some cases with explicit excludes. For example, the polling-based virtual control plane excludes
the task modules and adds the state machine managers instead. Every combination of features therefore requires its own
distribution.

## Approach

### Binding an extension to a feature

A feature is a capability of the runtime for which several implementations may be on the classpath, for example the
persistence, the state machine or the vault. It is represented by a configuration key, the _feature key_. All feature
keys share the prefix `edc.feature`, so that they are kept in a single place, e.g. `edc.feature.persistence`.

The `@Extension` annotation of the runtime metamodel receives three new attributes:

```java
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Extension {
    String value();

    String[] categories() default "";

    String featureKey() default "";

    String featureValue() default "";

    boolean loadAsDefault() default false;
}
```

An extension is bound to a feature by setting `featureKey` and `featureValue`:

```java
@Extension(value = "SQL asset index", featureKey = "edc.feature.persistence", featureValue = "postgres")
public class SqlAssetIndexServiceExtension implements ServiceExtension {
    // ...
}
```

The following rules apply:

- An extension that is not bound to a feature is always loaded, as it is today.
- An extension that is bound to a feature is loaded if the configuration contains the feature key and its value is
  exactly `featureValue`.
- If the feature key is not configured, the extension is only loaded if `loadAsDefault` is `true`. This is how the
  default of a feature is expressed.
- If a feature key that at least one extension on the classpath is bound to is configured with a value that none of
  them declares, the runtime fails to start. This catches misspelled values, which would otherwise silently select the
  default.
- An extension is bound to at most one feature.

The feature keys are evaluated once, at boot time, against the runtime configuration (configuration extensions,
environment variables and system properties), which is loaded before the extensions. Values that are stored in the
vault or in the configuration of a participant context are not considered.

### Effect on an extension that is not loaded

Extensions that are not loaded are removed before the dependency graph is built. As a consequence, such an extension:

- is not injected and does not receive any lifecycle callback (`initialize`, `prepare`, `start`, `shutdown`,
  `cleanup`),
- does not register any service, neither through `@Provider` methods nor through `@Provides`,
- does not have its `@Setting` and `@Configuration` fields evaluated, so missing configuration for a feature that is
  not loaded is not an error.

Services that it would have provided are resolved from the remaining extensions or from a default provider. If neither
exists, the runtime reports an unsatisfied dependency, as it does today.

Extension classes are still discovered and instantiated by the `ServiceLoader`, so their constructors and static
initializers must be free of side effects.

An extension that is loaded validates its configuration exactly as it does today. There is no fallback if the selected
feature is misconfigured or if its infrastructure is not reachable. For example, `edc.feature.statemachine=task` with an
unreachable NATS server fails at startup, it does not fall back to polling.

On startup, the runtime logs the value of every feature key and the extensions that were not loaded because of it.

### Initial features

The following features serve as example here:

| Feature key                | Feature value | Extensions                                                                  | `loadAsDefault` |
|----------------------------|---------------|-----------------------------------------------------------------------------|-----------------|
| `edc.feature.persistence`  | `postgres`    | the SQL store extensions and the SQL infrastructure extensions              | `false`         |
| `edc.feature.vault`        | `hashicorp`   | the extensions of the `vault-hashicorp` module                              | `false`         |
| `edc.feature.statemachine` | `polling`     | `TransferManagerExtension`, `ContractManagerExtension`                      | `true`          |
| `edc.feature.statemachine` | `task`        | the task core, the task executors, the NATS task publishers and subscribers | `false`         |
| `edc.feature.events`       | `nats`        | `NatsPublishingExtension`                                                   | `false`         |
| `edc.feature.nats.auth`    | `nkey`        | `NatsNkeyAuthExtension`                                                     | `false`         |

_this list may not be exhaustive, more features may land._

The SQL infrastructure extensions are the ones that only serve the SQL stores: SQL core, schema bootstrapper, lease and
connection pool.

`edc.feature.persistence` and `edc.feature.vault` have no value for the in-memory implementations: the in-memory stores
and the in-memory vault are default providers, so they are used whenever no SQL extension, or no HashiCorp Vault
extension respectively, is loaded. The vault is a feature of its own, but a runtime that uses the in-memory stores
will typically use the in-memory vault as well.

For `edc.feature.statemachine`, not loading the task-based state machine is only useful if the polling-based one takes
over, and vice versa. The feature is named after the mechanism rather than after NATS, because NATS is only the
transport of the task-based state machine.

Note that:

- `SqlTaskStoreExtension` is only bound to `edc.feature.persistence`. With `edc.feature.persistence=postgres` and
  `edc.feature.statemachine=polling` it is loaded but unused.
- The HashiCorp Vault extensions are the only provider of the `SignatureService`. Without them, the signature-backed
  STS is not registered.
- `NatsNkeyAuthExtension` contributes the credentials for all NATS connections, those of the task-based state machine
  as well as those of the event publishing. It is therefore bound to neither of their features, but to a feature of
  its own, `edc.feature.nats.auth`. Once it is loaded, `edc.nats.auth.nkey.seed.path` is mandatory: a missing seed
  path fails the startup, instead of connecting without credentials.
- Publishing events to NATS (`NatsPublishingExtension`) is independent of the state machine and is therefore not
  covered by `edc.feature.statemachine`. It is a feature of its own, `edc.feature.events`, so that either of the two
  can be used without the other. Event publishers that are not bound to a feature are not affected by it.

### Compatibility

Today, every extension on the classpath is loaded. Once the extensions listed above are bound to a feature, this is no
longer true for them, which is a breaking change:

- A runtime that contains the SQL modules must set `edc.feature.persistence=postgres`. Without it, the runtime starts
  with in-memory stores and does not report an error, because every store has an in-memory default.
- A runtime that contains the HashiCorp Vault module must set `edc.feature.vault=hashicorp`. Without it, the runtime
  starts with the in-memory vault and only logs the existing warning about it.
- A runtime that uses the task-based state machine must set `edc.feature.statemachine=task`. Without it, the task
  extensions are not loaded. If the polling-based managers are not on the classpath either, the runtime starts, but no
  state machine is processed.
- A runtime that publishes events to NATS must set `edc.feature.events=nats`. Without it, the runtime starts, but no
  events are published to NATS.
- A runtime that authenticates its NATS connections with an NKey must set `edc.feature.nats.auth=nkey`. Without it,
  the connections are made without credentials and are refused by a NATS server that requires authentication.

This also applies to custom-built runtimes and to test runtimes, because the feature is declared on the extension and
not on the distribution. In particular, a test that is meant to run against Postgres but does not set
`edc.feature.persistence=postgres` runs against the in-memory stores and may still pass. Extensions that are not bound
to a feature, including all downstream extensions, are not affected.

The `loadAsDefault` attribute cannot make these bindings backwards compatible:

- `edc.feature.persistence`, `edc.feature.vault`, `edc.feature.events` and `edc.feature.nats.auth` only have a single
  value, because their alternative requires no extension: the in-memory stores and the in-memory vault are default
  providers, events are simply not published to NATS, and NATS connections are made without credentials. With
  `loadAsDefault=true` the extensions bound to these features could not be opted out of, which is required for an
  all-in-one distribution.
- `edc.feature.statemachine` has two values, but only one of them can be the default: if both were loaded by default,
  an all-in-one distribution that does not configure the feature key would run both state machines. The default is
  `polling`, because it does not require additional infrastructure. On a runtime that contains the polling-based
  managers, leaving the feature key out is therefore the same as setting it to `polling`. On a runtime that only
  contains the task-based state machine, leaving it out loads no state machine, and setting it to `polling` fails the
  startup, because no extension on the classpath declares that value.

The values of `loadAsDefault` are therefore the ones listed in the table above.

### Affected components

- Runtime-Metamodel: the new attributes of the `@Extension` annotation, and the autodoc processor, which should
  include the feature of an extension in the generated manifest.
- Connector: evaluation of the features in the `boot` module, binding of the extensions listed above, and the
  all-in-one BOMs.

### Follow-up

The `boot` module is a foundational module that should not depend on connector-specific code, so it could be moved to
the Runtime-Metamodel repository. Today it depends on `core-spi`, `control-plane-spi` and `core-lib`, which would have
to be resolved first. This is not required for this decision and will be described in a separate issue.

### Alternatives considered

#### Implicit loading, based on the presence of configuration

An extension would be loaded if a full and valid configuration for it exists. For example, the SQL stores would only be
loaded if a datasource is configured, otherwise the in-memory stores would be used.

**Pro:** no feature key is required, providing the configuration loads the feature. Backwards compatible.

**Con:** the presence of configuration cannot be determined reliably. The datasource URL and credentials are preferably
stored in the vault, which is not available when the dependency graph is built, and many settings, such as the NATS
URLs, have default values and are therefore never absent. The runtime would also have to distinguish configuration
that is absent (do not load the feature) from configuration that is incomplete (fail at startup). Finally, a feature
could not be disabled temporarily without removing its configuration. Furthermore, the logic to determine whether to
load a feature or not is likely duplicated across several extensions.

#### Binding `@Provider` methods to a feature

The feature would be declared on individual provider methods, for example
`@Provider(featureKey = "edc.feature.persistence", featureValue = "postgres")`.

**Pro:** fine-grained.

**Con:** not every extension contributes its features through provider methods. Several SQL extensions register their
stores in `initialize()` and declare them with `@Provides`. The NATS task publishers and subscribers provide no service
at all: they open connections and register listeners in their lifecycle callbacks. Skipping a provider method would
leave all of this running, and the settings of the extension would still be validated.

Feature bindings on provider methods can be added later if a need for finer granularity arises.
