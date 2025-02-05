---
name: Release
about: Create an issue to track a release process.
title: "Release X.Y.Z.W"
labels: [ "task/release", "scope/core" ]
assignees: ""
---

# Release

## Work Breakdown

Feel free to edit this release checklist in-progress depending on what tasks need to be done:

- [ ] Decide a release version. The version must be the Eclipse core-edc version `X.Y.Z` appended with the sovity fork version `.W`, eg. `0.7.2.1`.
- [ ] Checkout the `sovity/X.Y.Z` branch you want to publish.
- [ ] Update the `version` in `gradle.properties`
- [ ] Update this issue's title to the new version.
- [ ] `release-prep` PR with target version-branch `sovity/X.Y.Z`:
    - [ ] Update the `CHANGELOG.md`.
        - [ ] Add a clean `Unreleased` version.
        - [ ] Add the version to the old section.
        - [ ] Add the current date to the old version.
        - [ ] Check the commit history for commits that might be product-relevant and thus should be added to the changelog. Maybe they were forgotten.
        - [ ] Write or review the `Deployment Migration Notes` section, check the commit history for changed / added
          configuration properties.
        - [ ] Write or review a release summary.
        - [ ] Write or review the compatible versions section.
        - [ ] Remove empty sections from the patch notes.
    - [ ] Merge the `release-prep` PR.
- [ ] Wait for the `sovity/X.Y.Z` branch to be green. You can check the status in GH [actions](https://github.com/sovity/core-edc/actions).
- [ ] [Create a release](https://github.com/sovity/core-edc/releases/new)
    - [ ] In `Choose the tag`, type your new release version in the format `vX.Y.Z.W` (for instance `v1.2.3.4`) then
      click `+Create new tag vX.Y.Z.W on release`.
    - [ ] Set the target branch to `sovity/0.2.1`.
    - [ ] Re-use the changelog section as release description, and the version as title.
- [ ] Check if the pipeline built the release versions in the Actions-Section (or you won't see it).
- [ ] [Promote](https://github.com/sovity/core-edc/blob/default/docs/publication/promote.md) the artifacts in Azure.
- [ ] Revisit the changed list of tasks and compare it
  with [.github/ISSUE_TEMPLATE/release.md](https://github.com/sovity/edc-extensions/blob/default/.github/ISSUE_TEMPLATE/release.md).
  Propose changes where it makes sense.
- [ ] Close this issue.
