# Automating the EDC release process

## Decision

We will use GitHub actions to automate the release of both `SNAPSHOT` and release versions to decrease the error
surface.

A dedicated `releases` branch will be created to host those.

## Rationale

This allows committers to simply trigger the creation of a version through GitHub actions and does not require logging
in to our JIPP (EF Jenkins) instance and perform manual actions there.

Having a dedicated branch for all releases will it make easier in the future to provide hotfixes for older versions.

## Approach

There will be a new branch called `releases`, which is only used to record the history of our releases, i.e. receive
merge commits for every release. In addition, a new GitHub workflow will be created that:

1. prompts the user to input version string. This must be a SemVer string.
2. creates a tag on `main` with that version string, e.g. `v0.0.1-SNAPSHOT`. This could be done automatically in step 5.
3. creates a merge-commit `main`->`releases`, where the version string is used in the commit message
4. triggers the release job on JIPP supplying the version string as input parameter
5. creates a GitHub Release

The JIPP then builds and publishes the version to MavenCentral, or OSSRH Snapshots if the version string ends
with `-SNAPSHOT`. For that, a new job will be created on Jenkins, that does _not_ have a cron-based build trigger.

## Future improvements

- update `gradle.properties`: the GitHub action could commit the user input (version string) back
  into `gradle.properties`. That would result in an additional commit and was therefor left out for now.
- bump version automatically: instead of manually entering a version we could have an "auto-bump" feature, that
  automatically increases the version in `gradle.properties`. This makes snapshots with metadata more difficult
  (e.g. `0.0.1-foobar-SNAPSHOT`), and was therefore skipped for now.
- use Jenkins' GitHub hook trigger for GITScm polling: GitHub calls a WebHook in Jenkins, who then in turn
  one-time-polls the Git repo, and triggers a build when changes were detected. This would get rid of the busy waiting
  of the GitHub Jenkins Action.
