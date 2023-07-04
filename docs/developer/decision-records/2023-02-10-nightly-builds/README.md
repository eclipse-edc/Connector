# Nightly builds become release versions

## Decision

Nightly builds will henceforth be published as release version (as opposed to: snapshots) and will be published to OSSRH
Releases.
Only major releases, such as milestones, will get published to MavenCentral.

## Rationale

Downstream projects may want to track the EDC features as closely as possible while still maintaining a consistent and
repeatable build pipeline. EDC's release train is roughly six to eight weeks, which may be too sparse for downstream
projects' development schedules.

To that end, the only option currently is to use nightlies, but they are snapshots, and snapshot builds are ill-suited,
because they are not permanent, and can be overwritten or deleted at any time.

## Approach

- Publish all snapshots to [OSSRH Snapshots](https://oss.sonatype.org/content/repositories/snapshots/)
- Make nightly versions releases, e.g. `0.0.1.4-SNAPSHOT0230210`
- Publish all release versions to [OSSRH Releases](https://oss.sonatype.org/content/repositories/releases/)
- Only publish major releases (e.g. milestones) to MavenCentral

The reason for using OSSRH Release instead of MavenCentral for nightly releases is simply that we do not want to spam
MavenCentral with ~300 artifacts on a daily basis, which would offer little value to the larger community.

The build plugin needs to be adapted to publish to OSSRH Releases by default. Further, we need to implement a separate,
additional task that allows publishing to MavenCentral, which is invoked from a (new) Jenkins job.
