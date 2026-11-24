# Conditional dependency resolution

## Decision

EDC will support activating extensions conditionally, based on an explicit configuration value, the "enabling switch".
An extension that declares such a condition is only loaded if the switch has the expected value. An extension that is
not activated behaves as if it were not on the classpath.

## Rationale

In order to publish usable all-in-one distributions of EDC components, those distributions must contain all major
features available in EDC today. These are most notably SQL persistence and the task-based state machine, which uses
NATS for message passing.

The distribution (i.e. a Docker image) would therefore have all those Java modules on the classpath, but only activate
those selected by configuration. The capabilities of a runtime would then be dictated by configuration rather than by
the presence of modules on the classpath.

This is not possible today: an extension that is on the classpath is always loaded. Features are selected at build time
by composing BOMs, in some cases with explicit excludes. For example, the polling-based virtual control plane excludes
the task modules and adds the state machine managers instead. Every combination of features therefore requires its own
distribution.

## Approach

### The enabling switch

A new class-level annotation is added to the runtime metamodel:

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ConditionalOnSetting {
    String key();

    String havingValue();

    boolean matchIfMissing() default false;
}
```

It is placed on the extension class:

```java
@ConditionalOnSetting(key = "edc.persistence", havingValue = "postgres")
@Extension(value = "SQL asset index")
public class SqlAssetIndexServiceExtension implements ServiceExtension {
    // ...
}
```

The following rules apply:

- An extension without the annotation is always active, as it is today.
- An annotated extension is active if the configuration contains `key` and its value is exactly `havingValue`.
- If `key` is not configured, the extension is only active if `matchIfMissing` is `true`. This is how the default of a
  switch is expressed.
- If a key that at least one extension on the classpath uses as a switch is configured with a value that none of them
  declares, the runtime fails to start. This catches misspelled values, which would otherwise silently select the
  default.
- An extension declares at most one condition.

The switches are evaluated once, at boot time, against the runtime configuration (configuration extensions,
environment variables and system properties), which is loaded before the extensions. Values that are stored in the
vault or in the configuration of a participant context are not considered.

### Effect on an inactive extension

Inactive extensions are removed before the dependency graph is built. As a consequence, an inactive extension:

- is not injected and does not receive any lifecycle callback (`initialize`, `prepare`, `start`, `shutdown`,
  `cleanup`),
- does not register any service, neither through `@Provider` methods nor through `@Provides`,
- does not have its `@Setting` and `@Configuration` fields evaluated, so missing configuration for a deactivated
  feature is not an error.

Services that an inactive extension would have provided are resolved from the remaining extensions or from a default
provider. If neither exists, the runtime reports an unsatisfied dependency, as it does today.

Extension classes are still instantiated by the `ServiceLoader`, so their constructors and static initializers must
be free of side effects.

An active extension validates its configuration exactly as it does today. There is no fallback if the selected feature
is misconfigured or if its infrastructure is not reachable. For example, `edc.statemachine=task` with an unreachable
NATS server fails at startup, it does not fall back to polling.

On startup, the runtime logs the value of every switch and the extensions that were deactivated by it.

### Switches

Two switches are introduced initially:

| Key                | Value      | Activates                                                                   | Active if the key is not set |
|--------------------|------------|-----------------------------------------------------------------------------|------------------------------|
| `edc.persistence`  | `postgres` | the SQL store extensions and the SQL infrastructure extensions              | no                           |
| `edc.statemachine` | `polling`  | `TransferManagerExtension`, `ContractManagerExtension`                      | yes                          |
| `edc.statemachine` | `task`     | the task core, the task executors, the NATS task publishers and subscribers | no                           |

The SQL infrastructure extensions are the ones that only serve the SQL stores: SQL core, schema bootstrapper, lease and
connection pool.

`edc.persistence` has no value for the in-memory stores: they are default providers, so they are used whenever no
SQL extension is active.

`edc.statemachine` is a selector rather than an on/off switch, because disabling the task-based state machine is only
useful if the polling-based one takes over, and vice versa. It is named after the mechanism rather than after NATS,
because NATS is only the transport of the task-based state machine.

Note that:

- `SqlTaskStoreExtension` is only bound to `edc.persistence`. With `edc.persistence=postgres` and
  `edc.statemachine=polling` it is active but unused.
- `NatsNkeyAuthExtension` remains unconditional. It has no effect unless `edc.nats.auth.nkey.seed.path` is configured.
- Publishing events to NATS (`NatsPublishingExtension`) is independent of the state machine and is not covered by
  `edc.statemachine`. Should it become part of a distribution, it receives its own switch.

### Compatibility

Today, the presence of a module on the classpath activates it. Once the extensions listed above are annotated, this is
no longer true for them, which is a breaking change:

- A runtime that contains the SQL modules must set `edc.persistence=postgres`. Without it, the runtime starts with
  in-memory stores and does not report an error, because every store has an in-memory default.
- A runtime that uses the task-based state machine must set `edc.statemachine=task`. Without it, the task extensions
  are inactive. If the polling-based managers are not on the classpath either, the runtime starts, but no state machine
  is processed.

This also applies to custom-built runtimes and to test runtimes, because the condition is declared on the extension and
not on the distribution. In particular, a test that is meant to run against Postgres but does not set
`edc.persistence=postgres` runs against the in-memory stores and may still pass. Extensions that do not carry the
annotation, including all downstream extensions, are not affected.

In order to make this backwards compatible, the default value of the `@ConditionalOnSetting` annotation must reflect the
current behaviour, so `postgres` for `edc.persistence` and `task` for `edc.statemachine`. That way, it's more of an
explicit _deactivation_ rather than _activation_.

### Affected components

- Runtime-Metamodel: the `@ConditionalOnSetting` annotation.
- Connector: evaluation of the conditions in the `boot` module, annotation of the extensions listed above, and the
  all-in-one BOMs.
- GradlePlugins: the autodoc processor should include the condition of an extension in the generated manifest.

### Alternatives considered

#### Implicit activation, based on the presence of configuration

An extension would be loaded if a full and valid configuration for it exists. For example, the SQL stores would only be
loaded if a datasource is configured, otherwise the in-memory stores would be used.

**Pro:** no additional switch is required, providing the configuration enables the feature. Backwards compatible.

**Con:** the presence of configuration cannot be determined reliably. The datasource URL and credentials are preferably
stored in the vault, which is not available when the dependency graph is built, and many settings, such as the NATS
URLs, have default values and are therefore never absent. The runtime would also have to distinguish configuration
that is absent (do not load the feature) from configuration that is incomplete (fail at startup). Finally, a feature
could not be disabled temporarily without removing its configuration. Furthermore, the logic to determine whether to
load a feature or not is likely duplicated across several extensions.

#### Explicit activation on `@Provider` methods

The enabling switch would be declared on individual provider methods, for example
`@Provider(enabledIf = "edc.persistence", value = "postgres")`.

**Pro:** fine-grained, no new annotation is required.

**Con:** not every extension contributes its features through provider methods. Several SQL extensions register their
stores in `initialize()` and declare them with `@Provides`. The NATS task publishers and subscribers provide no service
at all: they open connections and register listeners in their lifecycle callbacks. A deactivated provider method would
leave all of this running, and the settings of the extension would still be validated.

Conditions on provider methods can be added later if a need for finer granularity arises.
