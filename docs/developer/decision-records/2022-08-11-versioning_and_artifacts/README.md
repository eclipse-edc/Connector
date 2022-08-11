# Versioning concept for MVD

## Decision
We want to get rid of checking out other repositories during CI, building them locally and publishing them into the local Maven cache (`publishToMavenLocal`).

## Rationale
When building and/or running projects that use EDC (such as MVD) it is quite cumbersome and error prone having to check out a particular git ref in different projects and to build and publish them locally.

We will therefore move toward a system where we use distributed Maven artifacts rather than local ones. This is less flexible than git refs, but at the same improves coherence and setup speed.

## General rules
- All projects must use Maven artifacts from MavenCentral or OSSRH Snapshots
- EDC (and other projects) produce a new rolling `-SNAPSHOT` version based on their respective `main` branch every 30 minutes if there are changes. _This is already in place._
- EDC (and other projects) produce a nightly snapshot build containing the date in the metadata, in the format `X.Y.Z-YYYYMMDD-SNAPSHOT`. _This is already in place._

## Specific rules for "our" dependent projects 
_for the sake of brevity, "our" refers to all implementation projects inside the `eclipse-dataspaceconnector` org in Github_

- publishing a new release in EDC should also trigger a release in all "our" other projects with the same version string
- all our dependent projects **must** maintain version consistency: for example when RS and IH both reference EDC `0.0.1-some-fix-SNAPSHOT`, then MVD **must** reference that same version
- version bumps must happen across all "our" repos: when RS upgrades to EDC `0.0.1-milestone-69`, then all other projects **must** follow suit.

## During development of "our" dependent projects
- the `main` branches of dependent projects must always reference releases or "named" snapshots of EDC
- in case a dependent project requires a change in EDC, they can temporarily use the rolling snapshot or nightly version of EDC including that fix, but EDC should release a "named" snapshot, e.g. `0.0.1-something-SNAPSHOT` in a timely manner. From time forward, that project will use `0.0.1-something-SNAPSHOT` on its `main` branch.
- before merging the PR in the dependent project, there **must** be a named snapshot or release of EDC, which the dependent project references henceforth.
