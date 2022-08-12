# Versioning concept for MVD

## Decision

We want to get rid of checking out other repositories during CI, building them locally and publishing them into the
local Maven cache (`publishToMavenLocal`).

## Rationale

When building and/or running projects that use EDC (such as MVD) it is quite cumbersome and error-prone to check out a
particular git ref in different projects and to build and publish them locally.

We will therefore move toward a system where we use distributed Maven artifacts rather than local ones. This is less
flexible than git refs, but at the same time improves coherence and setup speed.

## General rules

_for the sake of brevity, the term "our" refers to all original implementation projects inside the
`eclipse-dataspaceconnector` org in Github. At the time of this writing that includes `DataSpaceConnector`,
`RegistrationService`, `IdentityHub`, `MinimumViableDataspace` and `FederatedCatalog` (not yet populated)._

All "our" projects must

- use Maven artifacts from MavenCentral or OSSRH Snapshots, both for local and CI builds
- produce a new rolling `-SNAPSHOT` version based on their respective `main` branch every 30 minutes if there are
  changes. _This is already in place._
- produce a nightly snapshot build containing the date in the metadata, in the format `X.Y.Z-YYYYMMDD-SNAPSHOT`. _This
  is already in place._

## Specific rules for "our" dependent projects

- publishing a new release in a dependency should also trigger a release in dependent projects with the same version
  string. E.g. building EDC -> triggers RS and IH.
- all "our" dependent projects **must** maintain version consistency: for example when RS and IH both reference
  EDC `0.0.1-some-fix-SNAPSHOT`, then MVD **must** reference that same version
- version bumps must happen across all "our" repos: when RS upgrades to EDC `0.0.1-milestone-69`, then all other
  projects **must** follow suit.

## During development of "our" dependent projects

- the `main` branches of "our" dependent projects must always reference releases or "named" snapshots of EDC
- in case a dependent project requires a change in EDC, they can temporarily use the rolling snapshot or nightly version
  of EDC including that fix, but EDC should release a "named" snapshot, e.g. `0.0.1-something-SNAPSHOT` in a timely
  manner. From that time forward, that project will use `0.0.1-something-SNAPSHOT` on its `main` branch.
- before merging the PR in the dependent project, there **must** be a named snapshot or release of EDC, which the
  dependent project references henceforth.
