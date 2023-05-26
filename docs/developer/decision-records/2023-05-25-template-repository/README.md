# Template and `.github` Repositories

## Decision

Two new repositories will be created within the EDC GitHub organization:
- `.github` repository for common files that are the _same_ for all components,
- dedicated [template repositories](https://docs.github.com/en/repositories/creating-and-managing-repositories/creating-a-template-repository)
  for files that must be _adapted/modified_ by each component.

## Rationale

Currently, general documents like the `pr_etiquette.md` or `CONTRIBUTING.md`
are located in the Connector repository, although they apply to all repositories in the scope of the
EDC project. In addition, resources like style files or issue/pr templates are duplicated across
repositories and, with this, are not up-to-date, as most of the changes to those documents are made
in the Connector repo and are not replicated across the other component repos.

The reasoning for moving these documents out to separate repositories is as follows:
- simplify maintenance of files
- facilitate the onboarding for new community members
- improve automation of GitHub processes
- harmonize creation process of new EDC repositories

## Approach

At the time of this writing, two repositories can be identified:
- `.github`
- `template-basic`

We need to identify "common" and "repository-specific" documents. In addition, we need to test how the
`.github` repo and the repositories' `.github` folders relate to each other.

### Common documents

Suggested structure for `.github` repository:
- `.github/`:
    - [ISSUE_TEMPLATE](https://github.com/eclipse-edc/Connector/tree/main/.github/ISSUE_TEMPLATE) and [PULL_REQUEST_TEMPLATE.md](https://github.com/eclipse-edc/Connector/blob/main/.github/PULL_REQUEST_TEMPLATE.md)
    - generic workflows, e.g.
        - [scan-pull-request.yaml](https://github.com/eclipse-edc/Connector/blob/main/.github/workflows/scan-pull-request.yaml),
        - [first-interaction.yml](https://github.com/eclipse-edc/Connector/blob/main/.github/workflows/first-interaction.yml),
        - [close-inactive-issues.yml](https://github.com/eclipse-edc/Connector/blob/main/.github/workflows/close-inactive-issues.yml)
        - `release-<COMPONENT>.yml`
        - ...
- `contributing/`:
    - [CONTRIBUTING.md](https://github.com/eclipse-edc/Connector/blob/main/CONTRIBUTING.md)
    - [contribution_categories.md](https://github.com/eclipse-edc/Connector/blob/main/contribution_categories.md)
    - [known_friends.md](https://github.com/eclipse-edc/Connector/blob/main/known_friends.md)
    - [pr_etiquette.md](https://github.com/eclipse-edc/Connector/blob/main/pr_etiquette.md)
    - [styleguide.md](https://github.com/eclipse-edc/Connector/blob/main/styleguide.md)
    - ...
- `docs/`: same as for every repo
    - `developer/`: generic documentation files from [docs/developer](https://github.com/eclipse-edc/Connector/tree/main/docs/developer)
        - `decision-records/`: those that cover the EDC project from [docs/developer/decision-records](https://github.com/eclipse-edc/Connector/tree/main/docs/developer/decision-records)
    - `legal/`: files from [docs/legal](https://github.com/eclipse-edc/Connector/tree/main/docs/legal)
    - `templates/`: files from [docs/templates](https://github.com/eclipse-edc/Connector/tree/main/docs/templates)
    - ...
- `profile/README.md`: provide a [welcome page](https://github.blog/changelog/2021-09-14-readmes-for-organization-profiles/)
- `resources/`: move files like [edc-checkstyle-config.xml](https://github.com/eclipse-edc/Connector/blob/main/resources/edc-checkstyle-config.xml)
- `README.md`
- ...

Impacts on component repositories:
- remove generic workflow files
- remove listed contributing files
- remove decision records that affect the general EDC project, e.g., org-wide release process
- remove generic resources
- keep component-specific decision records in component repositories

### Repository-specific documents

Suggested basic structure for new repositories:
- `.github/`: e.g., [dependabot.yml](https://github.com/eclipse-edc/Connector/blob/main/.github/dependabot.yml) _(to be identified, as mentioned above)_
- `docs/developer/decision-records/README.md` _(empty list)_
- [.gitattributes](https://github.com/eclipse-edc/Connector/blob/main/.gitattributes)
- [.gitignore](https://github.com/eclipse-edc/Connector/blob/main/.gitignore) _(empty)_
- [CODEOWNERS](https://github.com/eclipse-edc/Connector/blob/main/CODEOWNERS) _(empty list)_
- [LICENSE](https://github.com/eclipse-edc/Connector/blob/main/LICENSE)
- [NOTICE.md](https://github.com/eclipse-edc/Connector/blob/main/NOTICE.md) _(empty list)_


## Further Considerations

Repositories that could be identified in the future: 
- `template-gradle`

With additional files (matching Java/Gradle)
- `gradle/`
- [build.gradle.kts](https://github.com/eclipse-edc/Connector/blob/main/build.gradle.kts)
- [gradle.properties](https://github.com/eclipse-edc/Connector/blob/main/gradle.properties)
- [gradlew](https://github.com/eclipse-edc/Connector/blob/main/gradlew)
- [gradlew.bat](https://github.com/eclipse-edc/Connector/blob/main/gradlew.bat)
- [settings.gradle.kts](https://github.com/eclipse-edc/Connector/blob/main/settings.gradle.kts)

