# Scope-based Management API Authorization

## Decision

Access Control on EDC APIs will move from a _role-based_ access scheme to a _scope-based_ one. The `role` token claim and the
`@RolesAllowed`-based authorization will be removed in favor of a single, hierarchy-capable `scope` claim evaluated
through `@RequiredScope`. The `admin`, `provisioner` and `participant` roles are dropped; a principal's permissions are
determined solely by the scopes carried in its access token, not by _who_ the principal is.

> This is a breaking change to the API authorization SPI and the expected token shape. It affects downstream repositories
> that consume EDC API auth libraries, most notably **IdentityHub**. See [Downstream impact](#downstream-impact).

## Rationale

Today every Management API v5 endpoint is guarded by _two_ annotations that are both evaluated (logical AND):

- `@RolesAllowed({...})` &rarr; `RoleBasedAccessFilter`, matching a single `role` claim against `{admin, provisioner,
  participant}`.
- `@RequiredScope("management-api:read|write")` &rarr; `ScopeBasedAccessFilter`, matching a space-delimited `scope`
  claim.

In addition, `AuthorizationServiceImpl` performs the resource-ownership (tenant isolation) check and lets the `admin`
and `provisioner` roles bypass it.

This dual scheme has several drawbacks:

- **Non-standard and IdP-specific.** There is no standard JWT claim for roles; the current `role` claim is populated by a
  custom Keycloak extension. The `scope` claim, by contrast, is standardized (RFC 6749 / RFC 9068). Relying on scopes
  lets any compliant identity provider (vanilla Keycloak, Auth0, Entra ID, Cognito, Ory, ...) issue valid tokens
  **without a custom IdP plugin**. Removing that custom extension is the primary driver for this change.
- **Non-standard identity claim.** Resource ownership currently relies on a custom `participant_context_id` claim, which
  is also injected by an IdP extension. Carrying the participant context ID in the standard `sub` claim instead retires a
  _second_ custom extension.
- **Single-valued roles.** `ParticipantPrincipal.role` is a single string, so a principal cannot hold more than one
  role. Scopes are a set by nature and compose naturally.
- **Redundant dimensions.** Roles encode _which actor_, scopes encode _read/write_. Both can be expressed with a single
  scope vocabulary, removing a whole authorization layer (`RoleBasedAccessFilter`/`RoleBasedAccessFeature`).
- **Coarse granularity.** Scopes are currently limited to `management-api:read` and `management-api:write`. A scope
  grammar that supports per-resource scopes enables least-privilege credentials without further code changes.

Scopes do **not** replace the ownership check. "May write assets" is a static capability suitable for a token scope;
"may write _this_ asset" is a runtime, data-dependent decision and must remain in `AuthorizationServiceImpl`. Scopes
replace only the role-based _elevation_ (the cross-tenant bypass).

## Approach

### Scope grammar

Scopes follow a hierarchy-capable grammar so that the coarse tier ships now while finer-grained scopes can be introduced
later **without changing endpoint annotations** — only the scopes issued by the IdP change.

```
scope    := "management-api" [ ":" resource ] ":" action
resource := <term> | "*"            // e.g. assets, policies, participants, ... ; defaults to "*" when omitted
action   := "read" | "write" | "admin"
```

The following notations therefore coexist:

| Scope                            | Meaning                              | Availability               |
|----------------------------------|--------------------------------------|----------------------------|
| `management-api:read` / `:write` | shorthand for `*:read` / `*:write`   | now                        |
| `management-api:*:write`         | write any (ordinary) resource        | now (equivalent to above)  |
| `management-api:admin`           | cross-tenant elevation / superuser   | now                        |
| `management-api:policies:write`  | write **only** policies              | later, no code change      |

### Matching semantics

A _granted_ scope `G` (from the token) satisfies a _required_ scope `R` (from the endpoint) when:

- **resource:** `G.resource == R.resource` **or** `G.resource == "*"`
- **action:** the action hierarchy `admin ⊇ write ⊇ read` holds — a higher granted action satisfies a lower required one
  (a `write` token may also read; an `admin` token may do everything).

Concretely, a required action `R.action` is satisfied by a granted action `G.action` when:

| Required | Satisfied by granted     |
|----------|--------------------------|
| `read`   | `read`, `write`, `admin` |
| `write`  | `write`, `admin`         |
| `admin`  | `admin`                  |

Two safety rules:

1. **`admin` is the elevation flag**, not merely the highest action. Holding `management-api:admin` (≡ `*:admin`) is what
   bypasses the ownership check and grants cross-tenant access. Crucially, `*:write` does **not** imply `admin`, so a
   `write` token remains confined to its own participant context by the runtime ownership check.
2. **The `*` wildcard covers ordinary (tenant-owned) resources only, not system resources.** Participant-context
   onboarding, participant-context configuration and CEL expressions are gated by `admin`, so an ordinary `write` token
   cannot reach them.

A single `ScopeMatcher` (parsing the grammar, applying the wildcard and action hierarchy) is used by both the endpoint
filter (`ScopeBasedAccessFilter`) and the ownership service (`AuthorizationServiceImpl`), so the grammar lives in exactly
one place.

### Principal identity and ownership

The participant context ID — used by the ownership check to confine a principal to its own tenant — is taken from the
standard `sub` claim (replacing today's custom `participant_context_id` claim). `ServicePrincipalAuthenticationFilter`
derives the `ParticipantPrincipal` name from `sub`.

The ownership check then applies as follows:

- If the token carries an `…:admin` scope, the ownership check is bypassed and `sub` is **not** evaluated — an admin
  token's `sub` may be empty or any subject (e.g. a service-account or human-user id) and need not correspond to a participant
  context.
- For every other scope, the ownership check is **required**: `sub` must equal the resource owner, which must equal the
  resource's `participantContextId`. A non-admin token must therefore carry a `sub` that is a valid participant context
  ID.

Because identity now rides on the standard `sub` claim, the custom IdP extension that injected the `participant_context_id`
claim is no longer needed.

### Removal of the provisioner (and role) concept

Because authorization depends only on scopes and not on the principal's identity, the `provisioner` role becomes
obsolete: an endpoint that previously required the `provisioner` (or `admin`) role now requires the `management-api:admin`
scope. The mapping below preserves today's effective authorization:

| Controller(s)                                                                                                                  | Today (`@RolesAllowed`)                       | New `@RequiredScope`                          |
|--------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------|-----------------------------------------------|
| Asset, PolicyDefinition, ContractDefinition, ContractNegotiation, ContractAgreement, TransferProcess, Catalog, Discovery, DataPlaneRegistration | `{admin, participant}` (+ `provisioner`)      | `management-api:read` / `:write` + ownership check |
| DataspaceProfileContext — read                                                                                                 | `{provisioner, admin, participant}`           | `management-api:read`                          |
| DataspaceProfileContext — write                                                                                                | `{provisioner, admin}`                        | `management-api:admin`                         |
| ParticipantContext, ParticipantContextConfig, CelExpression                                                                    | `{provisioner, admin}` / `{provisioner}`      | `management-api:admin`                         |

If, in the future, a less-powerful "can onboard participants but is not a full superuser" credential is needed, it can be
expressed as a resource-scoped admin scope (e.g. `management-api:participants:admin`) carved out of `admin`; the grammar
already supports this.

### Affected components (this repository)

- `auth-spi`
  - new `Scope` value type and `ScopeMatcher`.
  - `ParticipantPrincipal`: drop the `role` field and the `getRoles()` / role helpers; keep `participantContextId` and
    `scope`.
  - `RequiredScope` annotation: unchanged.
- `auth-authentication-oauth2-lib`
  - `ServicePrincipalAuthenticationFilter`: stop reading the `role` claim (`Constants.TOKEN_CLAIM_ROLE` removed); read the
    participant context ID from the standard `sub` claim instead of the custom `participant_context_id` claim
    (`Constants.TOKEN_CLAIM_PARTICIPANT_CONTEXT_ID` removed). The participant-context existence validation is skipped for
    admin-scoped tokens, whose `sub` need not be a participant context.
- `auth-authorization-oauth2-lib`
  - `ScopeBasedAccessFilter`: use `ScopeMatcher` instead of a raw `split(" ").contains(...)`.
  - `AuthorizationServiceImpl`: the elevation bypass changes from `isUserInRole(admin|provisioner)` to a
    `management-api:admin` scope check; the ownership check is unchanged.
  - `RoleBasedAccessFilter` / `RoleBasedAccessFeature` removed, along with their registration in
    `ManagementApiAuthorizationExtension`.
- All Management API v5 controllers: `@RolesAllowed` annotations removed; `@RequiredScope` set per the mapping table.

### Token shape

A Management API access token carries the participant context ID in the standard `sub` claim and a space-delimited
`scope` claim. The custom `participant_context_id` and `role` claims are no longer read.

```json
{
  "iss": "https://idp.example.org/...",
  "sub": "did:web:participant-a",
  "scope": "management-api:read management-api:write",
  "exp": 1700000000
}
```

For non-admin tokens, `sub` must be a valid participant context ID (it drives the ownership check); for admin-scoped
tokens `sub` may be any subject. The connector continues to validate the token signature against the IdP's JWKS and to
enforce the standard `iss` / `exp` rules; no custom IdP extension is required.

## Downstream impact

The management API authentication/authorization libraries (`auth-spi`, `auth-authentication-oauth2-lib`,
`auth-authorization-oauth2-lib`, `ParticipantPrincipal`, `RequiredScope`) are consumed by downstream repositories.
Adopting this change therefore requires coordinated updates there:

- **IdentityHub** — consumes the same auth libraries and exposes management endpoints guarded with `@RolesAllowed` /
  `@RequiredScope` and the `ParticipantPrincipal`. It must:
  - re-annotate its controllers with the new scope scheme and remove `@RolesAllowed` usages;
  - adapt any code that reads `ParticipantPrincipal.role` / `getRoles()`;
  - update whatever component mints management tokens (e.g. its STS / token issuance and test fixtures) to emit the
    `scope` claim with the new grammar instead of the `role` claim, and to put the participant context ID in the `sub`
    claim instead of a custom `participant_context_id` claim.
- **Custom Keycloak extension(s)** — the extension(s) that currently inject the `role` and `participant_context_id`
  claims are no longer required for the Management API and can be retired once all consumers issue `scope` claims and
  carry the participant context ID in `sub`.
- **Deployment / dataspace distributions** (e.g. MVD, Tractus-X EDC and other connector distributions) — IdP/realm
  configuration must be updated to issue the `management-api:*` client scopes and to map the participant context ID onto
  the standard `sub` claim, replacing the previous role and `participant_context_id` mappers.

Because no backward-compatibility window is provided (see below), these repositories must migrate in lockstep with this
change.

## Backward compatibility

The Management API v5 is not yet final and carries no deprecation guarantees, so **no backward-compatibility window is
provided**. The `role` claim, the role-based filter (`RoleBasedAccessFilter` / `RoleBasedAccessFeature`) and all
`@RolesAllowed` annotations are removed outright; from the cutover, tokens must carry the new `scope` claim. Downstream
consumers must adopt the new scheme in lockstep.

The grammar is nonetheless designed so that introducing per-resource scopes later is purely additive: endpoints may be
re-annotated with finer `@RequiredScope` values and the IdP may issue narrower scopes, with no change to the matching
logic.
