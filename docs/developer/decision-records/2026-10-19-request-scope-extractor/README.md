# Request Scope Extractor

## Decision

We will introduce a dedicated `RequestScopeExtractor` SPI, and a `RequestScopeExtractorRegistry` that owns its
registration and invocation, to encapsulate the extraction of DCP request scopes. Scope extraction will no longer be
expressed as `PolicyValidatorRule` pre/post validators registered on the `PolicyEngine`, and will no longer rely on a
mutable `RequestScope.Builder` carried inside the `RequestPolicyContext`.

As part of this change, the three existing scope-related `PolicyValidatorRule` implementations
(`DcpScopeExtractorFunction`, `DefaultScopeMappingFunction`, `CatalogScopeExtractorFunction`) will collapse into two
`RequestScopeExtractor` implementations.

## Rationale

Today, request scopes (the `scope` claim of an egress DSP token, and the verification scopes of an ingress token) are
produced by registering `PolicyValidatorRule`s as pre- and post-validators on the `PolicyEngine`. There are three of
them:

- `DcpScopeExtractorFunction` — a pre-validator that extracts scopes from the policy constraints via
  `ScopeExtractorRegistry`.
- `CatalogScopeExtractorFunction` — a catalog-only pre-validator that extracts request-level scopes via
  `CatalogScopeExtractorRegistry`.
- `DefaultScopeMappingFunction` — a post-validator that adds profile-filtered default scopes from `DcpScopeRegistry`.

Each of them mutates, as a side effect, the shared `RequestScope.Builder` held by the `RequestPolicyContext`. The call
sites — `DspHttpRemoteMessageDispatcherImpl` (egress) and `ProtocolTokenValidatorImpl` (ingress) — then read the
accumulated scopes back out of that builder *after* `PolicyEngine.evaluate(...)` has run.

This mechanism works, but it conflates two unrelated concerns and has concrete downsides:

- **It abuses the validator pipeline as an invocation hook.** A `PolicyValidatorRule` is semantically a predicate "is
  this policy valid?". None of the three implementations actually use the outcome of policy evaluation: the two
  pre-validators run before evaluation, and the post-validator reads only the request message, not the evaluated rules.
  They are registered as validators purely to be invoked at the right moment with a context-typed dispatch.
- **The return value is overloaded and inconsistent.** A validator returning `false` short-circuits `evaluate` and
  prevents sibling validators from running. `DcpScopeExtractorFunction` returns `false` on extraction failure (and thus
  can abort evaluation), while the other two deliberately always return `true` and only log on failure. Three
  implementations end up with three different, partly accidental, failure semantics encoded in a `Boolean`.
- **The data flow is hidden.** The output does not appear in any signature: it travels through a mutable builder
  threaded inside the context, and the only way to obtain it is to know to call `requestScopeBuilder.build().getScopes()`
  after `evaluate`. This couples the call sites to an implicit side channel and keeps a mutable field on
  `RequestPolicyContext` that exists solely for this purpose.

Making scope extraction a first-class operation that *returns* its result removes the overloaded `Boolean`, makes the
data flow explicit in the signatures, and lets the three awkward validators become honest, pluggable strategies.

## Approach

### SPIs

We introduce a new functional interface in `decentralized-claims-spi`. It is deliberately named `RequestScopeExtractor`
to distinguish it from the existing, lower-level, per-constraint `ScopeExtractor`
(`extractScopes(leftValue, operator, rightValue, context)`), which stays as-is and continues to be used by
`DcpScopeExtractorVisitor`. The new interface sits one level above it and operates on a whole request:

```java
@FunctionalInterface
public interface RequestScopeExtractor<C extends RequestPolicyContext> {
    Result<Set<String>> extractScopes(Policy policy, C context);
}
```

Returning `Result<Set<String>>` preserves the one meaningful failure path (the constraint-based extraction) explicitly,
instead of smuggling it through a `Boolean` that also happens to control policy evaluation.

A registry owns the context-type dispatch that `PolicyEngine` performs today, and the union-combination of scopes that
the shared builder performs today:

```java
public interface RequestScopeExtractorRegistry {

    <C extends RequestPolicyContext> void registerExtractor(Class<C> contextType, RequestScopeExtractor<C> extractor);

    <C extends RequestPolicyContext> Result<Set<String>> extractScopes(Policy policy, C context);
}
```

`extractScopes` selects the extractors whose registered context type is assignable from the supplied context (mirroring
the current `contextType.isAssignableFrom(...)` filtering in `PolicyEngineImpl`), invokes them, and returns the union of
their scope sets, or a failure if any extraction that is meant to be blocking fails.

### Implementations

Because extraction no longer depends on the evaluation outcome, the pre/post distinction disappears, and the three
implementations collapse into two:

- **`DcpRequestScopeExtractor`** — replaces `DcpScopeExtractorFunction` and `DefaultScopeMappingFunction`. Both are
  DCP-wide (registered for the catalog, negotiation and transfer request contexts) and both draw from DCP registries. The
  merged extractor returns the union of the policy-constraint scopes (via `ScopeExtractorRegistry`) and the
  profile-filtered default scopes (via `DcpScopeRegistry`); the profile filtering currently in
  `DefaultScopeMappingFunction#filterScope` moves inside it.
- **`CatalogRequestScopeExtractor`** — replaces `CatalogScopeExtractorFunction`, essentially unchanged in substance. It
  remains separate because it is bound to the catalog context only, ignores the policy, and serves a distinct purpose
  (provider-side credential prefetch for catalog offer access policies, via `CatalogScopeExtractorRegistry`).

Collapsing all the way to a single implementation was considered and rejected: it would force one extractor to branch on
the context type and fuse two genuinely different responsibilities, trading the current awkwardness for a different one.

### Call sites

The egress dispatcher and the ingress token validator stop reaching into the context's builder and instead call the
registry directly:

```java
var result = requestScopeExtractorRegistry.extractScopes(policy, context);
// egress: fold result into the "scope" token claim
// ingress: fold result into the VerificationContext scopes
```

The `RequestScope.Builder` field can then be removed from `RequestPolicyContext`, and scope extraction is decoupled from
`PolicyEngine.evaluate(...)`. We will verify, while implementing, whether the egress path still needs to call
`evaluate` at all, or whether that call existed solely to trigger the scope extractors.

### Registration

`DcpScopeExtractorExtension` and `DynamicDcpScopeExtension` stop registering pre/post validators on the `PolicyEngine`
and instead register `DcpRequestScopeExtractor` (for all three request contexts) and `CatalogRequestScopeExtractor` (for
the catalog context) on the `RequestScopeExtractorRegistry`.

### Compatibility

`RequestScopeExtractor` and `RequestScopeExtractorRegistry` are new SPI types, so this is an additive change for most
adopters. However:

- Removing the `RequestScope.Builder` accessor from `RequestPolicyContext` is a breaking change for any downstream code
  that extracts scopes through a custom `PolicyValidatorRule`. Such code must migrate to a `RequestScopeExtractor`.
- The lower-level `ScopeExtractor` / `ScopeExtractorRegistry` and `CatalogScopeExtractor` /
  `CatalogScopeExtractorRegistry` SPIs are unaffected.

We may keep the old validator-based path working during a deprecation window if downstream impact warrants it;
otherwise the migration is a direct, mechanical one.

### Open points

- Whether the registry is a standalone component (preferred: smaller `PolicyEngine` surface, trivially unit-testable) or
  an added `extractScopes(policy, context)` method on `PolicyEngine` backed by the same logic.
- Whether the egress `PolicyEngine.evaluate(...)` call can be dropped once scopes are extracted separately.
- The exact failure policy of `RequestScopeExtractorRegistry.extractScopes` when some extractors succeed and one fails
  (fail fast vs. collect-and-report), which should reproduce today's effective behaviour: constraint-based extraction
  failures are blocking, default/catalog extraction failures are not.
