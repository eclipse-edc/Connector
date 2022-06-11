# Shared clock

## Decision

A mockable shared `Clock` is used across EDC modules  to get the time.

Other mechanisms for accessing the time, such as calls to `Instant.now()`, `new Date()`, or `System.currentTimeMillis()` shall not be used in production code. They may be used in test code.

## Rationale

This allows writing consistent test code with assertions about time behavior.

## Approach

The shared clock can be injected in service extensions:

```java
@Inject
private Clock clock;
```

or with a convenience method:

```java
@Override
public void initialize(ServiceExtensionContext context) {
  var clock = context.getClock();
  ...
}
```

By default, this provides the system clock. In integration tests, another `Clock` implementation can be registered through mocking:

```java
@ExtendWith(EdcExtension.class)
class AnIntegrationTest {
  @BeforeEach
  void setUp(EdcExtension extension) {
    extension.registerServiceMock(Clock.class, Clock.fixed(now, UTC));
  }
}
```

