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
- `Z` will remain 0 (could be used for bugfixes, please read below)
- releases will be created from `main` branch
- bugfix versions may be released for a specific X.Y version at the discretion of the committers.
- a new version release will need an agreement in the committer group before being triggered
- we won't follow backward compatibility: any release could bring in breaking changes
