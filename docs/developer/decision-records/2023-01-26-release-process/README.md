# Release process

## Decision

EDC release will happen using an ad-hoc root gradle project that has all the components as subprojects.
This needs to be done because the `gradle-nexus.publish-plugin` doesn't support a publication of artifacts coming from
separate repositories with the same `groupId`.

## Rationale

As the `publishToSonatype` gradle task needs to run together with the `closeAndReleaseSonatypeStagingRepository`, that's
the one needed to close the staging repository and publish the artifacts on the release repository (Maven Central), we'd
need to gather all the components in a single gradle project and run the publish task from there.
This approach can be used for "snapshot" and "nightly" releases as well.

## Approach

We'd need two new repositories under the `eclipse-edc` organization, that could be named, according to the convention:
- `JenkinsPipelines`:
  it will contain all the Jenkins pipeline files that are consumed by [our Jenkins instance](https://ci.eclipse.org/edc), 
  currently they are stored into a committer personal GitHub account.
- `Release`:
  it will contain the "root release project" and the script needed to prepare it for a version release, currently they
  are stored into a committer personal GitHub account.

On Jenkins there will be a single job that will build and release the current main branches given a version, at the end,
if the version doesn't end with the `-SNAPSHOT` postfix, the GitHub release will created on every component's repository.

That single job would then be used for official releases, snapshots and nightly releases.
