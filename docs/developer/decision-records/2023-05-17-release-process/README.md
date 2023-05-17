# Release process

## Decision

We will start to release more often, changing the versioning convention.

## Rationale

6-weeks (if not more) release cycles demonstrated to be not really suitable for downstream projects, because forced them
to either use nighly builds (that are SNAPSHOT) or wait for the next release.
A shorter release cycle would solve this issue.

## Approach

We will drop the `-milestone-#` suffix to the release version, passing to a standard SemVer version number like `X.Y.Z`.

The SemVer specification won't be fully followed up for the moment, the approach followed will be:
- `X` will remain 0 until the end of the "incubation phase"
- `Y` will change on every release
- `Z` will remain 0
- releases will be created from `main` branch
- for specific needs coming from downstream projects (that will need to be agreed within the committer group) a bugfix version could be released on a specific version
- a new version release will need an agreement in the committer group before being triggered
- a new version release could bring breaking changes
