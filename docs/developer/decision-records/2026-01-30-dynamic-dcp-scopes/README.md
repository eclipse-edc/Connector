# Adopt CEL Expressions for Policy Evaluation

## Decision

We will implement a dynamic scope resolution mechanism when using the Connector with the DCP protocol that allows scopes
to be defined and resolved at runtime based on the context of the request.

## Rationale

DCP scopes are of two types:

- Default scopes: predefined static scopes that are always included each DSP request.
- Policy scopes: scopes derived from the policy associated with a DSP request (catalog/negotiation/transfer).

Currently, both types require writing a custom extensions to define and resolve the scopes.

The first one requires to write a `PolicyValidatorRule` and register it in the `PolicyEngine` as post or pre validator.
The second one require to write a `ScopeExtractor` which should inspect constraints in the policy and extract the
relevant scopes.

This DR proposes a more flexible approach where both types of scopes extraction mechanism can be defined using
configuration, without the need to write custom code.

## Approach

### SPIs

We will introduce a new entity `DcpScope` that holds the following properties:

```java
public class DcpScope {

    public String profile;
    private String id;
    private Type type; // DEFAULT or POLICY
    private String value;
    private String prefixMapping;

    public enum Type {
        DEFAULT, POLICY
    }
}
```

where:

- `profile`: the dataspace profile this scope applies to (or `*` for all profiles)
- `id`: unique identifier of the scope
- `type`: the type of the scope, either `DEFAULT` or `POLICY`
- `value`: is the DCP scope value to apply if the scope matches.
- `prefixMapping`: optional JSON-LD prefix mapping to match with a policy constraints left operand.

When the type is `DEFAULT` the `value` is a static string representing the scope to add to each request.
When the type is `POLICY` the `value` will be added to the request only if a matching constraint is found in the policy.

We will also introduce two new SPIs:

- `DcpScopeStore`: an interface to manage `DcpScope` entities.
- `DcpScopeRegistry`: an interface to add/remove/resolve scopes.

### Default Implementations

We will provide at first the following implementations:

- An in-memory implementation of `DcpScopeStore`.
- A default implementation of `DcpScopeRegistry` that uses the `DcpScopeStore` to resolve scopes.
- A `DefaultScopeMappingFunction` that resolves default scopes by fetching all `DcpScope` of type `DEFAULT`
  from the `DcpScopeStore` matching the current dataspace profile.
- A `DynamicScopeExtractor` that resolves policy scopes by inspecting the policy constraints and matching
  them against the `DcpScope` of type `POLICY` in the `DcpScopeStore`. The matching is done by comparing the constraint
  left operand with the `prefixMapping` of the `DcpScope`. If a match is found, the corresponding `value` is added to
  the scopes.

Since at first it would not be possible to manage `DcpScope` entities via API, we will provide a way to bootstrap
them using configuration. For example:

```properties
edc.iam.dcp.scopes.membership.id:"membership-scope"
edc.iam.dcp.scopes.membership.type:"DEFAULT"
edc.iam.dcp.scopes.membership.value:"org.eclipse.edc.vc.type:MembershipCredential:read"
#
edc.iam.dcp.scopes.manufacturer.id:"manufacturer-scope"
edc.iam.dcp.scopes.manufacturer.type:"POLICY"
edc.iam.dcp.scopes.manufacturer.value:"org.eclipse.edc.vc.type:ManufacturerCredential:read"
edc.iam.dcp.scopes.manufacturer.prefix-mapping:"ManufacturerCredential"
```