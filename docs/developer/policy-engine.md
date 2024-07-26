# Policy Engine

## Register policy functions

The following will document how to register a constraint function to the policy engine.

### Step 1: Create an extension

As an example, we create the module `:extensions:policy:ids-policy`. 

1. Add a `build.gradle.kts` file:

    ```
    val rsApi: String by project
    
    plugins {
        `java-library`
    }
    
    dependencies {
        api(project(":spi"))
    }
    ```
   
2. Specify the `IdsPolicyExtension` in the `resources/META-INF/services` directory:

    ```
   org.eclipse.edc.protocol.ids.policy.IdsPolicyExtension
   ```

3. To ensure that the extension will be loaded, add it to the `settings.gradle.kts` at the root of the project:

    ```
    include(":extensions:policy:ids-policy")
    ```

### Step 2: Implement a `ServiceExtension`

The new policy extension will provide a service extension class and two constraint functions. In
`IdsPolicyExtension`, first, the scope `ALL_SCOPES` is bound to the rule type `use`. Next, both policy 
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
        ruleBindingRegistry.bind("use", ALL_SCOPES);

        policyEngine.registerFunction(ALL_SCOPES, Permission.class, ABS_SPATIAL_POSITION, new AbsSpatialPositionConstraintFunction());
        policyEngine.registerFunction(ALL_SCOPES, Permission.class, PARTNER_LEVEL, new PartnerLevelConstraintFunction());
    }
}
```

In this example, the functions are registered for all policy scopes, meaning they will be used for every policy
evaluation. Details on different policy scopes can be found [here](#policy-scopes).

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

### Policy scopes

By binding a function to a specific scope instead of all scopes, the function will only be included in evaluations for
that scope. Currently, the EDC core provides 3 different policy scopes, which are explained in the following.

#### Cataloging scope: `contract.cataloging`

This scope is used when contract offers are generated from contract definitions. Here, each contract definition's access
policy is evaluated to decide which contract definitions may be used to generate offers for the requesting agent.

#### Negotiation scope: `contract.negotiation`

This scope is used during the contract negotiation. The policies from each contract offer and agreement exchanged during
the negotiation are evaluated with this scope. This scope is also used to re-evaluate a contract agreement's policy
before a data transfer is initiated.

#### Manifest verification scope: `provision.manifest.verify`

This scope is used during the provisioning phase to evaluate the resource definitions of a generated resource manifest.
Functions registered in this scope may also modify resource definitions so that they comply with the policy.
Therefore, a `ResourceManifestContext`, which provides access to a manifest's resource definitions, is available
through the `PolicyContext` for functions registered in this scope. Using the `ResourceManifestContext`, resource
definitions can be retrieved and updated by type.

```java
@Override
public boolean evaluate(Operator operator, Object rightValue, Permission rule, PolicyContext context) {
    var manifestContext = context.getContextData(ResourceManifestContext.class);

    var bucketDefinitions = manifestContext.getDefinitions(S3BucketResourceDefinition.class);

    // verify and/or modify definitions to comply with policy
        
     manifestContext.replaceDefinitions(S3BucketResourceDefinition.class, verifiedBucketDefinitions);
     return true;
}
```

If any of the resource definitions cannot be modified to comply with the policy, the function should return `false`.
