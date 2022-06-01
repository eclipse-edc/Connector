# Policy Engine

## Register policy functions

The following will document how to register a constraint function to the policy engine.

### Step 1: Create an extension

As an example, we create the module `:extensions:policy:ids-policy`. 

1. Add a `build.gradle.kts` file:

    ```
    val infoModelVersion: String by project
    val rsApi: String by project
    
    plugins {
        `java-library`
    }
    
    dependencies {
        api(project(":spi"))
        implementation(project(":data-protocols:ids:ids-spi"))
    }
    ```
   
2. Specify the `IdsPolicyExtension` in the `resources/META-INF/services` directory:

    ```
   org.eclipse.dataspaceconnector.ids.policy.IdsPolicyExtension
   ```

4. To ensure that the extension will be loaded, add it to the `settings.gradle.kts` at the root of the project:

    ```
    include(":extensions:policy:ids-policy")
    ```

### Step 2: Implement a `ServiceExtension`

The new policy extension will provide a service extension class and two constraint functions. In
`IdsPolicyExtension`, first, the scope `ALL_SCOPES` is bound to the rule type `USE`. Next, both policy 
functions are registered to the policy engine.

```java
public class IdsPolicyExtension implements ServiceExtension {

    private static final String ABS_SPATIAL_POSITION = "ids:absoluteSpatialPosition";
    private static final String PARTNER_LEVEL = "ids:partnerLevel";

    @Inject
    private RuleBindingRegistry ruleBindingRegistry;

    @Inject
    private PolicyEngine policyEngine;

    @Override
    public String name() {
        return "IDS Policy";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        ruleBindingRegistry.bind(USE, ALL_SCOPES);

        policyEngine.registerFunction(ALL_SCOPES, Permission.class, ABS_SPATIAL_POSITION, new AbsSpatialPositionConstraintFunction());
        policyEngine.registerFunction(ALL_SCOPES, Permission.class, PARTNER_LEVEL, new PartnerLevelConstraintFunction());
    }
}
```

### Step 3: Implement an `AtomicConstraintFunction`

#### Absolute Spatial Position Constraint Function

```java
public class AbsSpatialPositionConstraintFunction implements AtomicConstraintFunction<Permission> {
    
    @Override
    public boolean evaluate(Operator operator, Object rightValue, Permission rule, PolicyContext context) {
        var region = context.getParticipantAgent().getClaims().get("region");
        switch (operator) {
            case EQ:
                return Objects.equals(region, rightValue);
            case NEQ:
                return !Objects.equals(region, rightValue);
            case IN:
                return ((Collection<?>) rightValue).contains(region);
            default:
                return false;
        }
    }
}
```

#### Partner Level Constraint Function

```java
public class PartnerLevelConstraintFunction implements AtomicConstraintFunction<Permission> {
    
    @Override
    public boolean evaluate(Operator operator, Object rightValue, Permission rule, PolicyContext context) {
        String partnerLevel = context.getParticipantAgent().getClaims().get("partnerLevel");
        switch (operator) {
            case EQ:
                return Objects.equals(partnerLevel, rightValue);
            case NEQ:
                return !Objects.equals(partnerLevel, rightValue);
            case IN:
                return ((Collection<?>) rightValue).contains(partnerLevel);
            default:
                return false;
        }
    }
}
```
