# Policies in EDC

Purpose of this guide is to give an overview of what policies are and how they work in EDC.
Policy model used internally is based on the [Open Digital Rights Language (ODRL)](https://www.w3.org/TR/odrl-model/) and going
through it would help get a more complete picture. Nevertheless, this guide can be followed without it and the sections below
provide a simplified overview of the main ODRL concepts.

## What are policies?

Conceptually, a policy is a non-empty collection of rules, which specify actions the consumer is _allowed_, _disallowed_
or _required_ to perform on an Asset, optionally refined by a set of constraints.

As indicated above, rules come in three types:
- **Permissions**, which specify actions the consumer is allowed to perform on an asset
    - _Example_: permission to use the asset, but only if the consumer is in the EU region
- **Prohibitions**, which specify actions the consumer is not allowed to perform on an asset
    - _Example_: prohibiting further distribution of the asset
- **Obligations** (a.k.a. **Duties**), which specify actions the consumer is required to perform
    - _Example_: obligation to delete the data after 30 days

It is possible to link these entities together, forming more complex rule chains. For example, a _Prohibition_ can be
extended with a set of _Obligations_, specifying actions that need to be performed in case the prohibition has been infringed.

For more details of the possible use cases, please consult the ODRL documentation.
Unless stated otherwise, all models mentioned below can be found in the `org.eclipse.dataspaceconnector.policy.model` package.

## Defining policies

Before we can define the policy itself, we first need to define one or more rules (permissions, prohibitions or obligations) that will make up the policy.
Using the examples given above, we can add the following:

```java
var readPermission = Permission.Builder.newInstance()
    .action(Action.Builder.newInstance().type("READ").build())
    .build();

var distributeProhibition = Prohibition.Builder.newInstance()
    .action(Action.Builder.newInstance().type("DISTRIBUTE").build())
    .build();
```

With the code above, we have defined a permission, allowing a read access to an asset, as well as a prohibition,
disallowing further distribution of an asset. Rules are envisioned to be reusable, so the ones define above can be associated
with any number of policies, and by extension, assets.

Next, we create the actual policy and attach it to the asset.

```java
var readNotDistributePolicy = Policy.Builder.newInstance()
    .id("use-all")
    .permission(readPermission)
    .prohibition(distributeProhibition)
    .target("test-document")
    .build();
```

Policies can be evaluated using an instance of the _PolicyEngine_ class. Simplest way is to use one provided by the _ServiceExtensionContext_.

During evaluation, verifiable claims and attributes sent by the other connector are also available, represented by an instance of the _ParticipantAgent_ class.
In this case, we will assume no claims or attributes have been sent. We will extend this in one of the subsequent examples.

```java
var policyEngine = serviceContext.getService(PolicyEngine.class);
var agent = new ParticipantAgent(Collections.emptyMap(), Collections.emptyMap());
var result = policyEngine.evaluate(usePolicy, agent);

result.isValid();
```

Result of the last expression is a boolean value specifying whether all rules have been satisfied.

## Constraints

While the rules above can be used to cover simple scenarios, things usually aren't so clear-cut. Perhaps we would like to
limit the use of an asset to consumer connectors located in EU, or introduce an embargo, preventing further asset distribution before a specific date.

In these cases, rules can be extended with one or more constraints, which further refine the semantics of a rule. A _Constraint_, in its simplest terms,
is a logical expression that can be evaluated on the provider and/or consumer side, and whose result (a boolean value) shows whether the refinement has been satisfied.

It comes in two flavors:

- **AtomicConstraint** - a binary boolean expression, in a `leftOperand operator rightOperand` form
    - _Example_: `"region" EQUALS "eu"`, `dateTime LESS_THAN "2021-12-31T00:00:00"`
- **MultiplicityConstraint** (and it's subclasses) - a collection of Constraints linked with logical operators
    - Concrete implementations include the And, Or and OnlyOne constraints
    - _Example_: `euConstraint AND goldPartnerConstraint AND notUnderEmbargoConstraint`

Similar to rules, these constraints can be nested, to form arbitrarily complex logical expressions.

Extending the example above, we can refine our read permission:
```java
var euConstraint = AtomicConstraint.Builder.newInstance()
    .leftExpression(new LiteralExpression("region"))
    .operator(EQ)
    .rightExpression(new LiteralExpression("eu"))
    .build();

var readPermission = Permission.Builder.newInstance()
    .action(Action.Builder.newInstance().type("READ").build())
    .constraint(euConstraint)   // This line is new
    .build();
```

Keep in mind that with the lines above we have just expressed an assertion that needs to be true, and not yet defined how the
evaluation of such assertion should proceed (e.g. where is the `region` value retrieved from?).
To do this, in the next section we will define our custom rule functions that will be invoked to process these constraints.

## Defining custom rule functions

If left as is, the expression `"region" EQ "eu"` will always evaluate to false, since the literals `"region"` and `"eu"` will
be compared by equality. What we actually wanted is for the value of the left operand, _region_, to be retrieved / generated from the provided input.
Such computed value should then be compared with the literal `"eu"` to produce the final result.

For this, we will define a rule function, that will be called every time a rule with `"region"` as the left operand needs to be evaluated.
This rule function needs to be available to the policy engine object which will be later used to evaluate this policy.

For the purpose of this example, we will extract the region of the consumer connector from the claims it provides:

```java
// Assuming we already have this from previous snippets
// var policyEngine = serviceContext.getService(PolicyEngine.class);

policyEngine.registerFunction(
  Permission.class, 
  "region", 
  (operator, rightOperand, permission, context) -> {
    var consumerRegion = context.getParticipantAgent().getClaims().get("region"); // #1
    switch (operator){
      case EQ:
        return Objects.equals(consumerRegion, rightOperand); 
      default:
        return false;
    }
  }
);
```

At **#1**, we are extracting the value from the verifiable claims provided by the other connector. In case of the embargo example,
we would be using the current datetime for the value of the left operand.

Putting it all together, we will again try to evaluate the policy, this time using a connector which has some verifiable claims.

```java
agent = new ParticipantAgent(Map.of("region", "eu"), Collections.emptyMap());
result = policyEngine.evaluate(usePolicy, agent);

result.isValid();
```

Once the policy evaluation reaches the `euConstraint`, left operand `"region"` will be resolved to value `"eu"` coming from the
provided verifiable claims, using the function above. This value will be compared against the right operand `"eu"`,
defined in the `euConstraint`, giving us a positive result.

## Custom policy models

While the ODRL policy model is one used by the EDC core, data protocols are free to introduce their own policy models.

This way custom data protocol can be used to exchange information between participating connectors,
while still leveraging the policy evaluation capabilities of the connector core.

In this section we'll take a look at how to define transformers between a custom policy model and the EDC one,
on the example of the IDS model. Full code of all relevant IDS transformers can be found in `data-protocols/ids/ids-transform-v1`.

To begin, we will need an instance of the _TransformerRegistry_ class, which acts as dispatcher, calling apropriate IDS-EDC or EDC-IDS type converters.
As with all services, it can be retrieved from the _ServiceExtensionContext_:

```java
var transformerRegistry = serviceContext.getService(TransformerRegistry.class);

var readPermission = Permission.Builder.newInstance()
  .action(Action.Builder.newInstance().type("idsc:READ").build())   // Updated
  .constraint(euConstraint)
  .build();

var transformResult = transformerRegistry.transform(readPermission, de.fraunhofer.iais.eis.Permission.class);
```

Note the change of type from `READ` to `idsc:READ` compared to our previous example. While the former is following the EDC model,
latter follows the IDC policy model.

`transformResult` will contain either a list of issues that appeared during the conversion, available through `transformResult.getProblems()` or
the actual result of the conversion, available through `transformResult.getOutput()`.






