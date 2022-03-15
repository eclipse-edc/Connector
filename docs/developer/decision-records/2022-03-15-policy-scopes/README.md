# Policy Scopes
            
## Decision

Implement policy scopes, which are runtime visibility and semantic boundaries for policy rules such that:

- It is possible to specify which rules contained in a policy should be evaluated in a given runtime context. 
- It is possible to specify which evaluation code is executed for a rule type in different runtime contexts.

## Rationale

### Rule Visibility Boundaries 

Policy rules may only be applicable in certain runtime contexts. For example, the following policy rule:

> Data must be anonymized.

That rule may be applicable to policy evaluation when a resource is provisioned but it may bot be applicable during data transfer. There must be a way to specify when a rule type
is applicable during runtime evaluation.
          
### Semantic Boundaries

Policy rules may have different implementation semantics in certain runtime contexts. For example:

> Asset content (data) must remain in EU-based compute environments.

When this rule is evaluated during authorization, a verifiable credential may be checked. When data transfer occurs, this rule may require data to be stored in a particular cloud 
region. There must be a way to specify which evaluation code is executed in a given runtime context.

## Approach

The following concepts will be introduced.

### Policy Scope

A policy scope defines a visibility and semantic boundary for policy rules. Scopes are hierarchical and expressed using dot notation. For example, provision.verify and 
provision.execution are child scopes of provision. If a rule is visible in a given scope, it will be included in policy evaluations for that scope; otherwise, it will be omitted. 
Policy rule semantics are implemented at runtime by rule and constraint functions (described below).

### Rule Binding

A rule binding makes a rule type visible in a policy scope. Since scopes are hierarchical, a binding that specifies a parent context will result in the rule type being visible in 
child scopes. The * wildcard is used to denote all scopes. A rule binding specifies one rule type and one scope. Rule types may be bound to different scopes using 
multiple bindings.

### Rule and Constraint Functions

Rule and constraint functions are registered for a policy scope and are code that is executed at runtime during policy evaluation. Rule and constraint functions implement rule 
semantics.


